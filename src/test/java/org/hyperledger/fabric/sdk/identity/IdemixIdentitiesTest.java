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

package org.hyperledger.fabric.sdk.identity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.milagro.amcl.FP256BN.BIG;
import org.apache.milagro.amcl.FP256BN.ECP;
import org.apache.milagro.amcl.RAND;
import org.hyperledger.fabric.protos.idemix.Idemix;
import org.hyperledger.fabric.protos.msp.Identities;
import org.hyperledger.fabric.protos.msp.MspConfig.IdemixMSPSignerConfig;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.idemix.Credential;
import org.hyperledger.fabric.sdk.idemix.IssuerPublicKey;
import org.hyperledger.fabric.sdk.idemix.Pseudonym;
import org.hyperledger.fabric.sdk.idemix.Signature;
import org.hyperledger.fabric.sdk.idemix.Utils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for IdemixIdentity and IdemixSigningIdentity
 */
public class IdemixIdentitiesTest {

    // Test resources with crypto material generated by the idemixgen tool (in go)
    private static final String TEST_PATH = "src/test/resources/idemix/";
    private static final String USER_PATH = "/user/";
    private static final String VERIFIER_PATH = "/msp/";
    private static final String MSP1OU1 = "MSP1OU1";
    private static final String MSP1OU1Admin = "MSP1OU1Admin";
    private static final String MSP1OU2 = "MSP1OU2";
    private static final String MSP1Verifier = "MSP1Verifier";
    private static final String MSP2OU1 = "MSP2OU1";
    private static final String SIGNER_CONFIG = "SignerConfig";
    private static final String IPK_CONFIG = "IssuerPublicKey";

    // Exception messages
    private static final String MSG_NULL_INPUT = "Input must not be null";
    private static final String MSG_WRONG_SIG = "Could not parse Idemix Nym Signature";

    private static final RAND rng = Utils.getRand();

    @Test
    public void testIdemixMSPSignerConfig() {

        // Test creating an identity with MSP1Verifier (should fail)
        IdemixMSPSignerConfig signerConfig = readIdemixMSPConfig(TEST_PATH + MSP1Verifier + USER_PATH, SIGNER_CONFIG);
        assertNull(signerConfig);

        signerConfig = readIdemixMSPConfig(TEST_PATH + MSP1OU1 + USER_PATH, SIGNER_CONFIG);
        assertNotNull(signerConfig);

    }

    @Test
    public void testIdemixSigningIdentity() {
        // Parse crypto material from files
        IdemixMSPSignerConfig signerConfig = null;
        signerConfig = readIdemixMSPConfig(TEST_PATH + MSP1OU1 + USER_PATH, SIGNER_CONFIG);
        assertNotNull(signerConfig);

        Idemix.IssuerPublicKey ipkProto = readIdemixIssuerPublicKey(TEST_PATH + MSP1OU1 + VERIFIER_PATH, IPK_CONFIG);
        IssuerPublicKey ipk = new IssuerPublicKey(ipkProto);
        assertTrue(ipk.check());

        BIG sk = BIG.fromBytes(signerConfig.getSk().toByteArray());

        Idemix.Credential credProto = null;
        try {
            credProto = Idemix.Credential.parseFrom(signerConfig.getCred());
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            fail("Could not parse a credential");
        }

        assertNotNull(credProto);

        Credential cred = new Credential(credProto);

        IdemixSigningIdentity signingIdentity = null;
        try {
            signingIdentity = new IdemixSigningIdentity(ipk, MSP1OU1, sk, cred, null, null, rng);
        } catch (CryptoException e) {
            fail("Could not create Idemix Signing Identity" + e.getMessage());
        }
        assertNotNull(signingIdentity);
        try {
            signingIdentity = new IdemixSigningIdentity(null, MSP1OU1, sk, cred,  null, null, rng);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }
        try {
            signingIdentity = new IdemixSigningIdentity(ipk, null, sk, cred, null, null, rng);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }
        try {
            signingIdentity = new IdemixSigningIdentity(ipk, "", sk, cred, null, null, rng);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }
        try {
            signingIdentity = new IdemixSigningIdentity(ipk, MSP1OU1, null, cred, null, null, rng);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }
        try {
            signingIdentity = new IdemixSigningIdentity(ipk, MSP1OU1, sk, null, null, null, rng);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }
        // Test other constructors
        Pseudonym nym = signingIdentity.getNym();
        Signature proof = signingIdentity.getProof();
        IdemixSigningIdentity signingIdenityNym = null;
        IdemixSigningIdentity signingIdenityNymProof = null;
        try {
            signingIdenityNymProof = new IdemixSigningIdentity(ipk, MSP1OU1, sk, cred, nym, proof, rng);
        } catch (CryptoException e) {
            fail("Could not create Idemix Signing Identity" + e.getMessage());
        }
        assertNotNull(signingIdenityNymProof);
        try {
            signingIdenityNymProof = new IdemixSigningIdentity(null, MSP1OU1, sk, cred, nym, proof, rng);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }
        try {
            signingIdenityNymProof = new IdemixSigningIdentity(ipk, null, sk, cred, nym, proof, rng);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }
        try {
            signingIdenityNymProof = new IdemixSigningIdentity(ipk, "", sk, cred, nym, proof, rng);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }
        try {
            signingIdenityNymProof = new IdemixSigningIdentity(ipk, MSP1OU1, null, cred, nym, proof, rng);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }
        try {
            signingIdenityNymProof = new IdemixSigningIdentity(ipk, MSP1OU1, sk, null, nym, proof, rng);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }
        try {
            signingIdenityNymProof = new IdemixSigningIdentity(ipk, MSP1OU1, sk, cred, null, proof, rng);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }
        try {
            signingIdenityNymProof = new IdemixSigningIdentity(ipk, MSP1OU1, sk, cred, nym, null, rng);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }
    }

    @Test
    public void testSigning() {

        IdemixSigningIdentity signingIdentity = createIdemixSigningIdentity(MSP1OU1);

        byte[] msg = {1, 2, 3, 4};
        byte[] sig = {1, 2, 3, 4};
        boolean b = false;

        try {
            b = signingIdentity.verifySignature(msg, null);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }

        try {
            b = signingIdentity.verifySignature(null, sig);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }

        try {
            b = signingIdentity.verifySignature(msg, sig);
        } catch (CryptoException e) {
            assertEquals(MSG_WRONG_SIG, e.getMessage());
        }

        try {
            sig = signingIdentity.sign(null);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }

        try {
            sig = signingIdentity.sign(msg);
        } catch (CryptoException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        try {
            b = signingIdentity.verifySignature(msg, sig);
        } catch (CryptoException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        assertTrue(b);
    }

    @Test
    public void testIdemixIdentity() {

        ECP nym = null;
        byte[] ou = null;
        byte[] role = null;
        Signature associationProof = null;
        IdemixIdentity idemixIdentity = null;

        // Prepare objects
        IdemixSigningIdentity signingIdentity = createIdemixSigningIdentity(MSP1OU1);
        assertNotNull(signingIdentity);

        // Test serializing this identity
        Identities.SerializedIdentity proto = signingIdentity.createSerializedIdentity();
        assertNotNull(proto);

        Identities.SerializedIdemixIdentity idemixProto = null;
        try {
            idemixProto = Identities.SerializedIdemixIdentity.parseFrom(proto.getIdBytes());
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            fail("Could not parse Idemix Serialized Identity" + e.getMessage());
        }
        if (idemixProto != null) {
            nym = new ECP(BIG.fromBytes(idemixProto.getNymX().toByteArray()), BIG.fromBytes(idemixProto.getNymY().toByteArray()));
            ou = idemixProto.getOU().toByteArray();
            role = idemixProto.getRole().toByteArray();
            try {
                associationProof = new Signature(Idemix.Signature.parseFrom(idemixProto.getProof().toByteArray()));
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
                fail("Cannot deserialize proof" + e.getMessage());
            }
        }

        try {
            idemixIdentity = new IdemixIdentity(MSP1OU1, nym, ou, role, associationProof);
        } catch (CryptoException e) {
            fail("Cannot create Idemix Identity" + e.getMessage());
        }

        assertNotNull(idemixIdentity);

        try {
            idemixIdentity = new IdemixIdentity(null);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }

        try {
            idemixIdentity = new IdemixIdentity(null, nym, ou, role, associationProof);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }

        try {
            idemixIdentity = new IdemixIdentity("", nym, ou, role, associationProof);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }

        try {
            idemixIdentity = new IdemixIdentity(MSP1OU1, null, ou, role, associationProof);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }

        try {
            idemixIdentity = new IdemixIdentity(MSP1OU1, nym, null, role, associationProof);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }

        try {
            idemixIdentity = new IdemixIdentity(MSP1OU1, nym, ou, null, associationProof);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }

        try {
            idemixIdentity = new IdemixIdentity(MSP1OU1, nym, ou, role, null);
        } catch (CryptoException e) {
            assertEquals(MSG_NULL_INPUT, e.getMessage());
        }

    }

    @Test
    public void testIdemixSigningIdentities() {

        // Test creating an identity with MSP1OU1
        IdemixSigningIdentity signingIdentity1 = createIdemixSigningIdentity(MSP1OU1);
        assertNotNull(signingIdentity1);

        // Test signing using this identity
        assertTrue(testSigning(signingIdentity1));

        // Test serializing this identity
        Identities.SerializedIdentity si = signingIdentity1.createSerializedIdentity();
        assertNotNull(si);

        IdemixIdentity id1 = null;
        try {
            id1 = new IdemixIdentity(si);
        } catch (CryptoException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        assertNotNull(id1);

        // Test creating an identity with MSP1OU1Admin
        IdemixSigningIdentity signingIdentity2 = createIdemixSigningIdentity(MSP1OU1Admin);
        assertNotNull(signingIdentity2);

        // Test signing using this identity
        assertTrue(testSigning(signingIdentity2));

        // Test creating an identity with MSP1OU2
        IdemixSigningIdentity signingIdentity3 = createIdemixSigningIdentity(MSP1OU2);
        assertNotNull(signingIdentity3);

        // Test signing using this identity
        assertTrue(testSigning(signingIdentity3));

        // Test creating an identity with MSP2OU1
        IdemixSigningIdentity signingIdentity4 = createIdemixSigningIdentity(MSP2OU1);
        assertNotNull(signingIdentity4);

        // Test signing using this identity
        assertTrue(testSigning(signingIdentity4));

    }


    public boolean testSigning(IdemixSigningIdentity signingIdentity) {

        byte[] msg = {1, 2, 3, 4};
        byte[] sig = null;

        try {
            sig = signingIdentity.sign(msg);
        } catch (CryptoException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        try {
            return signingIdentity.verifySignature(msg, sig);
        } catch (CryptoException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        return false;
    }

    /**
     * Helper function to create IdemixSigningIdentity from a file generated by idemixgen go tool
     *
     * @param mspId
     * @return IdemixSigningIdentity object
     */
    public IdemixSigningIdentity createIdemixSigningIdentity(String mspId) {
        IdemixMSPSignerConfig signerConfig = null;
        signerConfig = readIdemixMSPConfig(TEST_PATH + mspId + USER_PATH, SIGNER_CONFIG);
        assertNotNull(signerConfig);

        Idemix.IssuerPublicKey ipkProto = readIdemixIssuerPublicKey(TEST_PATH + mspId + VERIFIER_PATH, IPK_CONFIG);
        IssuerPublicKey ipk = new IssuerPublicKey(ipkProto);
        assertTrue(ipk.check());

        BIG sk = BIG.fromBytes(signerConfig.getSk().toByteArray());

        Idemix.Credential credProto = null;
        try {
            credProto = Idemix.Credential.parseFrom(signerConfig.getCred());
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            fail("Could not parse a credential");
        }

        assertNotNull(credProto);

        Credential cred = new Credential(credProto);

        IdemixSigningIdentity signingIdenity = null;
        try {
            signingIdenity = new IdemixSigningIdentity(ipk, mspId, sk, cred, null, null, rng);
        } catch (CryptoException e) {
            e.printStackTrace();
        }

        return signingIdenity;

    }

    /**
     * Helper function: parse Idemix MSP Signer config (is part of the MSPConfig proto) from path
     *
     * @param configPath
     * @param id
     * @return IdemixMSPSignerConfig proto
     */
    public IdemixMSPSignerConfig readIdemixMSPConfig(String configPath, String id) {

        Path path = Paths.get(configPath + id);
        byte[] data = null;
        try {
            data = Files.readAllBytes(path);
        } catch (IOException e) {
            return null;
        }

        IdemixMSPSignerConfig signerConfig = null;
        try {
            signerConfig = IdemixMSPSignerConfig.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            return null;
        }

        return signerConfig;
    }

    /**
     * Parse Idemix issuer public key from the config file
     *
     * @param configPath
     * @param id
     * @return Idemix IssuerPublicKey proto
     */
    public Idemix.IssuerPublicKey readIdemixIssuerPublicKey(String configPath, String id) {

        Path path = Paths.get(configPath + id);
        byte[] data = null;
        try {
            data = Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Idemix.IssuerPublicKey ipk = null;

        try {
            ipk = Idemix.IssuerPublicKey.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        return ipk;
    }

}
