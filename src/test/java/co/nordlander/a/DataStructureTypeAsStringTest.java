package co.nordlander.a;

import org.apache.activemq.command.CommandTypes;
import org.junit.Assert;
import org.junit.Test;

/**
 * Testing datastructure type conversion to String.
 */
public class DataStructureTypeAsStringTest {

	@Test
	public void assertBrokerInfo() {
		A a = new A();
		Assert.assertEquals("BROKER_INFO",a.dataStructureTypeToString(CommandTypes.BROKER_INFO));
	}
	
	@Test
	public void assertUnknownForUnknown() {
		A a = new A();
		Assert.assertEquals("unknown",a.dataStructureTypeToString((byte)254));
	}
}
