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

import org.apache.milagro.amcl.FP256BN.BIG;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class IdemixTest {

        @Test
        public void testIdemix() throws CryptoException {
                // Choose attribute names and create an issuer key pair
                String[] attributeNames = {"Attribute1", "Attribute2"};
                IdemixIssuerKey key = new IdemixIssuerKey(attributeNames);
                // check that the issuer public key is valid
                assertTrue(key.getIpk().check());

                // Test serialization of issuer public key
                assertTrue(new IdemixIssuerPublicKey(key.getIpk().toProto()).check());

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

                // user completes the credential and checks validity
                //c.complete(randCred);
                assertTrue(c.ver(sk, key.getIpk()));

                // Test serialization of IdemixCredential
                assertTrue(new IdemixCredential(c.toProto()).ver(sk, key.getIpk()));

                // Create a new unlinkable pseudonym
                IdemixPseudonym pseudonym = new IdemixPseudonym(sk, key.getIpk());

                // Generate new signature, disclosing no attributes
                boolean[] disclosure = {false, false};
                byte[] msg = {1, 2, 3, 4};
                IdemixSignature sig = new IdemixSignature(c, sk, pseudonym, key.getIpk(), disclosure, msg);
                // check that the signature is valid
                assertTrue(sig.ver(disclosure, key.getIpk(), msg, attrs));

                // Test serialization of IdemixSignature
                assertTrue(new IdemixSignature(sig.toProto()).ver(disclosure, key.getIpk(), msg, attrs));

                // Generate new signature, disclosing both attributes
                disclosure = new boolean[]{true, true};
                sig = new IdemixSignature(c, sk, pseudonym, key.getIpk(), disclosure, msg);
                // check that the signature is valid
                assertTrue(sig.ver(disclosure, key.getIpk(), msg, attrs));

                // Sign a message with respect to a pseudonym
                IdemixNymSignature nymsig = new IdemixNymSignature(sk, pseudonym, key.getIpk(), msg);
                // check that the pseudonym signature is valid
                assertTrue(nymsig.ver(pseudonym.getNym(), key.getIpk(), msg));

                // Test serialization of IdemixNymSignature
                assertTrue(new IdemixNymSignature(nymsig.toProto()).ver(pseudonym.getNym(), key.getIpk(), msg));
        }
}

