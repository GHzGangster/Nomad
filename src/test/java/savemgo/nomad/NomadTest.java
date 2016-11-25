package savemgo.nomad;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class NomadTest extends TestCase {

	public NomadTest(String testName) {
		super(testName);
	}

	public static Test suite() {
		return new TestSuite(NomadTest.class);
	}

	public void testApp() {
		assertTrue(true);
	}

}
