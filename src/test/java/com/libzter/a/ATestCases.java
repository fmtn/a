package com.libzter.a;

import static com.libzter.a.A.CMD_BROKER;
import static com.libzter.a.A.CMD_GET;
import static com.libzter.a.A.CMD_PUT;
import static com.libzter.a.A.CMD_WAIT;
import static com.libzter.a.A.CMD_PRIORITY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQDestination;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:activemq.xml"})
public class ATestCases {

	@Autowired
    BrokerService amqBroker;
	
    public static final String AMQ_URL = "tcp://localhost:61916";
    private static final String CMD_LINE_COMMON = "-" + CMD_BROKER + " " + AMQ_URL + " ";
    private static final String LN = System.getProperty("line.separator");
    private static final long TEST_TIMEOUT = 2000L;
    private Connection connection;
    private Session session;
    private ActiveMQConnectionFactory cf;
    private ExecutorService executor;
    private A a;
    private ATestOutput output;
    private Destination testTopic, testQueue;
    private TextMessage testMessage;
    
    // Need to grab stdout somehow during tests.
    class ATestOutput implements AOutput{
    	StringBuffer sb = new StringBuffer();
		public void output(Object... args) {
			for(Object arg : args){
				sb.append(arg.toString());
				System.out.print(arg.toString());
			}
			sb.append(LN);
			System.out.println("");
		}
		
		public String grab(){
			String ret = sb.toString();
			sb = new StringBuffer();
			return ret;
		}
		
		public String get(){
			return sb.toString();
		}
    }

    @Before
    public void setupJMS() throws Exception {
        cf = new ActiveMQConnectionFactory(AMQ_URL);
        connection = cf.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        
        executor = Executors.newSingleThreadExecutor();
        a = new A();
        output = new ATestOutput();
        a.output = output;
        
        // Clear
        for(ActiveMQDestination destination : amqBroker.getRegionBroker().getDestinations()){
        	amqBroker.getRegionBroker().removeDestination(
        			amqBroker.getRegionBroker().getAdminConnectionContext(),
        			destination,1);
        }
        
        testTopic = session.createTopic("TEST.TOPIC");
        testQueue = session.createQueue("TEST.QUEUE");
        testMessage = session.createTextMessage("test");
        connection.start();
    }
    @After
    public void disconnectJMS() throws JMSException {
        session.close();
        connection.close();
        executor.shutdown();
    }
    
    @Test
    public void testPutQueue() throws Exception{
    	String cmdLine = CMD_LINE_COMMON + "-" + CMD_PUT + "\"test\"" + " TEST.QUEUE";    	
    	a.run(cmdLine.split(" "));
    	MessageConsumer mc = session.createConsumer(testQueue);
    	TextMessage msg = (TextMessage)mc.receive(TEST_TIMEOUT);
    	assertEquals("test",msg.getText());
    }
    
    @Test
    public void testPutWithPriority() throws Exception{
    	final int priority = 6;
    	String cmdLine = CMD_LINE_COMMON + "-" + CMD_PRIORITY +" " + priority + " -" + CMD_PUT + "\"test\"" 
    			+ " TEST.QUEUE";
    	a.run(cmdLine.split(" "));
    	MessageConsumer mc = session.createConsumer(testQueue);
    	TextMessage msg = (TextMessage)mc.receive(TEST_TIMEOUT);
    	assertEquals("test",msg.getText());
    	assertEquals(priority,msg.getJMSPriority());
    }
    
    @Test
    public void testPutTopic() throws Exception{
    	String cmdLine = CMD_LINE_COMMON + "-" + CMD_PUT + "\"test\"" + " topic://TEST.TOPIC";    	
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
    	String cmdLine = CMD_LINE_COMMON + "-" + CMD_GET + " -" + 
    				CMD_WAIT + " 2000" + " TEST.QUEUE";
    	a.run(cmdLine.split(" "));
    	String out = output.grab();
    	assertTrue("Payload test expected",out.contains("Payload:"+LN+"test"));
    }
    
    @Test
    public void testGetTopic() throws Exception{
    	final String cmdLine = CMD_LINE_COMMON + "-" + CMD_GET + " -" +
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
    	assertTrue("Payload test expected",result.contains("Payload:"+LN+"test"));
    }
}
