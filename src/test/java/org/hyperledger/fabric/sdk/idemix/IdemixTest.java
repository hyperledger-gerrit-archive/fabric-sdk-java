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

import java.util.ArrayList;
import java.util.List;
import org.apache.milagro.amcl.FP256BN.BIG;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class IdemixTest {

    @Test
    public void testIdemix() throws InterruptedException {
        // Select attribute names and generate a Idemix Setup
        String[] attributeNames = {"Attribute1", "Attribute2"};

        IdemixSetup setup = new IdemixSetup(attributeNames, 1);
        setup.runThreads();

        IdemixSetup setup2 = new IdemixSetup(attributeNames, 2);
        setup2.runThreads();
    }

    private class IdemixSetup {
        private String[] attributeNames;
        private IdemixIssuerKey key;
        private BIG sk;
        private BIG issuerNonce;
        private IdemixCredRequest idemixCredRequest;
        private IdemixCredential idemixCredential;
        private BIG[] attrs;
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

        private Thread thr;
        private String name;

        private VerifyIdemix(IdemixSetup setup, String name) {
            this.key = setup.key;
            this.sk = setup.sk;
            this.attrs = setup.attrs;
            this.credential = setup.idemixCredential;
            this.name = name;
        }

        public void start() {
            System.out.println("Starting " +  name);
            if (thr == null) {
                thr = new Thread(this, name);
                thr.setDaemon(true);
                thr.start();
            }
        }

        private void invoke() {
            // user completes the credential and checks validity
            assertTrue(this.credential.verify(this.sk, this.key.getIpk()));

            // Test serialization of IdemixCredential
            assertTrue(new IdemixCredential(this.credential.toProto()).verify(this.sk, this.key.getIpk()));

            // Create a new unlinkable pseudonym
            IdemixPseudonym pseudonym = new IdemixPseudonym(this.sk, this.key.getIpk()); //tcert

            // Generate new signature, disclosing no attributes
            boolean[] disclosure = {false, false};
            byte[] msg = {1, 2, 3, 4};
            IdemixSignature sig = new IdemixSignature(this.credential, this.sk, pseudonym, this.key.getIpk(), disclosure, msg);
            // check that the signature is valid
            assertTrue(sig.verify(disclosure, this.key.getIpk(), msg, this.attrs));

            // Test serialization of IdemixSignature
            assertTrue(new IdemixSignature(sig.toProto()).verify(disclosure, this.key.getIpk(), msg, this.attrs));

            // Generate new signature, disclosing both attributes
            disclosure = new boolean[] {true, true};
            sig = new IdemixSignature(this.credential, this.sk, pseudonym, this.key.getIpk(), disclosure, msg);
            // check that the signature is valid
            assertTrue(sig.verify(disclosure, this.key.getIpk(), msg, this.attrs));

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
                invoke();
            }
            final Thread thread = Thread.currentThread();
            System.out.println(thread.getName() + " done.");

            thread.interrupt();
        }
    }
}
