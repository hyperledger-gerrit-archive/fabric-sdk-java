/*
 *
 *  Copyright IBM Corp. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.hyperledger.fabric.sdk.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import org.hyperledger.fabric.protos.idemix.Idemix;
import org.hyperledger.fabric.protos.msp.MspConfig;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.junit.BeforeClass;
import org.junit.Test;

public class IdemixUserStoreTest {

	private static IdemixUserStore store;

	static final String USER_ID = "johndoe";

    private static final String STORE_PATH = "src/test/fixture/IdemixUserStoreTest/";
    
    private static final String MSP_ID = "MSP1OU1";
    private static final String MSP_INVALID_IPK = "MSPInvalidIPK";
    private static final String MSP_BROKEN_IPK = "MSP1Broken";

    // Setup using a happy path
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		try {
			store = new IdemixUserStore(STORE_PATH, MSP_ID);
		} catch (Exception e) {
			fail("Unexpected exception while create IdemixUserStore : " + e.getMessage());
		}
		assertNotNull(store);
	}
	
    // Test creating a IdemixUserStore with null store path (should fail)
	// This also covers readIdemixIssuerPublicKey - IOException case
    @Test(expected = NullPointerException.class)
    public void testIdemixUserStoreInputNullStorePath() throws CryptoException {
    	new IdemixUserStore(null, MSP_ID);
    }	
    
    // Test creating a IdemixUserStore with null MSP ID (should fail)
	// This also covers readIdemixIssuerPublicKey - IOException case
    @Test(expected = NullPointerException.class)
    public void testIdemixUserStoreInputNullMspID() throws CryptoException {
		new IdemixUserStore(STORE_PATH, null);
    }	

    // Test creating a IdemixUserStore with invalid Issuer Public Key (should fail)
    @Test(expected = CryptoException.class)
    public void testIdemixUserStoreInputBrokenMSP() throws CryptoException {
		new IdemixUserStore(STORE_PATH, MSP_BROKEN_IPK);
    }
    
    // Test creating a IdemixUserStore with invalid Issuer Public Key (should fail)
	// It also covers readIdemixIssuerPublicKey - InvalidProtocolBufferException case.
    @Test(expected = IllegalArgumentException.class)
    public void testIdemixUserStoreInputInvalidIPK() throws CryptoException {
		new IdemixUserStore(STORE_PATH, MSP_INVALID_IPK);
    }
    
	// Test for get user
 	@Test
	public void testGetUser() {
		User user = null;
		try {
			user = store.getUser(USER_ID);
		} catch (Exception e) {
			fail("Unexpected exception while get user : " + e.getMessage());
		}
		assertNotNull(user);
		assertEquals(USER_ID, user.getName());
	}
    
	// Test for get user with no signer config (should fail)
	@Test(expected = IOException.class)
	public void testGetUserNotExist() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		store.getUser("doesNotExist");
	}
	
	// Test for protected / utility methods

	@Test
	public void testReadIdemixMSPConfig() {
		MspConfig.IdemixMSPSignerConfig config = null;
		try {
			config = store.readIdemixMSPConfig(USER_ID);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Unexpected exception while reading signerconfig: " + e.getMessage());
		}
		assertNotNull(config);
	}

	@Test
	public void testReadIdemixIssuerPublicKey() {
		Idemix.IssuerPublicKey ipk = store.readIdemixIssuerPublicKey();
		assertNotNull(ipk);
	}

	@Test
	public void testReadIdemixRevocationPublicKey() {
		PublicKey rpk = null;
		try {
			rpk = store.readIdemixRevocationPublicKey(MSP_ID);
		} catch (Exception e) {
			fail("Unexpected exception while reading Idemix Revocation Public Key : " + e.getMessage());
		}
		assertNotNull(rpk);
	}
	
}
