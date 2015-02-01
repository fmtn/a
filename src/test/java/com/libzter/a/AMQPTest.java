package com.libzter.a;

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
import java.nio.charset.StandardCharsets;

import static com.libzter.a.A.CMD_AMQP;
import static com.libzter.a.A.CMD_BROKER;
import static com.libzter.a.A.CMD_PUT;

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
}
