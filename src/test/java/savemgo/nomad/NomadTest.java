package savemgo.nomad;

import com.google.gson.JsonObject;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import savemgo.nomad.campbell.Campbell;

public class NomadTest extends TestCase {

	public NomadTest(String testName) {
		super(testName);
	}

	public static Test suite() {
		return new TestSuite(NomadTest.class);
	}
	
	public void testCampbell() {
		Campbell campbell = Campbell.instance();
		campbell.setBaseUrl("https://api.savemgo.com/campbell/");
		
		JsonObject response = campbell.getResponse("hub", "getLobbyList");
		assertTrue(Campbell.checkResult(response));
	}

}
