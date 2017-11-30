package co.nordlander.a;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

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
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.script.ScriptException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.MessageTransformer;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.command.BrokerInfo;
import org.apache.activemq.command.CommandTypes;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.command.DataStructure;
import org.apache.activemq.command.DestinationInfo;
import org.apache.activemq.command.ProducerInfo;
import org.apache.activemq.command.RemoveInfo;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.qpid.amqp_1_0.jms.impl.ConnectionFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A - A JMS testing and admin tool. Primarily built for use with ActiveMQ.
 */
public class A {
	private static final Logger logger = LoggerFactory.getLogger(A.class);
	protected ConnectionFactory cf;
	protected Connection conn;
	protected Session sess, tsess;
	protected CommandLine cmdLine;
	MessageDumpTransformer transformer = new MessageDumpTransformer();

	// Customizable output
	protected AOutput output = new AOutput() {
		public void output(Object... args) {
			for (Object arg : args) {
				System.out.print(arg.toString());
			}
			System.out.println("");
		}
	};
	
	// Commands
	public static final String CMD_AMQP = "A";
	public static final String CMD_ARTEMIS_CORE = "a";
	public static final String CMD_BROKER = "b";
	public static final String CMD_COPY_QUEUE = "C";
	public static final String CMD_COUNT = "c";
	public static final String CMD_CORRELATION_ID = "E";
	public static final String CMD_ENCODING = "e";
	public static final String CMD_JNDI_CF = "F";
	public static final String CMD_FIND = "f";
	public static final String CMD_GET = "g";
	public static final String CMD_SET_HEADER = "H";
	public static final String CMD_SET_INT_HEADER = "I";
	public static final String CMD_PRIORITY = "i";
	public static final String CMD_JNDI = "J";
	public static final String CMD_JMS_HEADERS = "j";
	public static final String CMD_LIST_QUEUES = "l";
	public static final String CMD_SET_LONG_HEADER = "L";
	public static final String CMD_MOVE_QUEUE = "M";
	public static final String CMD_NON_PERSISTENT = "n";
	public static final String CMD_OPENWIRE = "O";
	public static final String CMD_OUTPUT = "o";
	public static final String CMD_PASS = "P";
	public static final String CMD_PUT = "p";
	public static final String CMD_REPLY_TO = "r";
	public static final String CMD_READ_FOLDER = "R";
	public static final String CMD_TRANSFORM_SCRIPT = "S";
	public static final String CMD_SELECTOR = "s";
	public static final String CMD_NO_TRANSACTION_SUPPORT = "T";
	public static final String CMD_TYPE = "t";
	public static final String CMD_USER = "U";
	public static final String CMD_VERSION = "v";
	public static final String CMD_WAIT = "w";
	public static final String CMD_RESTORE_DUMP = "X";
	public static final String CMD_WRITE_DUMP = "x";
	public static final String CMD_JMS_TYPE = "y";
	
	// Various constants
	public static final long SLEEP_TIME_BETWEEN_FILE_CHECK = 1000L;
	public static final String DEFAULT_COUNT_GET = "1";
	public static final String DEFAULT_COUNT_ALL = "0";
	public static final String DEFAULT_WAIT = "50";
	public static final String TYPE_TEXT = "text";
	public static final String TYPE_BYTES = "bytes";
	public static final String TYPE_MAP = "map";
	public static final String DEFAULT_TYPE = TYPE_TEXT;
	public static final String DEFAULT_DATE_FORMAT = "yyyy MM dd HH:mm:ss";

	public enum Protocol {
		OpenWire, AMQP, ArtemisCore
	}

	public static void main(String[] args) throws ParseException,
			InterruptedException {
		A a = new A();
		a.run(args);
	}

	public void run(String[] args) throws InterruptedException {
		Options opts = createOptions();

		if (args.length == 0) {
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp(
					"java -jar a-<version>-with-dependencies.jar", opts, true);
			System.exit(0);
		}

		CommandLineParser cmdParser = new PosixParser();

		try {
			cmdLine = cmdParser.parse(opts, args);
			if( cmdLine.hasOption(CMD_VERSION)){
				executeShowVersion();
				return;
			}

			Protocol protocol = Protocol.OpenWire;
			if (cmdLine.hasOption(CMD_AMQP)) {
				protocol = Protocol.AMQP;
			} else if (cmdLine.hasOption(CMD_ARTEMIS_CORE)) {
				protocol = Protocol.ArtemisCore;
			}

			connect(cmdLine.getOptionValue(CMD_BROKER, "tcp://localhost:61616"),
					cmdLine.getOptionValue(CMD_USER),
					cmdLine.getOptionValue(CMD_PASS), protocol,
					cmdLine.getOptionValue(CMD_JNDI, ""),
					cmdLine.hasOption(CMD_NO_TRANSACTION_SUPPORT));

			long startTime = System.currentTimeMillis();
			executeCommandLine(cmdLine);
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			output("Operation completed in ", Long.toString(elapsedTime),
					"ms (excluding connect)");
		} catch (ParseException pe) {
			pe.printStackTrace();
			return;
		} catch (JMSException je) {
			je.printStackTrace();
			return;
		} catch (Exception e) {
			e.printStackTrace();
			return;
		} finally {
			try {
				if (sess != null) {
					sess.close();
				}

				if (conn != null) {
					conn.close();
				}
			} catch (JMSException e2) {
				e2.printStackTrace();
			}
		}
		logger.debug("Active threads {}", Thread.activeCount());
		logger.debug("At the end of the road");
	}

	protected void executeCommandLine(CommandLine cmdLine) throws Exception{
		if (cmdLine.hasOption(CMD_GET)) {
			executeGet(cmdLine);
		} else if (cmdLine.hasOption(CMD_PUT)) {
			executePut(cmdLine);
		} else if (cmdLine.hasOption(CMD_COPY_QUEUE)) {
			executeCopy(cmdLine);
		} else if (cmdLine.hasOption(CMD_MOVE_QUEUE)) {
			executeMove(cmdLine);
		} else if (cmdLine.hasOption(CMD_LIST_QUEUES)) {
			executeListQueues(cmdLine);
		} else if (cmdLine.hasOption(CMD_READ_FOLDER)) {
			executeReadFolder(cmdLine);
		} else if (cmdLine.hasOption(CMD_WRITE_DUMP)) {
			executeWriteDump(cmdLine);
		} else if (cmdLine.hasOption(CMD_RESTORE_DUMP)) {
			executeReadDump(cmdLine);
		} else {
			executeBrowse(cmdLine);
		}
	}

	protected void executeMove(CommandLine cmdLine) throws JMSException,
			UnsupportedEncodingException, IOException {
		
		// Should be able to support some kind of Move operation even though the session is not transacted.
		boolean hasTransactionalSession = tsess != null;
		Session moveSession = hasTransactionalSession ? tsess : sess;
		
		Queue tq = moveSession.createQueue(cmdLine.getArgs()[0]);
		Queue q = moveSession.createQueue(cmdLine.getOptionValue(CMD_MOVE_QUEUE)); // Source

		MessageConsumer mq = null;
		MessageProducer mp = moveSession.createProducer(tq);
		if (cmdLine.hasOption(CMD_SELECTOR)) { // Selectors
			mq = moveSession.createConsumer(q, cmdLine.getOptionValue(CMD_SELECTOR));
		} else {
			mq = moveSession.createConsumer(q);
		}
		int count = Integer.parseInt(cmdLine.getOptionValue(CMD_COUNT,
				DEFAULT_COUNT_ALL));
		int j = 0;
		while (j < count || count == 0) {
			Message msg = mq.receive(100L);
			if (msg == null) {
				break;
			} else {
				mp.send(msg);
				if( hasTransactionalSession ){
					moveSession.commit();
				}
				++j;
			}
		}
		output(j, " msgs moved from ", cmdLine.getOptionValue(CMD_MOVE_QUEUE),
				" to ", cmdLine.getArgs()[0]);
	}

	protected void executeCopy(CommandLine cmdLine) throws JMSException, ScriptException, IOException {
		Queue tq = sess.createQueue(cmdLine.getArgs()[0]);
		Queue q = sess.createQueue(cmdLine.getOptionValue(CMD_COPY_QUEUE)); // Source
		QueueBrowser qb = null;
		MessageProducer mp = sess.createProducer(tq);
		if (cmdLine.hasOption(CMD_SELECTOR)) { // Selectors
			qb = sess.createBrowser(q, cmdLine.getOptionValue(CMD_SELECTOR));
		} else {
			qb = sess.createBrowser(q);
		}
		int count = Integer.parseInt(cmdLine.getOptionValue(CMD_COUNT,
				DEFAULT_COUNT_ALL));
		int i = 0, j = 0;
		@SuppressWarnings("unchecked")
		Enumeration<Message> en = qb.getEnumeration();
		while ((i < count || count == 0) && en.hasMoreElements()) {
			Message msg = en.nextElement();
			if (msg == null) {
				break;
			} else {
				// if search is enabled
				if (cmdLine.hasOption(CMD_FIND)) {
					if (msg instanceof TextMessage) {
						String haystack = ((TextMessage) msg).getText();
						String needle = cmdLine.getOptionValue(CMD_FIND);
						if (haystack != null && haystack.contains(needle)) {
							if( cmdLine.hasOption(CMD_TRANSFORM_SCRIPT) ) {
								mp.send(transformMessage(msg,cmdLine.getOptionValue(CMD_TRANSFORM_SCRIPT)));
							} else {
								mp.send(msg);
							}
							
							++j;
						}
					}
				} else {
					mp.send(msg);
					++j;
				}
				++i;
			}
		}
		output(j, " msgs copied from ", cmdLine.getOptionValue(CMD_COPY_QUEUE),
				" to ", cmdLine.getArgs()[0]);
	}

	protected void connect(String url, String user, String password,
			Protocol protocol, String jndi, boolean noTransactionSupport) throws Exception {
		if (StringUtils.isBlank(jndi)) {
			switch (protocol) {
			case AMQP:
				cf = createAMQPCF(url);
				break;
			case OpenWire:
				cf = new ActiveMQConnectionFactory(url);
				break;
			case ArtemisCore:
				cf = ActiveMQJMSClient.createConnectionFactory(url, "");
				break;
			}
		} else {
			// Initialize CF via JNDI.
			Properties properties = new Properties();
			try {
				// try classpath
				InputStream propertiesStream = getClass().getResourceAsStream(
						jndi);
				if (propertiesStream == null) {
					// try absolut path
					propertiesStream = FileUtils
							.openInputStream(new File(jndi)); // will throw FNE
																// if not found
				}
				// Read the hello.properties JNDI propewsrties file and use
				// contents to create the InitialContext.
				properties.load(propertiesStream);
				Context context = new InitialContext(properties);
				// Alternatively, JNDI information can be supplied by setting
				// the "java.naming.factory.initial"
				// system property to value
				// "org.apache.qpid.amqp_1_0.jms.jndi.PropertiesFileInitialContextFactory"
				// and setting the "java.naming.provider.url" system property as
				// a URL to a properties file.
				cf = (ConnectionFactory) context.lookup(cmdLine.getOptionValue(
						CMD_JNDI_CF, "connectionFactory"));

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		if (user != null && password != null) {
			conn = cf.createConnection(user, password);
		} else {
			conn = cf.createConnection();
		}
		sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
		if (noTransactionSupport == false) { // Some providers cannot create transactional sessions. I.e. Azure Servie Bus
			tsess = conn.createSession(true, Session.AUTO_ACKNOWLEDGE);
		} else {
			tsess = null;
		}
		conn.start();
	}

	protected ConnectionFactory createAMQPCF(String uri) {
		try {
            return ConnectionFactoryImpl.createFromURL(uri);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
	}

	protected void executeGet(final CommandLine cmdLine) throws JMSException,
			IOException, ScriptException {
		Destination dest = createDestination(cmdLine.getArgs()[0]);
		MessageConsumer mq = null;
		if (cmdLine.hasOption(CMD_SELECTOR)) { // Selectors
			mq = sess
					.createConsumer(dest, cmdLine.getOptionValue(CMD_SELECTOR));
		} else {
			mq = sess.createConsumer(dest);
		}
		int count = Integer.parseInt(cmdLine.getOptionValue(CMD_COUNT,
				DEFAULT_COUNT_GET));
		long wait = Long.parseLong(cmdLine.getOptionValue(CMD_WAIT,
				DEFAULT_WAIT));
		int i = 0;
		while (i < count || i == 0) {
			Message msg = mq.receive(wait);
			if (msg == null) {
				output("No message received");
				break;
			} else {
				if( cmdLine.hasOption(CMD_TRANSFORM_SCRIPT) ) {
					msg = transformMessage(msg,cmdLine.getOptionValue(CMD_TRANSFORM_SCRIPT));
				}
				
				outputMessage(msg, cmdLine.hasOption(CMD_JMS_HEADERS));
				++i;
			}
		}
	}

	protected void executePut(final CommandLine cmdLine) throws IOException, JMSException, ScriptException{
		String data = cmdLine.getOptionValue(CMD_PUT);
		putData(data, cmdLine);
		if( data.startsWith("@")){
			output("File: " + data.substring(1) + " sent");
		} else {
			output("Message sent");
		}
	}
	
	protected void executeReadFolder(final CommandLine cmdLine) throws IOException,
	JMSException, ScriptException {
		
		final long fileAgeMS = 1000;
		String pathFilter = cmdLine.getOptionValue(CMD_READ_FOLDER);
		if( pathFilter.isEmpty() ){
			output("Option " + CMD_READ_FOLDER + " requires a path and wildcard filename.");
		}
		
		// expression will be like: /path/to/file/*.txt
		// Last index of / will divide filter from path
		File directory = Paths.get(".").toFile();
		String filter = pathFilter;
		int indexOfPathFilterSeparator = pathFilter.lastIndexOf('/');
		if( indexOfPathFilterSeparator > 0){ // path + filename/filter
			String path = pathFilter.substring(0, indexOfPathFilterSeparator);
			directory = new File(path);
			if ( pathFilter.endsWith("/")) {
				output("Option " + CMD_READ_FOLDER + " cannot end with /. Please pass a wildcard filename after path.");
				return;
			} else {
				filter = pathFilter.substring(indexOfPathFilterSeparator + 1);
			}
		}
		AndFileFilter fileFilters = new AndFileFilter();
		fileFilters.addFileFilter(new WildcardFileFilter(filter));
		fileFilters.addFileFilter(new AgeFileFilter(System.currentTimeMillis() - fileAgeMS));
		
		long startTime = System.currentTimeMillis();
		long waitTime = Long.parseLong(cmdLine.getOptionValue(CMD_WAIT,"0"));
		long endTime = startTime + waitTime;	
		do {
			Collection<File> files = FileUtils.listFiles(directory, fileFilters, null); // no recursion
			for(File file : files) {
				putData("@"+file.getAbsolutePath(),cmdLine);
				if (!file.delete()) {
					output("Failed to delete file " + file.getName());
				}
				output("File " + file.getName() + " sent");
			}
			
			try {
				Thread.sleep(SLEEP_TIME_BETWEEN_FILE_CHECK);
			} catch (InterruptedException e) {
				output("Interrupted");
				break;
			}
		} while( endTime > System.currentTimeMillis());
	}
	
	/**
	 * Executes a dump restore. 
	 * 
	 * Can be used with transactional on or off to achive a all or nothing-restore.
	 * Very large restores may not be possible using transactions, but can be done by turning it off.
	 * 
	 * @param cmdLine
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 * @throws ScriptException
	 * @throws JMSException
	 */
	protected void executeReadDump(CommandLine cmdLine) throws JsonParseException, JsonMappingException, IOException, ScriptException, JMSException {
		File dumpFile;
		try{
			dumpFile = new File(cmdLine.getOptionValue(CMD_RESTORE_DUMP));
			if (!dumpFile.exists()) {
				output("Dump file " + dumpFile.getAbsolutePath() + " does not exist");
				return;
			}
		} catch ( Exception e) {
			output("Restore option requires a path to a JSON dump file.");
			return;
		}
		
		String dumpJson = FileUtils.readFileToString(dumpFile, StandardCharsets.UTF_8);
		MessageDumpReader dumpReader = new MessageDumpReader(sess);
		List<MessageDump> dumpMessages = dumpReader.toDumpMessages(dumpJson);
		if( cmdLine.hasOption(CMD_TRANSFORM_SCRIPT)) {
			transformer.transformMessages(dumpMessages, cmdLine.getOptionValue(CMD_TRANSFORM_SCRIPT));
		}
		List<Message> messages = dumpReader.toMessages(dumpMessages);
		Destination destination = createDestination(cmdLine.getArgs()[0]);
		MessageProducer mp = tsess != null ? tsess.createProducer(destination) : sess.createProducer(destination);
		
		for (Message message : messages) {
			mp.send(message, message.getJMSDeliveryMode(), message.getJMSPriority(), message.getJMSExpiration() );
		}
		
		if (tsess != null){
			tsess.commit();
		}
		
		mp.close();
		output(messages.size() + " messages restored to " + cmdLine.getArgs()[0]);
	}

	protected void executeWriteDump(CommandLine cmdLine) throws JMSException, IOException, ScriptException {
		
		List<Message> msgs = consumeMessages(cmdLine);
		
		if( msgs.isEmpty()) {
			output("No messages found - no file written");
		} else {
			MessageDumpWriter mdw = new MessageDumpWriter();
			List<MessageDump> dumpMessages = mdw.toDumpMessages(msgs);
			if( cmdLine.hasOption(CMD_TRANSFORM_SCRIPT)) {
				transformer.transformMessages(dumpMessages, cmdLine.getOptionValue(CMD_TRANSFORM_SCRIPT));
			}
			
			final String jsonDump = mdw.toJson(dumpMessages);
			String filePath = cmdLine.getOptionValue(CMD_WRITE_DUMP);
			FileUtils.writeStringToFile(new File(filePath), jsonDump, StandardCharsets.UTF_8);
			output(msgs.size() + " messages written to " + filePath);
		}
	}

	protected void executeShowVersion() {
		String version = getClass().getPackage().getImplementationVersion();
		output("A - JMS Test/admin utility specialized for ActiveMQ by Petter Nordlander");
		output("Version " + version);
		output("GitHub page: https://github.com/fmtn/a");
	}

	protected List<Message> consumeMessages(CommandLine cmdLine) throws JMSException {
		Destination dest = createDestination(cmdLine.getArgs()[0]);
		MessageConsumer mq = null;
		if (cmdLine.hasOption(CMD_SELECTOR)) { // Selectors
			mq = sess.createConsumer(dest, cmdLine.getOptionValue(CMD_SELECTOR));
		} else {
			mq = sess.createConsumer(dest);
		}
		int count = Integer.parseInt(cmdLine.getOptionValue(CMD_COUNT,
				DEFAULT_COUNT_GET));
		long wait = Long.parseLong(cmdLine.getOptionValue(CMD_WAIT,
				DEFAULT_WAIT));
		
		List<Message> msgs = new ArrayList<Message>();
		int i = 0;
		while (i < count || i == 0) {
			Message msg = mq.receive(wait);
			if (msg == null) {
				break;
			} else {
				msgs.add(msg);
				++i;
			}
		}
		return msgs;
	}
	
	protected void putData(final String data, final CommandLine cmdLine) throws IOException,
			JMSException, ScriptException {
		// Check if we have properties to put
		Properties props = cmdLine.getOptionProperties(CMD_SET_HEADER);
		Properties intProps = cmdLine.getOptionProperties(CMD_SET_INT_HEADER);
		Properties longProps = cmdLine.getOptionProperties(CMD_SET_LONG_HEADER);

		String type = cmdLine.getOptionValue(CMD_TYPE, DEFAULT_TYPE);
		String encoding = cmdLine.getOptionValue(CMD_ENCODING, Charset
				.defaultCharset().name());

		Message outMsg = null;
		// figure out input data
		if (data.startsWith("@")) {
			outMsg = createMessageFromFile(data, type, encoding);
		} else {
			outMsg = createMessageFromInput(data, type, encoding);
		}

		MessageProducer mp = sess.createProducer(createDestination(cmdLine
				.getArgs()[0]));
		if (cmdLine.hasOption("n")) {
			mp.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		}

		// enrich headers.
		for (Entry<Object, Object> p : props.entrySet()) {
			outMsg.setObjectProperty((String) p.getKey(), p.getValue());
		}

		for (Entry<Object, Object> p : intProps.entrySet()) {
			outMsg.setIntProperty((String) p.getKey(), Integer.parseInt((String)p.getValue()));
		}

		for (Entry<Object, Object> p : longProps.entrySet()) {
			outMsg.setLongProperty((String) p.getKey(), Long.parseLong((String)p.getValue()));
		}

		if (cmdLine.hasOption("r")) {
			outMsg.setJMSReplyTo(createDestination(cmdLine.getOptionValue("r")));
		}

		if (cmdLine.hasOption(CMD_CORRELATION_ID)) {
			outMsg.setJMSCorrelationID(cmdLine.getOptionValue(CMD_CORRELATION_ID));
		}

		if (cmdLine.hasOption(CMD_PRIORITY)) {
			try {
				int priority = Integer.parseInt(cmdLine
						.getOptionValue(CMD_PRIORITY));
				mp.setPriority(priority);
			} catch (NumberFormatException nfe) {
				throw new NumberFormatException(
						"JMSPriority has to be an integer value");
			}
		}

		if (cmdLine.hasOption(CMD_JMS_TYPE)) {
			outMsg.setJMSType(cmdLine.getOptionValue(CMD_JMS_TYPE));
		}
		
		boolean useScript = cmdLine.hasOption(CMD_TRANSFORM_SCRIPT);
		final String script = cmdLine.getOptionValue(CMD_TRANSFORM_SCRIPT);
		
		// send multiple messages?
		if (cmdLine.hasOption("c")) {
			int count = Integer.parseInt(cmdLine.getOptionValue("c"));
			for (int i = 0; i < count; i++) {
				final Message finalMsg = useScript ? transformMessage(outMsg, script) : outMsg;
				mp.send(finalMsg);
			}
			output("", count, " messages sent");
		} else {
			final Message finalMsg = useScript ? transformMessage(outMsg, script) : outMsg;
			mp.send(finalMsg);
		}
	}
	
	protected Message transformMessage(final Message msg, final String script) throws JMSException, ScriptException, IOException{
		MessageDumpWriter mdw = new MessageDumpWriter();
		MessageDumpReader mdr = new MessageDumpReader(sess);
		return mdr.toJmsMessage(transformer.transformMessage(mdw.toDumpMessage(msg), script));
	}

	protected Message createMessageFromInput(final String data, String type, String encoding)
			throws JMSException, UnsupportedEncodingException, IOException, JsonParseException, JsonMappingException {
		Message outMsg = null;
		if( type.equals(TYPE_TEXT)) {
			outMsg = sess.createTextMessage(data);
		} else if ( type.equals(TYPE_BYTES)) {
			BytesMessage bytesMsg = sess.createBytesMessage();
			bytesMsg.writeBytes(data.getBytes(encoding));
			outMsg = bytesMsg;
		} else if( type.equals(TYPE_MAP)) {
			MapMessage mapMsg = sess.createMapMessage();
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> msg = mapper.readValue(data, new TypeReference<Map<String, Object>>() { });
			for (String key : msg.keySet()) {
				mapMsg.setObject(key, msg.get(key));
			}
			outMsg = mapMsg;
		} else {
			throw new IllegalArgumentException(CMD_TYPE + ": " + type);
		}
		return outMsg;
	}

	protected Message createMessageFromFile(final String data, String type, String encoding)
			throws IOException, JMSException, UnsupportedEncodingException, JsonParseException, JsonMappingException {
		
		Message outMsg = null;
		// Load file.
		byte[] bytes = FileUtils.readFileToByteArray(new File(data
				.substring(1)));
		if (type.equals(TYPE_TEXT)) {
			outMsg = sess.createTextMessage(new String(bytes, encoding));
		} else if(type.equals(TYPE_BYTES)) {
			BytesMessage bytesMsg = sess.createBytesMessage();
			bytesMsg.writeBytes(bytes);
			outMsg = bytesMsg;
		} else if(type.equals(TYPE_MAP)) {
			MapMessage mapMsg = sess.createMapMessage();
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> msg = mapper.readValue(bytes, new TypeReference<Map<String, Object>>() { });
			for (String key : msg.keySet()) {
				mapMsg.setObject(key, msg.get(key));
			}
			outMsg = mapMsg;
		} else {
			throw new IllegalArgumentException(CMD_TYPE + ": " + type);
		}
		return outMsg;
	}

	// Accepts a plain name, queue://<name>, topic://<name> etc.
	protected Destination createDestination(final String name)
			throws JMSException {
		// support queue:// as well.
		final String correctedName = name.replace("queue://", "queue:").replace("topic://", "topic:");
		if (correctedName.toLowerCase().startsWith("queue:")) {
			return sess.createQueue(correctedName);
		} else if (correctedName.toLowerCase().startsWith("topic:")) {
			return sess.createTopic(correctedName.substring("topic:".length()));
		} else {
			return sess.createQueue(correctedName);
		}
	}

	protected void executeBrowse(final CommandLine cmdLine)
			throws JMSException, IOException {
		final Queue q = sess.createQueue(cmdLine.getArgs()[0]);
		QueueBrowser qb = null;
		// Selector aware?
		if (cmdLine.hasOption(CMD_SELECTOR)) {
			qb = sess.createBrowser(q, cmdLine.getOptionValue(CMD_SELECTOR));
		} else {
			qb = sess.createBrowser(q);
		}

		@SuppressWarnings("rawtypes")
		final Enumeration en = qb.getEnumeration();
		int count = Integer.parseInt(cmdLine.getOptionValue(CMD_COUNT,
				DEFAULT_COUNT_ALL));
		int i = 0;
		while (en.hasMoreElements() && (i < count || count == 0)) {
			Object obj = en.nextElement();
			Message msg = (Message) obj;
			if (cmdLine.hasOption(CMD_FIND)) {
				String needle = cmdLine.getOptionValue(CMD_FIND);
				// need to search for some payload value
				if (msg instanceof TextMessage) {
					String haystack = ((TextMessage) msg).getText();
					if (haystack.contains(needle)) {
						outputMessage(msg, cmdLine.hasOption(CMD_JMS_HEADERS));
					}
				}
			} else {
				outputMessage(msg, cmdLine.hasOption(CMD_JMS_HEADERS));
			}
			++i;
		}
	}

	// ActiveMQ 5.x specific code. Not always working like expected.
	protected void executeListQueues(final CommandLine cmdLine)
			throws JMSException {
		if (conn instanceof org.apache.activemq.ActiveMQConnection) {
			final org.apache.activemq.ActiveMQConnection amqConn = (org.apache.activemq.ActiveMQConnection) conn;

			final Set<ActiveMQQueue> queues = amqConn.getDestinationSource()
					.getQueues();
			final Set<ActiveMQTopic> topics = amqConn.getDestinationSource()
					.getTopics();

			if (!queues.isEmpty()) {
				output("Queues:");
				for (ActiveMQQueue q : queues) {
					output(q.getPhysicalName());
				}
			}

			if (!topics.isEmpty()) {
				output("Topics:");

				for (ActiveMQTopic t : topics) {
					output(t.getTopicName());
				}
			}

		} else {
			throw new RuntimeException(
					"Only ActiveMQ 5.x connections support listing queues");
		}
	}

	protected void outputMessage(Message msg, boolean printJMSHeaders)
			throws JMSException, IOException {
		
		output("-----------------");
		if (printJMSHeaders) {
			outputHeaders(msg);
		}
		outputProperties(msg);
		// Output to file?
		FileOutputStream fos = null;
		File file = null;
		if (cmdLine.hasOption(CMD_OUTPUT)) {
			file = getNextFilename(cmdLine.getOptionValue(CMD_OUTPUT, "amsg"),
					0);
			if (file != null) {
				fos = new FileOutputStream(file);
			}
		}

		if (msg instanceof TextMessage) {
			TextMessage txtMsg = (TextMessage) msg;
			if (fos != null) {
				fos.write(txtMsg.getText().getBytes(
						cmdLine.getOptionValue(CMD_ENCODING, Charset
								.defaultCharset().name())));
				fos.close();
				output("Payload written to file ", file.getAbsolutePath());
			} else {
				output("Payload:");
				output(txtMsg.getText());
			}
		} else if (msg instanceof BytesMessage) {
			BytesMessage bmsg = (BytesMessage) msg;
			byte[] bytes = new byte[(int) bmsg.getBodyLength()];
			bmsg.readBytes(bytes);
			if (fos != null) {
				fos.write(bytes);
				fos.close();
				output("Payload written to file ", file.getAbsolutePath());
			} else {
				output("Hex Payload:");
				output(bytesToHex(bytes));
			}
		} else if (msg instanceof MapMessage) {
			MapMessage mapMsg = (MapMessage) msg;
			@SuppressWarnings("unchecked")
			Enumeration<String> keys = mapMsg.getMapNames();
			output("Payload:");
			while (keys.hasMoreElements()) {
				String name = keys.nextElement();
				Object property = mapMsg.getObject(name);
				output("  ", name, ": ", null != property ? property.toString() : "[null]");
			}
		} else if (msg instanceof ActiveMQMessage) { // Typically advisory messages of internal AMQ events.
			ActiveMQMessage cmdMsg = (ActiveMQMessage) msg;
			displayAdvisoryMessage(cmdMsg);
		} else {
			output("Unsupported message type: ", msg.getClass().getName());
		}
	}

	protected void displayAdvisoryMessage(ActiveMQMessage cmdMsg) throws IOException, JMSException {
		final String topic = cmdMsg.getJMSDestination().toString();
		final String advisoryMsg = advisoryDataStructureToString(cmdMsg.getDataStructure());
		final String advisoryType = cmdMsg.getDataStructure() != null ? "Type: " + dataStructureTypeToString(cmdMsg.getDataStructure().getDataStructureType()) : "";
		output("Advisory on " + topic + advisoryType + (advisoryMsg != null ? " Info " + advisoryMsg : ""));
		
	}
	
	protected String advisoryDataStructureToString(final DataStructure dataStructure) throws JMSException {
		
		if( dataStructure != null) {
			
			switch( dataStructure.getDataStructureType()) {
			
			case CommandTypes.PRODUCER_INFO:
				ProducerInfo pi = (ProducerInfo)dataStructure;
				return "ProducerId: " + pi.getProducerId().toString() + " destination: " + pi.getDestination().toString();
				
			case CommandTypes.CONSUMER_INFO:
				ConsumerInfo ci = (ConsumerInfo)dataStructure;
				return "ConsumerId: " + ci.getConsumerId().toString() + " destination: " + ci.getDestination().toString();
				
			case CommandTypes.CONNECTION_INFO:
				ConnectionInfo connInfo = (ConnectionInfo) dataStructure;
				String connStr = connInfo.getUserName() != null ? connInfo.getUserName() + "@" + connInfo.getClientIp() : connInfo.getClientIp();
				return "ConnectionId: " + connInfo.getConnectionId().toString() + " Connection from: " + connStr + " clientId: " +  connInfo.getClientId();
	
			case CommandTypes.REMOVE_INFO:
				RemoveInfo removeInfo = (RemoveInfo)dataStructure;
				return advisoryDataStructureToString(removeInfo.getObjectId());
				
			case CommandTypes.ACTIVEMQ_MESSAGE:
				ActiveMQMessage messageInfo = (ActiveMQMessage)dataStructure;
				return "messageId: " + messageInfo.getStringProperty("originalMessageId");
			
			case CommandTypes.DESTINATION_INFO:
				DestinationInfo destInfo = (DestinationInfo)dataStructure;
				return destInfo.getDestination().getQualifiedName() + (destInfo.getOperationType() == DestinationInfo.ADD_OPERATION_TYPE ? " added" : " removed");
				
			case CommandTypes.BROKER_INFO:
				BrokerInfo brokerInfo = (BrokerInfo)dataStructure;
				return "brokerId: " + brokerInfo.getBrokerId() + " brokerName: " 
									+ brokerInfo.getBrokerName() + " brokerURL: " + brokerInfo.getBrokerURL();
			
			default:
				return null;
			}
		} else {
			return null;
		}
		
	}

	protected String dataStructureTypeToString(byte dataStructureType)  {
		try{
			for(Field field : CommandTypes.class.getFields()) {
				String name = field.getName();
				byte value = field.getByte(null);
				if( dataStructureType == value ) {
					return name;
				}
			}
		}catch(Exception e){
			return "unknown";
		}
		return "unknown";
	}

	protected void displayRemoveInfo(final RemoveInfo removeInfo, final String startAdvisoryMsg) {
		switch(removeInfo.getObjectId().getDataStructureType()) {
		case CommandTypes.PRODUCER_INFO:
			ProducerInfo pi = (ProducerInfo)removeInfo.getObjectId();
			
			output("Removed producer " + startAdvisoryMsg + pi.getProducerId().getConnectionId() + " that produced to destination: " 
												+ pi.getDestination().toString() );
			break;
		case CommandTypes.CONSUMER_INFO:
			ConsumerInfo ci = (ConsumerInfo)removeInfo.getObjectId();
			output("Removed consumer " + startAdvisoryMsg + ci.getConsumerId().getConnectionId() + " that consumed destination: " 
												+ ci.getDestination().toString());
			break;
			
		case CommandTypes.CONNECTION_INFO:
			ConnectionInfo connInfo = (ConnectionInfo) removeInfo.getObjectId();
			String connStr = connInfo.getUserName() != null ? connInfo.getUserName() + "@" + connInfo.getClientIp() : connInfo.getClientIp();
			output("Removed connection " + startAdvisoryMsg + connInfo.getClientId() + " that connected from: " + connStr);
			break;
		}
	}
	
	protected File getNextFilename(String suggestedFilename, int i) {
		String filename = suggestedFilename;
		if (i > 0) {
			int idx = filename.lastIndexOf('.');
			if (idx == -1) {
				filename = suggestedFilename + "-" + i;
			} else {
				// take care of the extension.
				filename = filename.substring(0, idx) + "-" + i
						+ filename.substring(idx);
			}
		}
		File f = new File(filename);
		if (f.exists()) {
			return getNextFilename(suggestedFilename, ++i);
		} else {
			return f;
		}
	}

	protected void outputHeaders(Message msg) {
		output("Message Headers");
		try {
			String deliveryMode = msg.getJMSDeliveryMode() == DeliveryMode.PERSISTENT ? "persistent"
					: "non-persistent";
			output("  JMSCorrelationID: " + msg.getJMSCorrelationID());
			output("  JMSExpiration: "
					+ timestampToString(msg.getJMSExpiration()));
			output("  JMSDeliveryMode: " + deliveryMode);
			output("  JMSMessageID: " + msg.getJMSMessageID());
			output("  JMSPriority: " + msg.getJMSPriority());
			output("  JMSTimestamp: "
					+ timestampToString(msg.getJMSTimestamp()));
			output("  JMSType: " + msg.getJMSType());
			output("  JMSDestination: "
					+ (msg.getJMSDestination() != null ? msg
							.getJMSDestination().toString() : "Not set"));
			output("  JMSRedelivered: "
					+ Boolean.toString(msg.getJMSRedelivered()));
			output("  JMSReplyTo: "
					+ (msg.getJMSReplyTo() != null ? msg.getJMSReplyTo()
							.toString() : "Not set"));
		} catch (JMSException e) {
			// nothing to do here. just ignore.
			logger.debug("Cannot print JMS headers." + e.getMessage());
		}
	}

	protected String timestampToString(long timestamp) {
		Date date = new Date(timestamp);
		Format format = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
		String timeString = format.format(date).toString();
		return timeString;
	}

	protected void outputProperties(Message msg) throws JMSException {
		output("Message Properties");
		@SuppressWarnings("unchecked")
		Enumeration<String> en = msg.getPropertyNames();
		while (en.hasMoreElements()) {
			String name = en.nextElement();
			Object property = msg.getObjectProperty(name);
			output("  ", name, ": ", null != property ? property.toString() : "[null]");
		}
	}

	protected void output(Object... args) {
		output.output(args);
	}

	protected String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
	    for (byte b : bytes) {
	        sb.append(String.format("%02X ", b));
	    }
	    return sb.toString();
	}

	protected Options createOptions() {
		Options opts = new Options();
		opts.addOption(CMD_BROKER, "broker", true,
				"URL to broker. defaults to: tcp://localhost:61616");
		opts.addOption(CMD_GET, "get", false, "Get a message from destination");
		opts.addOption(CMD_PUT, "put", true,
				"Put a message. Specify data. if starts with @, a file is assumed and loaded");
		opts.addOption(CMD_TYPE, "type", true,
				"Message type to put, [bytes, text, map] - defaults to text");
		opts.addOption(CMD_ENCODING, "encoding", true,
				"Encoding of input file data. Default UTF-8");
		opts.addOption(CMD_NON_PERSISTENT, "non-persistent", false,
				"Set message to non persistent.");
		opts.addOption(CMD_REPLY_TO, "reply-to", true,
				"Set reply to destination, i.e. queue:reply");
		opts.addOption(CMD_CORRELATION_ID, "correlation-id", true,
				"Set CorrelationID");
		opts.addOption(
				CMD_OUTPUT,
				"output",
				true,
				"file to write payload to. If multiple messages, a -1.<ext> will be added to the file. BytesMessage will be written as-is, TextMessage will be written in UTF-8");
		opts.addOption(
				CMD_COUNT,
				"count",
				true,
				"A number of messages to browse,get,move or put (put will put the same message <count> times). 0 means all messages.");
		opts.addOption(CMD_JMS_HEADERS, "jms-headers", false,
				"Print JMS headers");
		opts.addOption(
				CMD_COPY_QUEUE,
				"copy-queue",
				true,
				"Copy all messages from this to target. Limited by maxBrowsePageSize in broker settings (default 400).");
		opts.addOption(CMD_MOVE_QUEUE, "move-queue", true,
				"Move all messages from this to target");
		opts.addOption(CMD_FIND, "find", true,
				"Search for messages in queue with this value in payload. Use with browse.");
		opts.addOption(CMD_SELECTOR, "selector", true,
				"Browse or get with selector");
		opts.addOption(CMD_WAIT, "wait", true,
				"Time to wait on get operation. Default 50. 0 equals infinity");
		opts.addOption(CMD_USER, "user", true, "Username to connect to broker");
		opts.addOption(CMD_PASS, "pass", true, "Password to connect to broker");
		opts.addOption(CMD_PRIORITY, "priority", true, "sets JMSPriority");
		opts.addOption(CMD_AMQP, "amqp", false,
				"Set protocol to AMQP. Defaults to OpenWire");
		opts.addOption(
				CMD_JNDI,
				"jndi",
				true,
				"Connect via JNDI. Overrides -b and -A options. Specify context file on classpath");
		opts.addOption(
				CMD_JNDI_CF,
				"jndi-cf-name",
				true,
				"Specify JNDI name for ConnectionFactory. Defaults to connectionFactory. Use with -J");
		opts.addOption(CMD_ARTEMIS_CORE, "artemis-core", false,
				"Set protocol to ActiveMQ Artemis Core. Defaults to OpenWire");
		opts.addOption(CMD_OPENWIRE, "openwire", false,
				"Set protocol to OpenWire. This is default protocol");
		opts.addOption(CMD_LIST_QUEUES, "list-queues", false,
				"List queues and topics on broker (OpenWire only)");
		
		opts.addOption(CMD_NO_TRANSACTION_SUPPORT,"no-transaction-support", false, 
				"Set to disable transactions if not supported by platform. "
				+ "I.e. Azure Service Bus. When set to false, the Move option is NOT atomic.");	
		
		opts.addOption(CMD_READ_FOLDER, "read-folder", true, 
				"Read files in folder and put to queue. Sent files are deleted! Specify path and a filename."
						+" Wildcards are supported '*' and '?'. If no path is given, current directory is assumed.");

		@SuppressWarnings("static-access")
		Option property = OptionBuilder
				.withArgName("property=value")
				.hasArgs(2)
				.withValueSeparator()
				.withDescription(
						"use value for given String property. Can be used several times.")
				.create(CMD_SET_HEADER);

		opts.addOption(property);

		@SuppressWarnings("static-access")
		Option longProperty = OptionBuilder
				.withArgName("property=value")
				.hasArgs(2)
				.withValueSeparator()
				.withDescription(
						"use value for given Long property. Can be used several times.")
				.create(CMD_SET_LONG_HEADER);

		opts.addOption(longProperty);

		@SuppressWarnings("static-access")
		Option intProperty = OptionBuilder
				.withArgName("property=value")
				.hasArgs(2)
				.withValueSeparator()
				.withDescription(
						"use value for given Integer property. Can be used several times.")
				.create(CMD_SET_INT_HEADER);
				
		opts.addOption(intProperty);
		
		opts.addOption(CMD_WRITE_DUMP, "write-dump", true, "Write a dump of messages to a file. "
						+ "Will preserve metadata and type. Can  be used with transformation option" );
		
		opts.addOption(CMD_RESTORE_DUMP, "restore-dump", true, "Restore a dump of messages in a file," + 
						"created with -" + CMD_WRITE_DUMP + ". Can be used with transformation option.");
		
		opts.addOption(CMD_TRANSFORM_SCRIPT, "transform-script", true, "JavaScript code (or @path/to/file.js). "
					+"Used to transform messages with the dump options. Access message in JavaScript by msg.JMSType = 'foobar';");

		opts.addOption(CMD_VERSION, "version", false, "Show version of A");

		opts.addOption(CMD_JMS_TYPE, "jms-type", true, "Sets JMSType header" );

		
		return opts;
	}
}
