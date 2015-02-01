package com.libzter.a;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Properties;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.Context;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.qpid.amqp_1_0.jms.impl.ConnectionFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A - An ActiveMQ/JMS testing and admin tool
 */
public class 
A {
	private static final Logger logger = LoggerFactory.getLogger(A.class);
	protected ConnectionFactory cf;
	protected Connection conn;
	protected Session sess,tsess;
	protected CommandLine cmdLine;
	
	// Customizable output
	protected AOutput output = new AOutput(){
		public void output(Object... args) {
			for(Object arg : args){
				System.out.print(arg.toString());
			}
			System.out.println("");
		}
	};
	
	public static String CMD_AMQP = "A";
	public static String CMD_BROKER = "b";
	public static String CMD_GET = "g";
	public static String CMD_PUT = "p";
	public static String CMD_TYPE = "t";
	public static String CMD_ENCODING = "e";
	public static String CMD_NON_PERSISTENT = "n";
	public static String CMD_REPLY_TO = "r";
	public static String CMD_OUTPUT = "o";
	public static String CMD_COUNT = "c";
	public static String CMD_JMS_HEADERS = "j";
	public static String CMD_COPY_QUEUE  = "C";
	public static String CMD_MOVE_QUEUE = "M";
	public static String CMD_FIND = "f";
	public static String CMD_SELECTOR = "s";
	public static String CMD_WAIT = "w";
	public static String CMD_USER = "U";
	public static String CMD_PASS = "P";
	public static String CMD_SET_HEADER = "H";
	public static String CMD_PRIORITY = "i";
	public static String CMD_JNDI = "J";
	public static String CMD_JNDI_CF = "F";
	public static String DEFAULT_COUNT_GET = "1";
	public static String DEFAULT_COUNT_ALL = "0";
	public static String DEFAULT_WAIT = "50";
	public static String TYPE_TEXT = "text";
	public static String DEFAULT_TYPE = TYPE_TEXT;
	public static String DEFAULT_DATE_FORMAT = "yyyy MM dd HH:mm:ss";

	public static void main(String[] args) throws ParseException, InterruptedException{
		A a = new A();
		a.run(args);
	}

	public void run(String[] args) throws InterruptedException{
		Options opts = new Options();
		opts.addOption(CMD_BROKER, "broker", true, "URL to broker. defaults to: tcp://localhost:61616");
		opts.addOption(CMD_GET,"get", false, "Get a message from destination");
		opts.addOption(CMD_PUT,"put", true, "Put a message. Specify data. if starts with @, a file is assumed and loaded");
		opts.addOption(CMD_TYPE,"type",true, "Message type to put, [bytes, text] - defaults to text");
		opts.addOption(CMD_ENCODING,"encoding",true,"Encoding of input file data. Default UTF-8");
		opts.addOption(CMD_NON_PERSISTENT,"non-persistent",false,"Set message to non persistent.");
		opts.addOption(CMD_REPLY_TO,"reply-to",true,"Set reply to destination, i.e. queue:reply");
		opts.addOption(CMD_OUTPUT,"output",true,"file to write payload to. If multiple messages, a -1.<ext> will be added to the file. BytesMessage will be written as-is, TextMessage will be written in UTF-8");
		opts.addOption(CMD_COUNT,"count",true,"A number of messages to browse,get or put (put will put the same message <count> times). 0 means all messages.");
		opts.addOption(CMD_JMS_HEADERS,"jms-headers",false,"Print JMS headers");
		opts.addOption(CMD_COPY_QUEUE,"copy-queue",true,"Copy all messages from this to target. Limited by maxBrowsePageSize in broker settings (default 400).");
		opts.addOption(CMD_MOVE_QUEUE,"move-queue",true,"Move all messages from this to target");
		opts.addOption(CMD_FIND, "find", true, "Search for messages in queue with this value in payload. Use with browse.");
		opts.addOption(CMD_SELECTOR,"selector",true,"Browse or get with selector");
		opts.addOption(CMD_WAIT,"wait",true,"Time to wait on get operation. Default 50. 0 equals infinity");
		opts.addOption(CMD_USER,"user",true,"Username to connect to broker");
		opts.addOption(CMD_PASS,"pass",true,"Password to connect to broker");
		opts.addOption(CMD_PRIORITY,"priority",true,"sets JMSPriority");
		opts.addOption(CMD_AMQP,"amqp",false,"Set protocol to AMQP. Defaults to OpenWire");
		opts.addOption(CMD_JNDI,"jndi",true,"Connect via JNDI. Overrides -b and -A options. Specify context file on classpath");
		opts.addOption(CMD_JNDI_CF,"jndi-cf-name",true,"Specify JNDI name for ConnectionFactory. Defaults to connectionFactory. Use with -J");

		@SuppressWarnings("static-access")
		Option property = OptionBuilder.withArgName("property=value" )
                .hasArgs(2)
                .withValueSeparator()
                .withDescription( "use value for given property. Can be used several times." )
                .create( CMD_SET_HEADER );

		opts.addOption(property);

		if( args.length == 0){
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp("java -jar a-<version>-with-dependencies.jar", opts, true);
			System.exit(0);
		}

		CommandLineParser cmdParser = new PosixParser();

		try {
			cmdLine = cmdParser.parse(opts, args);
			connect(cmdLine.getOptionValue(CMD_BROKER, "tcp://localhost:61616"),
					cmdLine.getOptionValue(CMD_USER),
					cmdLine.getOptionValue(CMD_PASS),
					cmdLine.hasOption(CMD_AMQP),
					cmdLine.getOptionValue(CMD_JNDI,""));

			 long startTime = System.currentTimeMillis();

			if( cmdLine.hasOption(CMD_GET)){
				executeGet(cmdLine);
			}else if(cmdLine.hasOption(CMD_PUT) ){
				executePut(cmdLine);
			}else if( cmdLine.hasOption(CMD_COPY_QUEUE)){
				executeCopy(cmdLine);
			}else if( cmdLine.hasOption(CMD_MOVE_QUEUE)){
				executeMove(cmdLine);
			}else{
				executeBrowse(cmdLine);
			}

		  long stopTime = System.currentTimeMillis();
		  long elapsedTime = stopTime - startTime;
		  output("Operation completed in ",Long.toString(elapsedTime),"ms (excluding connect)");
		} catch (ParseException pe) {
			pe.printStackTrace();
			return;
		} catch (JMSException je){
			je.printStackTrace();
			return;
		} catch (Exception e){
			e.printStackTrace();
			return;
		} finally{
			try {
				sess.close();
				conn.close();
			} catch (JMSException e2) {
				e2.printStackTrace();
			}
		}
		logger.debug("Active threads {}", Thread.activeCount());
		logger.debug("At the end of the road");
	}

	protected void executeMove(CommandLine cmdLine) throws JMSException, UnsupportedEncodingException, IOException {
		Queue tq = tsess.createQueue(cmdLine.getArgs()[0]);
		Queue q =  tsess.createQueue(cmdLine.getOptionValue(CMD_MOVE_QUEUE)); // Source
		MessageConsumer mq = null;
		MessageProducer mp = tsess.createProducer(tq);
		if( cmdLine.hasOption(CMD_SELECTOR)){ // Selectors
			mq = tsess.createConsumer(q,cmdLine.getOptionValue(CMD_SELECTOR));
		}else{
			mq = tsess.createConsumer(q);
		}
		int count = Integer.parseInt(cmdLine.getOptionValue(CMD_COUNT,DEFAULT_COUNT_ALL));
		int i = 0,j = 0;
		while(i < count || count == 0 ){
			Message msg = mq.receive(100L);
			if( msg == null){
				break;
			}else{
				mp.send(msg);
				tsess.commit();
				++j;
			}
		}
		output(j, " msgs moved from ", cmdLine.getOptionValue(CMD_MOVE_QUEUE),
				" to ",cmdLine.getArgs()[0]);
	}

	protected void executeCopy(CommandLine cmdLine) throws JMSException {
		Queue tq = sess.createQueue(cmdLine.getArgs()[0]);
		Queue q =  sess.createQueue(cmdLine.getOptionValue(CMD_COPY_QUEUE)); // Source
		QueueBrowser qb = null;
		MessageProducer mp = sess.createProducer(tq);
		if( cmdLine.hasOption(CMD_SELECTOR)){ // Selectors
			qb = sess.createBrowser(q,cmdLine.getOptionValue(CMD_SELECTOR));
		}else{
			qb = sess.createBrowser(q);
		}
		int count = Integer.parseInt(cmdLine.getOptionValue(CMD_COUNT,DEFAULT_COUNT_ALL));
		int i = 0, j = 0;
		@SuppressWarnings("unchecked")
		Enumeration<Message> en = qb.getEnumeration();
		while((i < count || count == 0) && en.hasMoreElements()){
			Message msg = en.nextElement();
			if( msg == null){
				break;
			}else{
				// if search is enabled
				if( cmdLine.hasOption(CMD_FIND)){
					if( msg instanceof TextMessage){
						String haystack = ((TextMessage)msg).getText();
						String needle = cmdLine.getOptionValue(CMD_FIND);
						if( haystack != null && haystack.contains(needle)){
							mp.send(msg);
							++j;
						}
					}
				}else{
					mp.send(msg);
					++j;
				}
				++i;
			}
		}
		output(j," msgs copied from ", cmdLine.getOptionValue(CMD_COPY_QUEUE), " to ", cmdLine.getArgs()[0]);
	}

	protected void connect(String url,String user, String password,boolean amqp,String jndi) throws JMSException {
		if( jndi == null || jndi == ""){
			cf = amqp ? createAMQPCF(url) : (new ActiveMQConnectionFactory(url));
		}else {
			// Initialize CF via JNDI.
			Properties properties = new Properties();
			try{
				// try classpath
				InputStream propertiesStream = getClass().getResourceAsStream(jndi);
				if( propertiesStream == null){
					// try absolut path
					propertiesStream = FileUtils.openInputStream(new File(jndi)); // will throw FNE if not found
				}
				// Read the hello.properties JNDI properties file and use contents to create the InitialContext.
				properties.load(propertiesStream);
				Context context = new InitialContext(properties);
				// Alternatively, JNDI information can be supplied by setting the "java.naming.factory.initial"
				// system property to value "org.apache.qpid.amqp_1_0.jms.jndi.PropertiesFileInitialContextFactory"
				// and setting the "java.naming.provider.url" system property as a URL to a properties file.
				cf = (ConnectionFactory) context.lookup(cmdLine.getOptionValue(CMD_JNDI_CF,"connectionFactory"));

			}catch(Exception e){
				throw new RuntimeException(e);
			}
		}
		
		if( user != null && password != null){
			conn = (Connection) cf.createConnection(user, password);
		}else{
			conn = cf.createConnection();
		}
		sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
		tsess = conn.createSession(true, Session.AUTO_ACKNOWLEDGE);
		conn.start();
	}

	protected ConnectionFactory createAMQPCF(String uri){
		try{
			return  ConnectionFactoryImpl.createFromURL(uri);
		}catch(MalformedURLException e){
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	protected void executeGet(CommandLine cmdLine) throws JMSException, UnsupportedEncodingException, IOException {
		Destination dest = createDestination(cmdLine.getArgs()[0]);
		MessageConsumer mq = null;
		if( cmdLine.hasOption(CMD_SELECTOR)){ // Selectors
			mq = sess.createConsumer(dest,cmdLine.getOptionValue(CMD_SELECTOR));
		}else{
			mq = sess.createConsumer(dest);
		}
		int count = Integer.parseInt(cmdLine.getOptionValue(CMD_COUNT,DEFAULT_COUNT_GET));
		long wait = Long.parseLong(cmdLine.getOptionValue(CMD_WAIT,DEFAULT_WAIT));
		int i = 0;
		while(i < count || i == 0 ){
			Message msg = mq.receive(wait);
			if( msg == null){
				output("No message received");
				break;
			}else{
				outputMessage(msg,cmdLine.hasOption(CMD_JMS_HEADERS));
				++i;
			}
		}
	}

	protected void executePut(CommandLine cmdLine) throws IOException, JMSException {
		// Check if we have properties to put
		Properties props = cmdLine.getOptionProperties(CMD_SET_HEADER);
		String type = cmdLine.getOptionValue(CMD_TYPE,DEFAULT_TYPE);
		String encoding = cmdLine.getOptionValue(CMD_ENCODING, Charset.defaultCharset().name());
		
		Message outMsg = null;
		// figure out input data
		String data = cmdLine.getOptionValue(CMD_PUT);
		if( data.startsWith("@")){
			// Load file.
			byte[] bytes = FileUtils.readFileToByteArray(new File(data.substring(1)));
			if( type.equals(TYPE_TEXT)){
				TextMessage textMsg = sess.createTextMessage(new String(bytes,encoding));
				outMsg = textMsg;
			}else{
				BytesMessage bytesMsg = sess.createBytesMessage();
				bytesMsg.writeBytes(bytes);
				outMsg = bytesMsg;
			}
		}else{
			TextMessage textMsg = sess.createTextMessage(data);
			outMsg = textMsg;
		}
		
		MessageProducer mp = sess.createProducer(createDestination(cmdLine.getArgs()[0]));
		if( cmdLine.hasOption("n")){;
			mp.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		}

		// enrich headers.
		for(Entry<Object,Object> p : props.entrySet()){
			outMsg.setObjectProperty((String)p.getKey(), p.getValue());
		}

		if( cmdLine.hasOption("r")){
			outMsg.setJMSReplyTo(createDestination(cmdLine.getOptionValue("r")));
		}
		
		if( cmdLine.hasOption(CMD_PRIORITY)){
			try{
				int priority = Integer.parseInt(cmdLine.getOptionValue(CMD_PRIORITY));
				mp.setPriority(priority);
			}catch(NumberFormatException nfe){
				throw new NumberFormatException("JMSPriority has to be an integer value");
			}
		}
		
		// send multiple messages?
		if( cmdLine.hasOption("c")){
			int count = Integer.parseInt(cmdLine.getOptionValue("c"));
			for(int i=0;i<count;i++){
				mp.send(outMsg);
			}
			output("", count," messages sent");
		}else{
			mp.send(outMsg);
			output("Message sent");
		}
	}

	// Accepts a plain name, queue://<name>, topic://<name> etc.
	protected Destination createDestination(String name) throws JMSException {
		// support queue:// as well.
		name = name.replace("/","");
		if( name.toLowerCase().startsWith("queue:")){
			return sess.createQueue(name.substring("queue:".length()));
		}else if( name.toLowerCase().startsWith("topic:")){
			return sess.createTopic(name.substring("topic:".length()));
		}else{
			return sess.createQueue(name);
		}
	}

	protected void executeBrowse(CommandLine cmdLine) throws JMSException, UnsupportedEncodingException, IOException {
		Queue q = sess.createQueue(cmdLine.getArgs()[0]);
		QueueBrowser qb = null;
		// Selector aware?
		if( cmdLine.hasOption(CMD_SELECTOR)){
			qb = sess.createBrowser(q,cmdLine.getOptionValue(CMD_SELECTOR));
		}else{
			qb = sess.createBrowser(q);
		}

		@SuppressWarnings("rawtypes")
		Enumeration en = qb.getEnumeration();
		int count = Integer.parseInt(cmdLine.getOptionValue(CMD_COUNT,DEFAULT_COUNT_ALL));
		int i = 0;
		while(en.hasMoreElements() && (i < count || count == 0 )){
			Object obj = en.nextElement();
			Message msg = (Message)obj;
			if( cmdLine.hasOption(CMD_FIND)){
				String needle = cmdLine.getOptionValue(CMD_FIND);
				// need to search for some payload value
				if( msg instanceof TextMessage){
					String haystack = ((TextMessage)msg).getText();
					if(haystack.contains(needle)){
						outputMessage(msg,cmdLine.hasOption(CMD_JMS_HEADERS));
					}
				}
			}else{
				outputMessage(msg,cmdLine.hasOption(CMD_JMS_HEADERS));
			}
			++i;
		}
	}

	protected void outputMessage(Message msg,boolean printJMSHeaders) throws JMSException, UnsupportedEncodingException, IOException {
		output("-----------------");
		if( printJMSHeaders ){
			outputHeaders(msg);
		}
		outputProperties(msg);
		// Output to file?
		FileOutputStream fos = null;
		File file = null;
		if( cmdLine.hasOption(CMD_OUTPUT)){
			file = getNextFilename(cmdLine.getOptionValue(CMD_OUTPUT,"amsg"),0);
			if( file != null ) {
				fos = new FileOutputStream(file);
			}
		}

		if( msg instanceof TextMessage){
			TextMessage txtMsg = (TextMessage)msg;
			if( fos != null){
				fos.write(txtMsg.getText().getBytes(cmdLine.getOptionValue(CMD_ENCODING,Charset.defaultCharset().name())));
				fos.close();
				output("Payload written to file ", file.getAbsolutePath());
			}else{
				output("Payload:");
				output(txtMsg.getText());
			}
		}else if( msg instanceof BytesMessage){
			BytesMessage bmsg = (BytesMessage)msg;
			byte[] bytes = new byte[(int) bmsg.getBodyLength()];
			bmsg.readBytes(bytes);
			if( fos != null ){
				fos.write(bytes);
				fos.close();
				output("Payload written to file ", file.getAbsolutePath());
			}else{
				output("Hex Payload:");
				output(bytesToHex(bytes));
			}
		}else{
			output("Unsupported message type: ", msg.getClass().getName());
		}
	}

	protected File getNextFilename(String suggestedFilename, int i) {
		String filename = suggestedFilename;
		if( i > 0 ){
			int idx = filename.lastIndexOf('.');
			if( idx == -1 ){
				filename = suggestedFilename + "-" + i;
			}else{
				// take care of the extension.
				filename = filename.substring(0,idx) + "-" + i + filename.substring(idx);
			}
		}
		File f = new File(filename);
		if( f.exists() ){
			return getNextFilename(suggestedFilename, ++i);
		}else{
			return f;
		}
	}

	protected void outputHeaders(Message msg){
		output("Message Headers");
		try {
		    String deliveryMode = msg.getJMSDeliveryMode() == DeliveryMode.PERSISTENT?"persistent":"non-persistent";
		    output("  JMSCorrelationID: " + msg.getJMSCorrelationID());
			output("  JMSExpiration: " + timestampToString(msg.getJMSExpiration()));
			output("  JMSDeliveryMode: " + deliveryMode);
			output("  JMSMessageID: " + msg.getJMSMessageID());
			output("  JMSPriority: " + msg.getJMSPriority());
			output("  JMSTimestamp: " + timestampToString(msg.getJMSTimestamp()));
			output("  JMSType: " + msg.getJMSType());
			output("  JMSDestination: " + (msg.getJMSDestination() != null ? msg.getJMSDestination().toString() : "Not set"));
			output("  JMSRedelivered: " + Boolean.toString(msg.getJMSRedelivered()));
			output("  JMSReplyTo: " + (msg.getJMSReplyTo() != null ? msg.getJMSReplyTo().toString() : "Not set"));
		} catch (JMSException e) {
			// nothing to do here. just ignore.
			logger.debug("Cannot print JMS headers."  + e.getMessage());
		}
	}

	protected String timestampToString(long timestamp){
		Date date = new Date(timestamp);
	    Format format = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
	    String timeString =  format.format(date).toString();
	    return timeString;
	}

	protected void outputProperties(Message msg) throws JMSException {
		output("Message Properties");
		@SuppressWarnings("unchecked")
		Enumeration<String> en = msg.getPropertyNames();
		while(en.hasMoreElements()){
			String name = en.nextElement();
			Object property = msg.getObjectProperty(name);
			output("  ", name ,": ",property.toString());
		}
	}

	protected void output(Object... args){
		output.output(args);
	}

	//Byte flippin magic. Gotta love it.
	protected String bytesToHex(byte[] bytes) {
		final char[] hexArray = "0123456789ABCDEF".toCharArray();
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}
