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

import javax.jms.*;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQDestination;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:activemq.xml"})
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class AOpenWireTest extends BaseTest{

	public static final String AMQ_URL = "tcp://localhost:61916";

	private String CMD_LINE_COMMON = getConnectCommand();

	@Override
	protected ConnectionFactory getConnectionFactory() {
		return new ActiveMQConnectionFactory(AMQ_URL);
	}

	@Override
	protected String getConnectCommand() {
		return "-" + CMD_BROKER + " " + AMQ_URL + " ";
	}
}
