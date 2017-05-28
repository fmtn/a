/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.nordlander.a;

import static co.nordlander.a.A.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.broker.BrokerService;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;

/**
 * A base class with all the test cases.
 * The actual transport protocol has to be implemented as well as the broker implementation.
 * This is done in the real test classes. They could test any JMS complaint protocol and broker.
 *
 * This makes it easy to test that the basic functionality works with different ActiveMQ configurations.
 *
 * Created by petter on 2015-01-30.
 */
public abstract class BaseTest {

    protected static final String LN = System.getProperty("line.separator");
    protected static final long TEST_TIMEOUT = 2000L;
    protected Connection connection;
    protected Session session;
    protected ConnectionFactory cf;
    protected ExecutorService executor;
    protected A a;
    protected ATestOutput output;
    protected Destination testTopic, testQueue, sourceQueue, targetQueue;
    protected TextMessage testMessage;

    @Autowired
    protected BrokerService amqBroker;

    protected abstract ConnectionFactory getConnectionFactory();
    protected abstract String getConnectCommand();
    protected abstract void clearBroker() throws Exception;
    
    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();


    @Before
    public void setupJMS() throws Exception {
        cf = getConnectionFactory();
        connection = cf.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        executor = Executors.newSingleThreadExecutor();
        a = new A();
        output = new ATestOutput();
        a.output = output;

        clearBroker();

        testTopic = session.createTopic("TEST.TOPIC");
        testQueue = session.createQueue("TEST.QUEUE");
        sourceQueue = session.createQueue("SOURCE.QUEUE");
        targetQueue = session.createQueue("TARGET.QUEUE");
        testMessage = session.createTextMessage("test");
        connection.start();

        clearQueue(targetQueue);
        clearQueue(testQueue);
        clearQueue(sourceQueue);
    }

    @After
    public void disconnectJMS() throws JMSException {
        session.close();
        connection.close();
        executor.shutdown();
    }

    @Test
    public void testPutQueue() throws Exception{
        String cmdLine = getConnectCommand() + "-" + CMD_PUT + " \"test\"" + " TEST.QUEUE";
        System.out.println("Testing cmd: " + cmdLine);
        a.run(cmdLine.split(" "));
        MessageConsumer mc = session.createConsumer(testQueue);
        TextMessage msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertEquals("test",msg.getText());
    }
    
    @Test
    public void testPutBytesQueue() throws Exception {
    	String cmdLine = getConnectCommand() + "-" + CMD_PUT + " \"test\" -" + CMD_TYPE + " " + TYPE_BYTES + " TEST.QUEUE";
    	System.out.println("Testing cmd: " + cmdLine);
    	a.run(cmdLine.split(" "));

    	MessageConsumer mc = session.createConsumer(testQueue);
        BytesMessage msg = (BytesMessage)mc.receive(TEST_TIMEOUT);
        byte[] bytes = new byte[(int) msg.getBodyLength()];
        msg.readBytes(bytes);
        assertEquals("test",new String(bytes, StandardCharsets.UTF_8));
    
    }

    @Test
    public void testPutWithPriority() throws Exception{
        final int priority = 6;
        String cmdLine = getConnectCommand() + "-" + CMD_PRIORITY +" " + priority + " -" + CMD_PUT + "\"test\""
                + " TEST.QUEUE";
        a.run(cmdLine.split(" "));
        MessageConsumer mc = session.createConsumer(testQueue);
        TextMessage msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertEquals("test",msg.getText());
        assertEquals(priority,msg.getJMSPriority());
    }

    @Test
    public void testPutTopic() throws Exception{
        String cmdLine = getConnectCommand() + "-" + CMD_PUT + "\"test\"" + " topic://TEST.TOPIC";
        Future<TextMessage> resultMessage = executor.submit(new Callable<TextMessage>(){
            public TextMessage call() throws Exception {
                MessageConsumer mc = session.createConsumer(testTopic);
                return (TextMessage)mc.receive(TEST_TIMEOUT);
            }
        });
        a.run(cmdLine.split(" "));
        assertEquals("test",resultMessage.get().getText());
    }

    @Test
    public void testGetQueue() throws Exception{
        MessageProducer mp = session.createProducer(testQueue);
        mp.send(testMessage);
        String cmdLine = getConnectCommand() + "-" + CMD_GET + " -" +
                CMD_WAIT + " 2000" + " TEST.QUEUE";
        a.run(cmdLine.split(" "));
        String out = output.grab();
        assertTrue("Payload test expected",out.contains("Payload:"+LN+"test"));
    }
    
    @Test
    public void testGetQueueWithSelector() throws Exception{
        MessageProducer mp = session.createProducer(testQueue);
        
        Message theOne = session.createTextMessage("theOne"); // message 1
        theOne.setStringProperty("identity","theOne");
        Message theOther = session.createTextMessage("theOther"); // message 2
        theOther.setStringProperty("identity","theOther");
        
        mp.send(theOne);
        mp.send(theOther);
        
        String cmdLine = getConnectCommand() + "-" + CMD_GET + " -" + CMD_SELECTOR + " identity='theOne'" + " -" +
                CMD_WAIT + " 2000" + " TEST.QUEUE";
        a.run(cmdLine.split(" "));
        String out = output.grab();
        assertTrue("Payload test expected",out.contains("Payload:"+LN+"theOne"));
        assertFalse("The other not expected", out.contains("Payload:" + LN + "theOther"));
    }

    @Test
    public void testGetTopic() throws Exception{
        final String cmdLine = getConnectCommand() + "-" + CMD_GET + " -" +
                CMD_WAIT + " 4000" + " topic://TEST.TOPIC";
        Future<String> resultString = executor.submit(new Callable<String>(){
            public String call() throws Exception {
                a.run(cmdLine.split(" "));
                return output.grab();
            }
        });
        Thread.sleep(300); // TODO remove somehow?
        MessageProducer mp = session.createProducer(testTopic);
        mp.send(testMessage);
        String result = resultString.get();
        assertTrue("Payload test expected", result.contains("Payload:" + LN + "test"));
    }

    /**
     * Test that all messages are copied (not moved) from one queue to the other.
     * @throws Exception
     */
    @Test
    public void testCopyQueue() throws Exception{
        final String cmdLine = getConnectCommand() + "-" + CMD_COPY_QUEUE + " SOURCE.QUEUE TARGET.QUEUE";
        MessageProducer mp = session.createProducer(sourceQueue);
        mp.send(testMessage);
        mp.send(testMessage);
        a.run(cmdLine.split(" "));
        MessageConsumer mc = session.createConsumer(sourceQueue);
        TextMessage msg = null;
        // Verify messages are left on source queue
        msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertNotNull(msg);
        msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertNotNull(msg);
        msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertNull(msg);
        // Verify messages are copied to target queue
        mc = session.createConsumer(targetQueue);
        msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertNotNull(msg);
        msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertNotNull(msg);
        msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertNull(msg);
    }

    /**
     * Test that all messages are moved from one queue to the other.
     * @throws Exception
     */
    @Test
    public void testMoveQueue() throws Exception{
        final String cmdLine = getConnectCommand() + "-" + CMD_MOVE_QUEUE + " SOURCE.QUEUE TARGET.QUEUE";
        MessageProducer mp = session.createProducer(sourceQueue);
        mp.send(testMessage);
        mp.send(testMessage);
        a.run(cmdLine.split(" "));
        MessageConsumer mc = session.createConsumer(sourceQueue);
        TextMessage msg = null;
        // Verify NO messages are left on source queue
        msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertNull(msg);
        // Verify messages are moved to target queue
        mc = session.createConsumer(targetQueue);
        msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertNotNull(msg);
        msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertNotNull(msg);
        msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertNull(msg);
    }
    
    /**
     * Test that all messages are moved from one queue to the other.
     * Input count = 0
     * @throws Exception
     */
    @Test
    public void testMoveZeroCountQueue() throws Exception{
        final String cmdLine = getConnectCommand() + "-" + CMD_MOVE_QUEUE + " SOURCE.QUEUE -" + CMD_COUNT + " 0 TARGET.QUEUE";
        MessageProducer mp = session.createProducer(sourceQueue);
        mp.send(testMessage);
        mp.send(testMessage);
        a.run(cmdLine.split(" "));
        MessageConsumer mc = session.createConsumer(sourceQueue);
        TextMessage msg = null;
        // Verify NO messages are left on source queue
        msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertNull(msg);
        // Verify messages are moved to target queue
        mc = session.createConsumer(targetQueue);
        msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertNotNull(msg);
        msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertNotNull(msg);
        msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertNull(msg);
    }

   /**
     * Test that all messages but one message is moved from one queue to the other.
     * @throws Exception
     */
    @Test
    public void testMoveCountQueue() throws Exception{
        final String cmdLine = getConnectCommand() + "-" + CMD_MOVE_QUEUE + " SOURCE.QUEUE " + "-c" + " 4 TARGET.QUEUE";
        MessageProducer mp = session.createProducer(sourceQueue);

        mp.send(testMessage);
        mp.send(testMessage);
        mp.send(testMessage);
        mp.send(testMessage);
        mp.send(testMessage);
	
        a.run(cmdLine.split(" "));
        MessageConsumer mc = session.createConsumer(sourceQueue);
        TextMessage msg = null;

        // Verify 1 messages are left on source queue
        msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertNotNull(msg);

        // Verify NO messages are left on source queue
        msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertNull(msg);

        // Verify 4 messages is moved to target queue
        mc = session.createConsumer(targetQueue);

        msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertNotNull(msg);
        msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertNotNull(msg);
        msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertNotNull(msg);
        msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertNotNull(msg);

	
        // Verify NO messages are left on target queue
        msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertNull(msg);
    }


    @Test
    public void testGetCount() throws Exception{
        final String cmdLine = getConnectCommand() + "-" + CMD_GET + " -" + CMD_COUNT + "2 TEST.QUEUE";
        MessageProducer mp = session.createProducer(testQueue);
        mp.send(testMessage);
        mp.send(testMessage);
        a.run(cmdLine.split(" "));
        String out = output.grab().replaceFirst("Operation completed in .+","");

        final String expectedOut = "-----------------" + LN +
                "Message Properties" + LN +
                "Payload:" + LN +
                "test" + LN +
                "-----------------" + LN +
                "Message Properties" + LN +
                "Payload:" + LN +
                "test" + LN + LN;
        assertEquals(expectedOut,out);
    }

    @Test
    public void testMoveSelector() throws Exception{
        final String cmdLine = getConnectCommand() + "-" + CMD_MOVE_QUEUE + " SOURCE.QUEUE -s identity='theOne' TARGET.QUEUE";
        MessageProducer mp = session.createProducer(sourceQueue);

        Message theOne = session.createTextMessage("theOne"); // message
        theOne.setStringProperty("identity","theOne");
        Message theOther = session.createTextMessage("theOther"); // message
        theOther.setStringProperty("identity","theOther");

        mp.send(theOne);
        mp.send(theOther);

        a.run(cmdLine.split(" "));
        List<TextMessage> msgs = getAllMessages(session.createConsumer(sourceQueue));
        assertEquals(1,msgs.size());
        assertEquals("theOther",msgs.get(0).getText());

        msgs = getAllMessages(session.createConsumer(targetQueue));
        assertEquals(1,msgs.size());
        assertEquals("theOne",msgs.get(0).getText());
    }

    @Test
    public void testCopySelector() throws Exception{
        final String cmdLine = getConnectCommand() + "-" + CMD_COPY_QUEUE + " SOURCE.QUEUE -s \"identity='the One'\" TARGET.QUEUE";
        MessageProducer mp = session.createProducer(sourceQueue);

        Message theOne = session.createTextMessage("theOne"); // message
        theOne.setStringProperty("identity","the One");
        Message theOther = session.createTextMessage("theOther"); // message
        theOther.setStringProperty("identity","theOther");

        mp.send(theOne);
        mp.send(theOther);

        a.run(splitCmdLine(cmdLine));
        List<TextMessage> msgs = getAllMessages(session.createConsumer(sourceQueue));
        assertEquals(2,msgs.size());

        msgs = getAllMessages(session.createConsumer(targetQueue));
        assertEquals(1,msgs.size());
        assertEquals("theOne",msgs.get(0).getText());
    }

    @Test
    public void testSendMapMessage() throws Exception {
        File folder = tempFolder.newFolder();
        final String msgInJson = "{\"TYPE\":\"test\", \"ID\":1}";
        final File file = new File(folder, "file1.json");
        FileUtils.writeStringToFile(file, msgInJson, StandardCharsets.UTF_8);

        final String cmdLine = getConnectCommand() + "-" + CMD_PUT + "@" + file.getAbsolutePath() + " -" + CMD_TYPE + " " + TYPE_MAP + " TEST.QUEUE";
        a.run(cmdLine.split(" "));

        MessageConsumer mc = session.createConsumer(testQueue);
        MapMessage msg1 = (MapMessage)mc.receive(TEST_TIMEOUT);
        assertEquals(msg1.getString("TYPE"), "test");
        assertEquals(msg1.getInt("ID"), 1);
    }

    @Test
    public void testReadFolder() throws Exception {
    	File folder = tempFolder.newFolder();
    	final String file1 = "file1-content";
    	final String file2 = "file2-content";
    	final String file3 = "no-go";
    	FileUtils.writeStringToFile(new File(folder, "file1.txt"), file1, StandardCharsets.UTF_8);
    	FileUtils.writeStringToFile(new File(folder, "file2.txt"), file2, StandardCharsets.UTF_8);
    	FileUtils.writeStringToFile(new File(folder, "file3.dat"), file3, StandardCharsets.UTF_8);
    	Thread.sleep(2000L); // Saturate file age
    	final String fileFilter = folder.getAbsolutePath() + "/*.txt";
    	final String cmdLine = getConnectCommand() + "-" + CMD_READ_FOLDER + " " + fileFilter + " TEST.QUEUE";
    	a.run(cmdLine.split(" "));
    	
    	MessageConsumer mc = session.createConsumer(testQueue);
    	TextMessage msg1 = (TextMessage)mc.receive(TEST_TIMEOUT);
    	assertNotNull(msg1);
    	assertFalse(file3.equals(msg1.getText()));
    	TextMessage msg2 = (TextMessage)mc.receive(TEST_TIMEOUT);
    	assertNotNull(msg2);
    	assertFalse(file3.equals(msg2.getText()));
    	assertNull(mc.receive(TEST_TIMEOUT));
    	File[] remainingFiles = folder.listFiles();
    	assertEquals(1,remainingFiles.length); // one file left - the .dat one
    	assertEquals("file3.dat",remainingFiles[0].getName());
    }
    
    @Test
    public void testDumpMessages() throws Exception {
    	final String testCorrId = "MyCorrelationId";
    	final String stringPropertyValue = "String Value - å";
    	final String utfText = "Utf-8 Text - 😁";
    	final Queue replyQueue = session.createQueue("test.reply.queue");
    	final TextMessage tm1 = session.createTextMessage(utfText);
    	
    	tm1.setStringProperty("myStringProperty", stringPropertyValue);
    	tm1.setIntProperty("myIntProperty", 42);
    	tm1.setDoubleProperty("myDoubleProperty", Math.PI);
    	tm1.setJMSType("myJmsType");
    	tm1.setJMSCorrelationID(testCorrId);
    	tm1.setJMSDeliveryMode(DeliveryMode.PERSISTENT);
    	tm1.setJMSPriority(2);
    	tm1.setJMSReplyTo(replyQueue);
    	
    	BytesMessage bm1 = session.createBytesMessage();
    	bm1.writeBytes(utfText.getBytes(StandardCharsets.UTF_8));
    	
    	MessageProducer mp = session.createProducer(testQueue);
        mp.send(tm1);
        mp.send(bm1);
        File folder = tempFolder.newFolder();
        File dumpFile = new File(folder, "dump.json");
        
        String cmdLine = getConnectCommand() + "-" + CMD_WRITE_DUMP + " " + dumpFile.getAbsolutePath() + " -" +
                CMD_WAIT + " 2000 -" + CMD_COUNT + " 2" + " TEST.QUEUE";
        a.run(cmdLine.split(" "));
     
        ObjectMapper om = new ObjectMapper();
        
        String result = FileUtils.readFileToString(dumpFile, StandardCharsets.UTF_8);
        System.out.println(result);
        List<MessageDump> resultMsgs = Arrays.asList(om.readValue(result, MessageDump[].class));
        assertEquals(2, resultMsgs.size());
        
        MessageDump resultMsg1 = resultMsgs.get(0);
        assertEquals("TextMessage", resultMsg1.type);
        assertEquals(utfText, resultMsg1.body);
        assertEquals(stringPropertyValue, resultMsg1.stringProperties.get("myStringProperty"));
        
        // decode obj property to List and check consistency. 
        // TODO Actually only works with OpenWire, so ignoring this. Other implementations may only support String, Integer etc.
//        String objectPropertyString = resultMsg1.objectProperties.get("myObjectProperty");
 //       List<String> decodedObjProperty = SerializationUtils.deserialize(Base64.decodeBase64(objectPropertyString));
 //       assertEquals(testList, decodedObjProperty);
        
        assertEquals(new Integer(DeliveryMode.PERSISTENT), resultMsg1.JMSDeliveryMode);
        assertEquals(testCorrId, resultMsg1.JMSCorrelationID);
        
        MessageDump resultMsg2 = resultMsgs.get(1);
        assertEquals("BytesMessage", resultMsg2.type);
        assertEquals(utfText, new String(Base64.decodeBase64(resultMsg2.body), StandardCharsets.UTF_8));
    }
    
    @Test
    public void testRestoreDump() throws IOException, InterruptedException, JMSException {
    	// place file where it can be reached by a - that is on file system, not classpath.
    	File dumpFile = tempFolder.newFile("testdump.json");
    	InputStream jsonStream = BaseTest.class.getClassLoader().getResourceAsStream("testdump.json");
    	FileUtils.writeByteArrayToFile(dumpFile, IOUtils.toByteArray(jsonStream));
    	IOUtils.closeQuietly(jsonStream);
    	
    	final String utfText = "Utf-8 Text - 😁";
    	
    	String cmdLine = getConnectCommand() + "-" + CMD_RESTORE_DUMP + " " + dumpFile.getAbsolutePath() + " " + "TEST.QUEUE";
        a.run(cmdLine.split(" "));
    	
        MessageConsumer mc = session.createConsumer(testQueue);
        TextMessage msg1 = (TextMessage) mc.receive(TEST_TIMEOUT);
        assertNotNull(msg1);
        // msgId is always recreated in JMS - do not test!
        // JMS Timestamp also recreated - do not test!
        
        assertEquals("MyCorrelationId", msg1.getJMSCorrelationID());
        assertEquals(2, msg1.getJMSDeliveryMode());
        assertEquals(4, msg1.getJMSPriority());
        assertEquals("myJmsType", msg1.getJMSType());
        assertEquals(Math.PI, msg1.getDoubleProperty("myDoubleProperty"), 0.000000000000001);
        assertEquals(42, msg1.getIntProperty("myIntProperty"));
        assertEquals(utfText, msg1.getText());
        assertEquals("String Value - å", msg1.getStringProperty("myStringProperty"));
        BytesMessage msg2 = (BytesMessage) mc.receive(TEST_TIMEOUT);
        assertNotNull(msg2);
        byte[] msg2Data = new byte[(int) msg2.getBodyLength()];
        msg2.readBytes(msg2Data);
        assertEquals(utfText, new String(msg2Data, StandardCharsets.UTF_8));
        mc.close();
    }
    
    @Test
    public void testDumpMessagesAndTransform() throws Exception {
    	final String text = "A - JMS util";
    	final TextMessage tm1 = session.createTextMessage(text);
    	tm1.setStringProperty("changeme", "old value");
    	
    	MessageProducer mp = session.createProducer(testQueue);
        mp.send(tm1);
        File folder = tempFolder.newFolder();
        File dumpFile = new File(folder, "dump.json");
        String script = "\"msg.body=msg.body.replace('A','B');msg.stringProperties.put('changeme','new');\"";
        
        String cmdLine = getConnectCommand() + "-" + CMD_WRITE_DUMP + " " + dumpFile.getAbsolutePath() + " -" +
                CMD_WAIT + " 2000 -" + CMD_COUNT + " 1 -" + CMD_TRANSFORM_SCRIPT + " " + script + " TEST.QUEUE";
        a.run(cmdLine.split(" "));
     
        ObjectMapper om = new ObjectMapper();
        
        String result = FileUtils.readFileToString(dumpFile, StandardCharsets.UTF_8);
        System.out.println(result);
        List<MessageDump> resultMsgs = Arrays.asList(om.readValue(result, MessageDump[].class));
        assertEquals(1, resultMsgs.size());
        
        MessageDump resultMsg1 = resultMsgs.get(0);
        assertEquals("B - JMS util", resultMsg1.body);
        assertEquals("new", resultMsg1.stringProperties.get("changeme"));
    }
    
    /**
     * Needed to split command line arguments by space, but not quoted.
     * @param cmdLine command line
     * @return the arguments.
     */
    protected String[] splitCmdLine(String cmdLine){
        List<String> matchList = new ArrayList<String>();
        Pattern regex = Pattern.compile("[^\\s\"]+|\"([^\"]*)\"");
        Matcher regexMatcher = regex.matcher(cmdLine);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                matchList.add(regexMatcher.group(1));
            } else {
                matchList.add(regexMatcher.group());
            }
        }
        return matchList.toArray(new String[0]);
    }

    protected List<TextMessage> getAllMessages(MessageConsumer mc) throws JMSException {
        TextMessage msg = null;
        List<TextMessage> msgs = new ArrayList<TextMessage>();
        while( (msg = (TextMessage) mc.receive(TEST_TIMEOUT))!=null){
            msgs.add(msg);
        }
        return msgs;
    }
    
    protected void clearQueue(final Destination dest) throws JMSException {
    	MessageConsumer mc = session.createConsumer(dest);
    	int cnt = 0;
    	while( mc.receive(1L) != null) {
    		cnt++;
    	}
    	mc.close();
    	System.out.println(cnt + " messages cleared from " + dest.toString());
    }
}
