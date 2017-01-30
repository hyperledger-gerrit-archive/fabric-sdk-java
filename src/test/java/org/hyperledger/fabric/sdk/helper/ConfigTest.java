package org.hyperledger.fabric.sdk.helper;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConfigTest {

	public static Config config;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		config = Config.getConfig();
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testGetConfig() {
		assertEquals(config.getSecurityLevel(), 256);
		assertEquals(config.getHashAlgorithm(), "SHA2") ;
		String[] cacerts = config.getPeerCACerts();
		assertEquals(cacerts[0], "src/resources/peercacert.pem" );	
	}
}
