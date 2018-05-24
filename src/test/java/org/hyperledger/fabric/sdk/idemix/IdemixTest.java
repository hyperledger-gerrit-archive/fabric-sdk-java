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
import java.util.ArrayList;
import java.util.List;
import org.apache.milagro.amcl.FP256BN.BIG;
import org.apache.milagro.amcl.FP256BN.ECP;
import org.hyperledger.fabric.protos.idemix.Idemix;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IdemixTest {

    @Test
    public void testIdemix() throws InterruptedException {
        // Select attribute names and generate a Idemix Setup
        String[] attributeNames = {"Attr1", "Attr2", "Attr3", "Attr4", "Attr5"};

        IdemixSetup setup = new IdemixSetup(attributeNames, 1);
        setup.runThreads();

        // IdemixSetup setup2 = new IdemixSetup(attributeNames, 2);
        // setup2.runThreads();
    }

    private class IdemixSetup {
        private String[] attributeNames;
        private IdemixIssuerKey key;
        private BIG sk;
        private BIG issuerNonce;
        private IdemixCredRequest idemixCredRequest;
        private IdemixCredential idemixCredential;
        private BIG[] attrs;
        private WeakBB.KeyPair wbbKeyPair;
        private KeyPair revocationKeyPair;
        private int threads;

        private IdemixSetup(String[] attributeNames, int threads) {
            if (threads > 0) {
                this.threads = threads;
            } else {
                this.threads = 1;
            }
            // Choose attribute names and create an issuer key pair
            // this.attributeNames = new String[]{"Attribute1", "Attribute2"};
            this.attributeNames = attributeNames;
            this.key = new IdemixIssuerKey(this.attributeNames);

            // Choose a user secret key and request a credential
            this.sk = new BIG(IdemixUtils.randModOrder());
            this.issuerNonce = new BIG(IdemixUtils.randModOrder());
            this.idemixCredRequest = new IdemixCredRequest(this.sk, this.issuerNonce, this.key.getIpk()); //csr

            // Issue a credential
            this.attrs = new BIG[this.attributeNames.length];
            for (int i = 0; i < this.attributeNames.length; i++) {
                this.attrs[i] = new BIG(i);
            }
            this.idemixCredential = new IdemixCredential(this.key, this.idemixCredRequest, this.attrs); //certificate

            this.wbbKeyPair = WeakBB.weakBBKeyGen();

            // Generate a revocation key pair
            this.revocationKeyPair = RevocationAuthority.generateLongTermRevocationKey();

            // Check all the generated data
            checkSetup();
        }

        private void checkSetup() {
            // check that the issuer public key is valid
            assertTrue(this.key.getIpk().check());
            // Test serialization of issuer public key
            assertTrue(new IdemixIssuerPublicKey(this.key.getIpk().toProto()).check());
            // Test credential request
            assertTrue(this.idemixCredRequest.check(this.key.getIpk()));
            // Test serialization of cred request
            assertTrue(new IdemixCredRequest(this.idemixCredRequest.toProto()).check(key.getIpk()));
            // Test revocation key pair
            assertNotNull(this.revocationKeyPair);
        }

        private void runThreads() throws InterruptedException {
            List<VerifyIdemix> runners = new ArrayList<>();
            for (int i = this.threads; i > 0; --i) {
                runners.add(new VerifyIdemix(this, this.threads > 1 ? "T" + i : "Single thread"));
            }

            for (VerifyIdemix t : runners) {
                t.start();
            }

            for (VerifyIdemix t : runners) {
                t.thr.join();
            }
        }
    }

    private class VerifyIdemix implements Runnable {
        private IdemixIssuerKey key;
        private BIG sk;
        private BIG[] attrs;
        private IdemixCredential credential;
        private WeakBB.KeyPair wbbKeyPair;
        private KeyPair revocationKeyPair;
        private Thread thr;
        private String name;

        private VerifyIdemix(IdemixSetup setup, String name) {
            this.key = setup.key;
            this.sk = setup.sk;
            this.attrs = setup.attrs;
            this.credential = setup.idemixCredential;
            this.wbbKeyPair = setup.wbbKeyPair;
            this.revocationKeyPair = setup.revocationKeyPair;
            this.name = name;
        }

        public void start() {
            if (thr == null) {
                thr = new Thread(this, name);
                thr.setDaemon(true);
                thr.start();
            }
        }

        private void invoke() throws CryptoException {
            // WeakBB test
            // Random message to sign
            BIG wbbMessage = IdemixUtils.randModOrder();
            // Sign the message with keypair secret key
            ECP wbbSignature = WeakBB.weakBBSign(this.wbbKeyPair.getSk(), wbbMessage);
            // Check the signature with valid PK and valid message
            assertTrue(WeakBB.weakBBVerify(this.wbbKeyPair.getPk(), wbbSignature, wbbMessage));
            // Try to check a random message
            assertFalse(WeakBB.weakBBVerify(this.wbbKeyPair.getPk(), wbbSignature, IdemixUtils.randModOrder()));

            // user completes the credential and checks validity
            assertTrue(this.credential.verify(this.sk, this.key.getIpk()));

            // Test serialization of IdemixCredential
            assertTrue(new IdemixCredential(this.credential.toProto()).verify(this.sk, this.key.getIpk()));

            // Create CRI that contains no revocation mechanism
            int epoch = 0;
            BIG[] rhIndex = {new BIG(0)};
            Idemix.CredentialRevocationInformation cri = RevocationAuthority.createCRI(this.revocationKeyPair.getPrivate(), rhIndex, epoch, RevocationAlgorithm.ALG_NO_REVOCATION);

            // Create a new unlinkable pseudonym
            IdemixPseudonym pseudonym = new IdemixPseudonym(this.sk, this.key.getIpk()); //tcert

            // Test signing no disclosure
            boolean[] disclosure = {false, false, false, false, false};
            byte[] msg = {1, 2, 3, 4, 5};
            IdemixSignature signature = new IdemixSignature(this.credential, this.sk, pseudonym, this.key.getIpk(), disclosure, msg, 0, cri);
            assertNotNull(signature);

            // check that the signature is valid
            assertTrue(signature.verify(disclosure, this.key.getIpk(), msg, this.attrs, 0, this.revocationKeyPair.getPublic(), epoch));

            // Test serialization of IdemixSignature
            assertTrue(new IdemixSignature(signature.toProto()).verify(disclosure, key.getIpk(), msg, attrs, 0, revocationKeyPair.getPublic(), epoch));

            // Test signing selective disclosure
            boolean[] disclosure2 = {false, true, true, true, false};
            signature = new IdemixSignature(this.credential, this.sk, pseudonym, this.key.getIpk(), disclosure2, msg, 0, cri);
            assertNotNull(signature);

            // check that the signature is valid
            assertTrue(signature.verify(disclosure2, this.key.getIpk(), msg, this.attrs, 0, this.revocationKeyPair.getPublic(), epoch));

            // Test signature verification with different disclosure
            assertFalse(signature.verify(disclosure, this.key.getIpk(), msg, this.attrs, 0, this.revocationKeyPair.getPublic(), epoch));

            // test signature verification with different issuer public key
            assertFalse(signature.verify(disclosure2, new IdemixIssuerKey(new String[]{"Attr1, Attr2, Attr3, Attr4, Attr5"}).getIpk(), msg, this.attrs, 0, this.revocationKeyPair.getPublic(), epoch));

            // test signature verification with different message
            byte[] msg2 = {1, 1, 1};
            assertFalse(signature.verify(disclosure2, this.key.getIpk(), msg2, this.attrs, 0, this.revocationKeyPair.getPublic(), epoch));

            // Sign a message with respect to a pseudonym
            IdemixPseudonymSignature nymsig = new IdemixPseudonymSignature(this.sk, pseudonym, this.key.getIpk(), msg);
            // check that the pseudonym signature is valid
            assertTrue(nymsig.verify(pseudonym.getNym(), this.key.getIpk(), msg));

            // Test serialization of IdemixPseudonymSignature
            assertTrue(new IdemixPseudonymSignature(nymsig.toProto()).verify(pseudonym.getNym(), this.key.getIpk(), msg));
        }

        @Override
        public void run() {
            // Ten times per thread
            for (int i = 10; i > 0; --i) {
                try {
                    invoke();
                } catch (CryptoException e) {
                    e.printStackTrace();
                }
            }
            final Thread thread = Thread.currentThread();

            thread.interrupt();
        }
    }
}