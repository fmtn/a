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

import org.apache.activemq.command.ActiveMQDestination;
import org.apache.qpid.amqp_1_0.jms.impl.ConnectionFactoryImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.TextMessage;
import java.net.MalformedURLException;
import static co.nordlander.a.A.CMD_AMQP;
import static co.nordlander.a.A.CMD_BROKER;
import static co.nordlander.a.A.CMD_PUT;

/**
 * Test class to test basic operations using AMQP transport.
 *
 * Created by Petter on 2015-01-30.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:activemq_amqp.xml"})
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class AMQPTest extends BaseTest {

    protected static final String AMQ_AMQP_URL = "amqp://guest:guest@localhost:5677";

    @Override
    protected ConnectionFactory getConnectionFactory() {
        try {
            return ConnectionFactoryImpl.createFromURL(AMQ_AMQP_URL);
        }catch(MalformedURLException e){
            throw new RuntimeException(e);
        }
    }

    @Test
    public void JndiConnectTest() throws Exception{
        String cmdLine =  "--jndi /amqp/jndi.properties -" + CMD_PUT + "\"test\"" + " TEST.QUEUE";
        a.run(cmdLine.split(" "));
        MessageConsumer mc = session.createConsumer(testQueue);
        TextMessage msg = (TextMessage)mc.receive(TEST_TIMEOUT);
        assertEquals("test",msg.getText());
    }

    @Override
    protected String getConnectCommand() {
        return "-" + CMD_AMQP + " -" + CMD_BROKER + " " + AMQ_AMQP_URL + " ";
    }

   @Override
   protected void clearBroker() throws Exception {
      // Clear
      for(ActiveMQDestination destination : amqBroker.getRegionBroker().getDestinations()){
         amqBroker.getRegionBroker().removeDestination(
            amqBroker.getRegionBroker().getAdminConnectionContext(),
            destination,1);
      }
   }
}
