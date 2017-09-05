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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.SerializationUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Writes JSON string from a list of MessageDump messages.
 * @author Petter Nordlander
 *
 */
public class MessageDumpWriter {

	public String messagesToJsonString(List<Message> messages) throws JMSException, JsonProcessingException {
		
		List<MessageDump> dumpedMessages = new ArrayList<MessageDump>(messages.size());
		for( Message message : messages) {
			dumpedMessages.add(toDumpMessage(message));
		}
		ObjectMapper om = new ObjectMapper();
		return om.writeValueAsString(dumpedMessages);
	}
	
	public String toJson(List<MessageDump> msgs) throws JsonProcessingException {
		ObjectMapper om = new ObjectMapper();
		return om.writeValueAsString(msgs);
	}
	
	public List<MessageDump> toDumpMessages(List<Message> msgs) throws JMSException{
		List<MessageDump> dump = new ArrayList<MessageDump>();
		for( Message msg : msgs){
			dump.add(toDumpMessage(msg));
		}
		return dump;
	}
	
	public MessageDump toDumpMessage(Message msg) throws JMSException{
		
		MessageDump dump = new MessageDump();
		dump.JMSCorrelationID = msg.getJMSCorrelationID();
		dump.JMSMessageID = msg.getJMSMessageID();
		dump.JMSType = msg.getJMSType();
		dump.JMSDeliveryMode =  msg.getJMSDeliveryMode();
		dump.JMSExpiration = msg.getJMSExpiration();
		dump.JMSRedelivered = msg.getJMSRedelivered();
		dump.JMSTimestamp =  msg.getJMSTimestamp();
		dump.JMSPriority = msg.getJMSPriority();
		
		@SuppressWarnings("rawtypes")
		Enumeration propertyNames = msg.getPropertyNames();
		while(propertyNames.hasMoreElements()){
			System.out.println("looping!");
			String property = (String) propertyNames.nextElement();
			Object propertyValue = msg.getObjectProperty(property);
			if( propertyValue instanceof String){
				dump.stringProperties.put(property, (String)propertyValue);
			} else if ( propertyValue instanceof Integer ){
				dump.intProperties.put(property, (Integer)propertyValue);
			} else if ( propertyValue instanceof Long) {
				dump.longProperties.put(property, (Long)propertyValue);
			} else if( propertyValue instanceof Double) {
				dump.doubleProperties.put(property, (Double) propertyValue);
			} else if (propertyValue instanceof Short) {
				dump.shortProperties.put(property, (Short)propertyValue);
			} else if (propertyValue instanceof Float) {
				dump.floatProperties.put(property, (Float) propertyValue);
			} else if (propertyValue instanceof Byte) {
				dump.byteProperties.put(property, (Byte)propertyValue);
			} else if (propertyValue instanceof Boolean) {
				dump.boolProperties.put(property, (Boolean)propertyValue);
			} else if (propertyValue instanceof Serializable){
				// Object property.. if it's on Classpath and Serializable
				byte[] propBytes = SerializationUtils.serialize((Serializable) propertyValue);
				dump.objectProperties.put(property, Base64.encodeBase64String(propBytes));
			} else {
				// Corner case.
				throw new IllegalArgumentException("Property of key '"+ property +"' is not serializable. Type is: " + propertyValue.getClass().getCanonicalName());
			}
		}
		
		dump.body = "";
		dump.type = "";
		
		if (msg instanceof TextMessage) {
			dump.body = ((TextMessage)msg).getText();
			dump.type = "TextMessage";
		} else if (msg instanceof BytesMessage) {
			BytesMessage bm = (BytesMessage)msg;
			byte[] bytes = new byte[(int) bm.getBodyLength()];
			bm.readBytes(bytes);
			dump.body = Base64.encodeBase64String(bytes);
			dump.type = "BytesMessage";
		} else if (msg instanceof ObjectMessage) {
			ObjectMessage om = (ObjectMessage)msg;
			byte[] objectBytes = SerializationUtils.serialize(om.getObject());
			dump.body = Base64.encodeBase64String(objectBytes);
			dump.type = "ObjectMessage";
		}
		return dump;
	}
	
}
