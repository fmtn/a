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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests A with OpenWire protocol. I.e. ActiveMQ 5 native protocol.
 * @author Petter Nordlander
 *
 */
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
	@Ignore // test seem fails under some conditions. The list command is not waterproof.
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
