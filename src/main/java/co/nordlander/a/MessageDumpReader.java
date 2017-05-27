package co.nordlander.a;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.SerializationUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MessageDumpReader {
	
	protected Session session;
	
	public MessageDumpReader(final Session session){
		this.session = session;
	}

	public List<Message> loadMessagesFromJson(String json) throws JsonParseException, JsonMappingException, IOException, JMSException{
		ObjectMapper om = new ObjectMapper();
		List<MessageDump> msgDumps = om.readValue(json, new TypeReference<List<MessageDump>>(){});
		List<Message> messages = new ArrayList<Message>(msgDumps.size());
		for(MessageDump dump : msgDumps){
			messages.add(pojoToMessage(dump));
		}
		return messages;
	}
	
	protected Message pojoToMessage(MessageDump dump) throws JMSException {
		Message msg = null;
		if ("TextMessage".equals(dump.type) ) {
			TextMessage tm = session.createTextMessage(dump.body);
			msg = tm;
		} else if ( "BytesMessage".equals(dump.type) ) {
			BytesMessage bm = session.createBytesMessage();
			byte[] messageBytes = Base64.decodeBase64(dump.body);
			bm.writeBytes(messageBytes);
			msg = bm;
		} else if ("ObjectMessage".equals(dump.type)) {
			byte[] objectBytes = Base64.decodeBase64(dump.body);
			Serializable theObject = SerializationUtils.deserialize(objectBytes);
			ObjectMessage om = session.createObjectMessage(theObject);
			msg = om;
		} else {
			throw new RuntimeException("Illegal type: " + dump.type);
		}
		
		for( Map.Entry<String, Boolean> entry : dump.boolProperties.entrySet() ) {
			msg.setBooleanProperty(entry.getKey(), entry.getValue());
		}
		
		for( Map.Entry<String, String> entry : dump.stringProperties.entrySet() ) {
			msg.setStringProperty(entry.getKey(), entry.getValue());
		}
		
		for( Map.Entry<String, Short> entry : dump.shortProperties.entrySet() ) {
			msg.setShortProperty(entry.getKey(), entry.getValue());
		}
		
		for( Map.Entry<String, Integer> entry : dump.intProperties.entrySet() ) {
			msg.setIntProperty(entry.getKey(), entry.getValue());
		}
		
		for( Map.Entry<String, Long> entry : dump.longProperties.entrySet() ) {
			msg.setLongProperty(entry.getKey(), entry.getValue());
		}
		
		for( Map.Entry<String, Float> entry : dump.floatProperties.entrySet() ) {
			msg.setFloatProperty(entry.getKey(), entry.getValue());
		}
		
		for( Map.Entry<String, Double> entry : dump.doubleProperties.entrySet() ) {
			msg.setDoubleProperty(entry.getKey(), entry.getValue());
		}
		
		for( Map.Entry<String, Byte> entry : dump.byteProperties.entrySet() ) {
			msg.setByteProperty(entry.getKey(), entry.getValue());
		}
		
		for( Map.Entry<String,String> entry : dump.objectProperties.entrySet() ) {
			byte[] objectBytes = Base64.decodeBase64(entry.getValue());
			Serializable theObject = SerializationUtils.deserialize(objectBytes);
			msg.setObjectProperty(entry.getKey(),theObject);
		}
		
		
		if( dump.JMSRedelivered != null) {
			msg.setJMSRedelivered(dump.JMSRedelivered);
		}
		
		if (dump.JMSCorrelationID != null) {
			msg.setJMSCorrelationID(dump.JMSCorrelationID);
		}
		
		if( dump.JMSDeliveryMode != null ) {
			msg.setJMSDeliveryMode(dump.JMSDeliveryMode);
		}
		
		if( dump.JMSExpiration != null){
			msg.setJMSExpiration(dump.JMSExpiration);
		}
		
		if( dump.JMSMessageID != null) {
			msg.setJMSMessageID(dump.JMSMessageID);
		}
		
		if (dump.JMSTimestamp != null) {
			msg.setJMSTimestamp(dump.JMSTimestamp);
		}
		
		if (dump.JMSType != null) {
			msg.setJMSType(dump.JMSType);
		}
		
		return msg;
	}
}
