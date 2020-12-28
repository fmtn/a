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

import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageProducer;
import java.util.Arrays;

import static co.nordlander.a.A.*;
import static org.junit.Assert.assertEquals;

/**
 * Tests A with Artemis/HornetQ native protocol.
 * @author Petter Nordlander
 *
 */
public class ArtemisJmsTest extends BaseTest{

   protected static final String AMQ_ARTEMIS_URL = "tcp://localhost:61616";
   protected static EmbeddedActiveMQ broker;

   @BeforeClass
   public static void createArtemisBroker() throws Exception{
      System.out.println("Starting Artemis");
      broker = new EmbeddedActiveMQ();
      broker.start();
   }

   @Override
   protected ConnectionFactory getConnectionFactory() {
      try {
         return ActiveMQJMSClient.createConnectionFactory(AMQ_ARTEMIS_URL, "");
      }catch(Exception e){
         e.printStackTrace();
         return null;
      }
   }

   @Override
   protected String getConnectCommand() {
      return "-" + CMD_ARTEMIS_CORE + " -" + CMD_BROKER + " " + AMQ_ARTEMIS_URL + " ";
   }

   /**
    * Special treatment for testGetCount since Artemis sets optional JMS headers.
    * @throws Exception
    */
   @Override
   public void testGetCount() throws Exception{
      final String cmdLine = getConnectCommand() + "-" + CMD_GET + " -" + CMD_COUNT + "2 TEST.QUEUE";
      MessageProducer mp = session.createProducer(testQueue);
      mp.send(testMessage);
      mp.send(testMessage);
      MessageDumpWriter writer = new MessageDumpWriter();
      System.out.println(writer.messagesToJsonString(Arrays.asList((Message)testMessage)));
      a.run(cmdLine.split(" "));
      String out = output.grab().replaceFirst("Operation completed in .+","");

      final String expectedOut = "-----------------" + LN +
         "Message Properties" + LN +
         "  JMSXDeliveryCount: 1" + LN +
         "Payload:" + LN +
         "test" + LN +
         "-----------------" + LN +
         "Message Properties" + LN +
         "  JMSXDeliveryCount: 1" + LN +
         "Payload:" + LN +
         "test" + LN + LN;
      assertEquals(expectedOut,out);
   }

   @AfterClass
   public static void tearDownBroker() throws Exception {
      if(broker != null){
         broker.stop();
      }
   }

   public void clearBroker() throws Exception {
      for(QueueConfiguration qc : broker.getActiveMQServer().getConfiguration().getQueueConfigs()){
         broker.getActiveMQServer().destroyQueue(qc.getName());
      }
   }
}
