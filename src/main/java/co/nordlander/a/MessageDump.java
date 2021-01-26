package co.nordlander.a;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

public class MessageDump {

	public String JMSCorrelationID;
	public String JMSMessageID;
	public String JMSType;
	public Integer JMSDeliveryMode;
	public Long JMSExpiration;
	public Boolean JMSRedelivered;
	public Long JMSTimestamp;
	public Integer JMSPriority;
	
	public Map<String,String> stringProperties = new HashMap<>();
	public Map<String,Integer> intProperties = new HashMap<>();
	public Map<String,Long> longProperties = new HashMap<>();
	public Map<String,Float> floatProperties = new HashMap<>();
	public Map<String,Double> doubleProperties = new HashMap<>();
	public Map<String,Boolean> boolProperties = new HashMap<>();
	public Map<String,Short> shortProperties = new HashMap<>();
	public Map<String,Byte> byteProperties = new HashMap<>();
	public Map<String,String> objectProperties = new HashMap<>();
	
	public String body;
	public String type;
	
	/**
	 * Accessor method to ByteMessage payload for JavaScript transformers.
	 * @param text string to encode
	 * @param charset java charset name. i.e. UTF-8
	 * @throws UnsupportedEncodingException
	 */
	public void encode(String text, String charset) throws UnsupportedEncodingException {
		if ( type != "BytesMessage") { //TODO mabe add support for Map messages as well.
			throw new IllegalArgumentException("Encode is only applicable to BytesMessages.");
		}
		
		body = Base64.encodeBase64String(text.getBytes(charset));
	}
	
	/**
	 * Accessor method to ByteMessage payloada for JavaScript transformers.
	 * @param charset java charset name. i.e. UTF-8. You need to know payload charset!
	 * @return byte payload as string
	 * @throws UnsupportedEncodingException
	 */
	public String decode(String charset) throws UnsupportedEncodingException {
		if ( type != "BytesMessage") { //TODO mabe add support for Map messages as well.
			throw new IllegalArgumentException("Decode is only applicable to BytesMessages.");
		}
		
		return new String(Base64.decodeBase64(body), charset);
	}
	
}
