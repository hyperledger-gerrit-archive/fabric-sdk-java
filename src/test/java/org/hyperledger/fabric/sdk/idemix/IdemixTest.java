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

package org.hyperledger.fabric.sdk.idemix;

import java.security.KeyPair;

import org.apache.milagro.amcl.FP256BN.BIG;
import org.apache.milagro.amcl.FP256BN.ECP;
import org.hyperledger.fabric.protos.idemix.Idemix;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class IdemixTest {

    @Test
    public void testIdemix() throws CryptoException {
        // Test WeakBB
        // Generate a new key pair
        WeakBB.KeyPair keypair = WeakBB.weakBBKeyGen();
        // Random message to sign
        BIG wbbMessage = IdemixUtils.randModOrder();
        // Sign the message with keypair secret key
        ECP wbbSignature = WeakBB.weakBBSign(keypair.getSk(), wbbMessage);
        // Check the signature with valid PK and valid message
        assertTrue(WeakBB.weakBBVerify(keypair.getPk(), wbbSignature, wbbMessage));
        // Try to check a random message
        assertFalse(WeakBB.weakBBVerify(keypair.getPk(), wbbSignature, IdemixUtils.randModOrder()));

        // Test idemix functionality
        String[] attributeNames = {"Attr1", "Attr2", "Attr3", "Attr4", "Attr5"};
        IdemixIssuerKey key = new IdemixIssuerKey(attributeNames);
        // check that the issuer public key is valid
        assertTrue(key.getIpk().check());

        // Test serialization of issuer public key
        assertTrue(new IdemixIssuerPublicKey(key.getIpk().toProto()).check());

        // Test issuance
        // Choose a user secret key and request a credential
        BIG sk = new BIG(IdemixUtils.randModOrder());
        BIG issuerNonce = new BIG(IdemixUtils.randModOrder());
        IdemixCredRequest m = new IdemixCredRequest(sk, issuerNonce, key.getIpk());
        assertTrue(m.check(key.getIpk()));

        // Test serialization of cred request
        assertTrue(new IdemixCredRequest(m.toProto()).check(key.getIpk()));

        // Issue a credential
        BIG[] attrs = new BIG[attributeNames.length];
        for (int i = 0; i < attributeNames.length; i++) {
            attrs[i] = new BIG(i);
        }
        IdemixCredential c = new IdemixCredential(key, m, attrs);

        assertTrue(c.verify(sk, key.getIpk()));

        // Generate a revocation key pair
        KeyPair revocationKeyPair = RevocationAuthority.generateLongTermRevocationKey();
        assertNotNull(revocationKeyPair);

        // Create CRI that contains no revocation mechanism
        int epoch = 0;
        BIG[] rhIndex = {new BIG(0)};
        Idemix.CredentialRevocationInformation cri = RevocationAuthority.createCRI(revocationKeyPair.getPrivate(), rhIndex, epoch, RevocationAlgorithm.ALG_NO_REVOCATION);

        // Test serialization of IdemixCredential
        assertTrue(new IdemixCredential(c.toProto()).verify(sk, key.getIpk()));

        // Create a new unlinkable pseudonym
        IdemixPseudonym pseudonym = new IdemixPseudonym(sk, key.getIpk());

        // Test signing no disclosure
        boolean[] disclosure = {false, false, false, false, false};
        byte[] msg = {1, 2, 3, 4, 5};
        IdemixSignature signature = new IdemixSignature(c, sk, pseudonym, key.getIpk(), disclosure, msg, 0, cri);
        assertNotNull(signature);
        // check that the signature is valid
        assertTrue(signature.verify(disclosure, key.getIpk(), msg, attrs, 0, revocationKeyPair.getPublic(), epoch));

        // Test serialization of IdemixSignature
        assertTrue(new IdemixSignature(signature.toProto()).verify(disclosure, key.getIpk(), msg, attrs, 0, revocationKeyPair.getPublic(), epoch));

        // Test signing selective disclosure
        boolean[] disclosure2 = {false, true, true, true, false};
        signature = new IdemixSignature(c, sk, pseudonym, key.getIpk(), disclosure2, msg, 0, cri);
        assertNotNull(signature);

        // check that the signature is valid
        assertTrue(signature.verify(disclosure2, key.getIpk(), msg, attrs, 0, revocationKeyPair.getPublic(), epoch));

        // Test signature verification with different disclosure
        assertFalse(signature.verify(disclosure, key.getIpk(), msg, attrs, 0, revocationKeyPair.getPublic(), epoch));

        // test signature verification with different issuer public key
        assertFalse(signature.verify(disclosure2, new IdemixIssuerKey(attributeNames).getIpk(), msg, attrs, 0, revocationKeyPair.getPublic(), epoch));

        // test signature verification with different message
        byte[] msg2 = {1, 1, 1};
        assertFalse(signature.verify(disclosure2, key.getIpk(), msg2, attrs, 0, revocationKeyPair.getPublic(), epoch));

        // Sign a message with respect to a pseudonym
        IdemixPseudonymSignature nymsig = new IdemixPseudonymSignature(sk, pseudonym, key.getIpk(), msg);
        // check that the pseudonym signature is valid
        assertTrue(nymsig.verify(pseudonym.getNym(), key.getIpk(), msg));

        // Test serialization of IdemixPseudonymSignature
        assertTrue(new IdemixPseudonymSignature(nymsig.toProto()).verify(pseudonym.getNym(), key.getIpk(), msg));
    }
}
