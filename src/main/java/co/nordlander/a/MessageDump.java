package co.nordlander.a;

import java.util.HashMap;
import java.util.Map;

public class MessageDump {

	public String JMSCorrelationID;
	public String JMSMessageID;
	public String JMSType;
	public Integer JMSDeliveryMode;
	public Long JMSExpiration;
	public Boolean JMSRedelivered;
	public Long JMSTimestamp;
	
	public Map<String,String> stringProperties = new HashMap<String,String>();
	public Map<String,Integer> intProperties = new HashMap<String,Integer>();
	public Map<String,Long> longProperties = new HashMap<String,Long>();
	public Map<String,Float> floatProperties = new HashMap<String,Float>();
	public Map<String,Double> doubleProperties = new HashMap<String,Double>();
	public Map<String,Boolean> boolProperties = new HashMap<String,Boolean>();
	public Map<String,Short> shortProperties = new HashMap<String,Short>();
	public Map<String,Byte> byteProperties = new HashMap<String,Byte>();
	public Map<String,String> objectProperties = new HashMap<String,String>();
	
	public String body;
	public String type;
	
}
