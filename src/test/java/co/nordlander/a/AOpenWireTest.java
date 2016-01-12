package co.nordlander.a;

import static co.nordlander.a.A.CMD_BROKER;
import static co.nordlander.a.A.CMD_LIST_QUEUES;
import static co.nordlander.a.A.CMD_PUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQDestination;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:activemq.xml"})
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class AOpenWireTest extends BaseTest{

	public static final String AMQ_URL = "tcp://localhost:61916";

	@Test
	public void jndiConnectTest() throws Exception {
		String cmdLine =  "--jndi /openwire/jndi.properties -" + CMD_PUT + "\"test\"" + " TEST.QUEUE";
		a.run(cmdLine.split(" "));
		MessageConsumer mc = session.createConsumer(testQueue);
		TextMessage msg = (TextMessage)mc.receive(TEST_TIMEOUT);
		assertEquals("test", msg.getText());
	}
	
	@Test
	public void listQueuesTest() throws Exception {
		
		MessageProducer mp = session.createProducer(testQueue);
	    mp.send(testMessage);
	    MessageProducer mp2 = session.createProducer(testTopic);
	    mp2.send(testMessage);
	    
		String cmdLine = getConnectCommand() + " -" + CMD_LIST_QUEUES;
		a.run(cmdLine.split(" "));
		String result = output.grab();
		assertTrue(result.contains("TEST.QUEUE"));
		assertTrue(result.contains("TEST.TOPIC"));
	}
	
	@Override
	protected ConnectionFactory getConnectionFactory() {
		return new ActiveMQConnectionFactory(AMQ_URL);
	}

	@Override
	protected String getConnectCommand() {
		return "-" + CMD_BROKER + " " + AMQ_URL + " ";
	}

   @Override
   protected void clearBroker() throws Exception {
      // Clear
	   amqBroker.deleteAllMessages();
      for(ActiveMQDestination destination : amqBroker.getRegionBroker().getDestinations()){
         amqBroker.getRegionBroker().removeDestination(
            amqBroker.getRegionBroker().getAdminConnectionContext(),
            destination,1);
      }
   }
}
