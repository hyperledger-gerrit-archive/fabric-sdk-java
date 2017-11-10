/*
 *
 *  Copyright 2017 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
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

import com.ibm.zurich.amcl.BN254.BIG;
import com.ibm.zurich.amcl.BN254.ECP;
import com.ibm.zurich.amcl.RAND;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class IdemixTest {

    @Test
    public void testIdemix() throws CryptoException {
        // Get RNG
        RAND rand = Utils.getRand();

        // Choose attribute names and create an issuer key pair
        String[] attributeNames = {"Attribute1", "Attribute2"};
        IssuerKey key = new IssuerKey(attributeNames, rand);
        // Check that the issuer public key is valid
        assertTrue(key.Ipk.Check());

        // Test serialization of issuer key
        assertTrue(new IssuerKey(key.toProto()).Ipk.Check());

        // Choose a user secret key and request a credential
        BIG sk = new BIG(Utils.RandModOrder(rand));
        BIG randCred = new BIG(Utils.RandModOrder(rand));
        BIG issuerNonce = new BIG(Utils.RandModOrder(rand));
        CredRequest m = new CredRequest(sk, randCred, issuerNonce, key.Ipk, rand);
        assertTrue(m.Check(key.Ipk));

        // Test serialization of cred request
        assertTrue(new CredRequest(m.toProto()).Check(key.Ipk));

        // Issue a credential
        BIG[] attrs = new BIG[attributeNames.length];
        for (int i = 0; i < attributeNames.length; i++) {
            attrs[i] = new BIG(i);
        }
        Credential c = new Credential(key, m, attrs, rand);

        // user completes the credential and checks validity
        c.Complete(randCred);
        assertTrue(c.Ver(sk, key.Ipk));

        // Test serialization of Credential
        assertTrue(new Credential(c.toProto()).Ver(sk, key.Ipk));

        // Create a new unlinkable pseudonym
        Pseudonym p = new Pseudonym(sk, key.Ipk, rand);
        ECP Nym = p.getNym();
        BIG RandNym = p.getRandNym();

        // Generate new signature, disclosing no attributes
        byte[] disclosure = {0, 0};
        byte[] msg = {1, 2, 3, 4};
        Signature sig = new Signature(c, sk, Nym, RandNym, key.Ipk, disclosure, msg, rand);
        // Check that the signature is valid
        assertTrue(sig.Ver(disclosure, key.Ipk, msg, attrs));

        // Test serialization of Signature
        assertTrue(new Signature(sig.toProto()).Ver(disclosure, key.Ipk, msg, attrs));

        // Generate new signature, disclosing both attributes
        disclosure = new byte[]{1, 1};
        sig = new Signature(c, sk, Nym, RandNym, key.Ipk, disclosure, msg, rand);
        // Check that the signature is valid
        assertTrue(sig.Ver(disclosure, key.Ipk, msg, attrs));

        // Sign a message with respect to a pseudonym
        NymSignature nymsig = new NymSignature(sk, Nym, RandNym, key.Ipk, msg, rand);
        // Check that the pseudonym signature is valid
        assertTrue(nymsig.Ver(Nym, key.Ipk, msg));

        // Test serialization of NymSignature
        assertTrue(new NymSignature(nymsig.toProto()).Ver(Nym, key.Ipk, msg));
    }
}
