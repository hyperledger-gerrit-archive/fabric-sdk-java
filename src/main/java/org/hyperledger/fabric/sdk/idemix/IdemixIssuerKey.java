/*
 *
 *  Copyright 2017, 2018 IBM Corp. All Rights Reserved.
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

import com.google.protobuf.ByteString;
import org.apache.milagro.amcl.FP256BN.BIG;
import org.apache.milagro.amcl.RAND;
import org.hyperledger.fabric.protos.idemix.Idemix;
import org.hyperledger.fabric.sdk.exception.CryptoException;

/**
 * IdemixIssuerKey represents an idemix issuer key pair
 */
public class IdemixIssuerKey {

    private final BIG Isk;
    private final IdemixIssuerPublicKey Ipk;

    /**
     * Constructor
     *
     * @param attributeNames the names of attributes as String array (must not contain duplicates)
     * @param rng            a random number generator
     */
    public IdemixIssuerKey(String[] attributeNames) {
        // generate the secret key
        Isk = IdemixUtils.randModOrder();

        // construct the corresponding public key
        Ipk = new IdemixIssuerPublicKey(attributeNames, Isk);
    }

    public IdemixIssuerPublicKey getIpk() {
        return Ipk;
    }

    public BIG getIsk() {
        return Isk;
    }
}
