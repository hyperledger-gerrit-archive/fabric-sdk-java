package org.hyperledger.fabric.sdk.user;

import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.identity.SigningIdentity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IdemixUserTest {

    private static final String TEST_PATH = "src/test/resources/idemix/";
    private static final String MSP1OU1 = "MSP1OU1";
    private static final String SIGNER_CONFIG = "SignerConfig";

    @Test
    public void testIdemixUser() {
        try {
            IdemixUserStore store = new IdemixUserStore(TEST_PATH, MSP1OU1);
            User user = store.getUser(SIGNER_CONFIG);
            assertNotNull(user);
            assertEquals(MSP1OU1, user.getMspId());

            SigningIdentity si = user.getSigningIdentity();
            assertNotNull(si);

            // Sign
            byte[] msg = "Hello World!!!".getBytes();
            byte[] sigma = si.sign(msg);

            // Verify
            boolean valid = si.verifySignature(msg, sigma);
            assertTrue(valid);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

}
