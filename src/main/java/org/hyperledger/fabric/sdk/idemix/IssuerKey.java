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

import com.google.protobuf.ByteString;
import com.ibm.zurich.amcl.BN254.BIG;
import com.ibm.zurich.amcl.RAND;
import org.hyperledger.fabric.protos.idemix.Idemix;
import org.hyperledger.fabric.sdk.exception.CryptoException;

/**
 * IssuerKey represents an idemix issuer key pair
 */
public class IssuerKey {

    public BIG Isk;
    public IssuerPublicKey Ipk;

    /**
     * Constructor
     *
     * @param attributeNames the names of attributes as String array (must not contain duplicates)
     * @param rng            a random number generator
     */
    public IssuerKey(String[] attributeNames, RAND rng) throws CryptoException {
        // generate the secret key
        this.Isk = Utils.randModOrder(rng);

        // construct the corresponding public key
        this.Ipk = new IssuerPublicKey(attributeNames, rng, this.Isk);
    }


    /**
     * Construct IssuerKey from a serialized issuer key
     */
    public IssuerKey(Idemix.IssuerKey proto) {
        this.Ipk = new IssuerPublicKey(proto.getIPk());
        this.Isk = BIG.fromBytes(proto.getISk().toByteArray());
    }

    /**
     * Convert this IssuerKey to a proto
     */
    public Idemix.IssuerKey toProto() {
        Idemix.IssuerKey proto = Idemix.IssuerKey.newBuilder()
                .setIPk(this.Ipk.toProto())
                .setISk(ByteString.copyFrom(Utils.bigToBytes(this.Isk)))
                .build();

        return proto;
    }
}
