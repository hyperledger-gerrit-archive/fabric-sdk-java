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
import org.apache.milagro.amcl.FP256BN.ECP;
import org.apache.milagro.amcl.RAND;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.junit.Test;

import static org.hyperledger.fabric.sdk.idemix.Utils.getRand;
import static org.junit.Assert.assertTrue;

public class IdemixTest {

    @Test
    public void testIdemix() throws CryptoException {
        // Get RNG
        RAND rand = getRand();

        // Choose attribute names and create an issuer key pair
        String[] attributeNames = {"Attribute1", "Attribute2"};
        IssuerKey key = new IssuerKey(attributeNames, rand);
        // check that the issuer public key is valid
        assertTrue(key.Ipk.check());

        // Test serialization of issuer key
        assertTrue(new IssuerKey(key.toProto()).Ipk.check());

        // Choose a user secret key and request a credential
        BIG sk = new BIG(Utils.randModOrder(rand));
        BIG randCred = new BIG(Utils.randModOrder(rand));
        BIG issuerNonce = new BIG(Utils.randModOrder(rand));
        CredRequest m = new CredRequest(sk, randCred, issuerNonce, key.Ipk, rand);
        assertTrue(m.check(key.Ipk));

        // Test serialization of cred request
        assertTrue(new CredRequest(m.toProto()).check(key.Ipk));

        // Issue a credential
        BIG[] attrs = new BIG[attributeNames.length];
        for (int i = 0; i < attributeNames.length; i++) {
            attrs[i] = new BIG(i);
        }
        Credential c = new Credential(key, m, attrs, rand);

        // user completes the credential and checks validity
        c.complete(randCred);
        assertTrue(c.ver(sk, key.Ipk));

        // Test serialization of Credential
        assertTrue(new Credential(c.toProto()).ver(sk, key.Ipk));

        // Create a new unlinkable pseudonym
        Pseudonym p = new Pseudonym(sk, key.Ipk, rand);
        ECP nym = p.getNym();
        BIG randNym = p.getRandNym();

        // Generate new signature, disclosing no attributes
        byte[] disclosure = {0, 0};
        byte[] msg = {1, 2, 3, 4};
        Signature sig = new Signature(c, sk, nym, randNym, key.Ipk, disclosure, msg, rand);
        // check that the signature is valid
        assertTrue(sig.ver(disclosure, key.Ipk, msg, attrs));

        // TODO: reenable this once the idemix library is fixed
        // Test serialization of Signature
//        assertTrue(new Signature(sig.toProto()).ver(disclosure, key.Ipk, msg, attrs));

        // Generate new signature, disclosing both attributes
        disclosure = new byte[]{1, 1};
        sig = new Signature(c, sk, nym, randNym, key.Ipk, disclosure, msg, rand);
        // check that the signature is valid
        assertTrue(sig.ver(disclosure, key.Ipk, msg, attrs));

        // Sign a message with respect to a pseudonym
        NymSignature nymsig = new NymSignature(sk, nym, randNym, key.Ipk, msg, rand);
        // check that the pseudonym signature is valid
        assertTrue(nymsig.ver(nym, key.Ipk, msg));

        // Test serialization of NymSignature
        assertTrue(new NymSignature(nymsig.toProto()).ver(nym, key.Ipk, msg));
    }
}
