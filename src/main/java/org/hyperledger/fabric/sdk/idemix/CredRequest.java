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

import java.util.Arrays;

import com.google.protobuf.ByteString;
import org.apache.milagro.amcl.FP256BN.BIG;
import org.apache.milagro.amcl.FP256BN.ECP;
import org.apache.milagro.amcl.RAND;
import org.hyperledger.fabric.protos.idemix.Idemix;

/**
 * CredRequest represents the first message of the idemix issuance protocol,
 * in which the user requests a credential from the issuer.
 */
public class CredRequest {
    private ECP nym;
    private BIG issuerNonce;
    private BIG proofC;
    private BIG proofS1;
    private BIG proofS2;

    public static final String CREDREQUEST_LABEL = "credRequest";


    /**
     * Constructor
     *
     * @param sk          the secret key of the user
     * @param credS1      a credential
     * @param issuerNonce a nonce
     * @param ipk         the issuer public key
     * @param rng         a random number generator
     */
    public CredRequest(BIG sk, BIG credS1, BIG issuerNonce, IssuerPublicKey ipk, RAND rng) {

        this.nym = ipk.Hsk.mul2(sk, ipk.HRand, credS1);
        this.issuerNonce = new BIG(issuerNonce);

        // Create Zero Knowledge Proof
        BIG rsk = Utils.randModOrder(rng);
        BIG rRand = Utils.randModOrder(rng);
        ECP t = ipk.Hsk.mul2(rsk, ipk.HRand, rRand);

        // Make proofData: total 3 elements of G1, each 2*fieldBytes+1 (ECP),
        // plus length of String array,
        // plus one BIG
        byte[] proofData = new byte[CREDREQUEST_LABEL.getBytes().length + 3 * (2 * Utils.fieldBytes + 1) + 2 * Utils.fieldBytes];

        int index = 0;
        index = Utils.append(proofData, index, CREDREQUEST_LABEL.getBytes());
        index = Utils.append(proofData, index, Utils.ecpToBytes(t));
        index = Utils.append(proofData, index, Utils.ecpToBytes(ipk.Hsk));
        index = Utils.append(proofData, index, Utils.ecpToBytes(this.nym));
        index = Utils.append(proofData, index, Utils.bigToBytes(issuerNonce));
        index = Utils.append(proofData, index, ipk.Hash);

        this.proofC = Utils.hashModOrder(proofData);

        // Compute proofS1 = ...
        this.proofS1 = BIG.modmul(this.proofC, sk, Utils.groupOrder).plus(rsk);
        this.proofS1.mod(Utils.groupOrder);

        // Compute proofS2 = ...
        this.proofS2 = BIG.modmul(this.proofC, credS1, Utils.groupOrder).plus(rRand);
        this.proofS2.mod(Utils.groupOrder);
    }

    /**
     * Construct a CredRequest from a serialized credrequest
     */
    public CredRequest(Idemix.CredRequest proto) {
        this.nym = Utils.transformFromProto(proto.getNym());
        this.proofC = BIG.fromBytes(proto.getProofC().toByteArray());
        this.proofS1 = BIG.fromBytes(proto.getProofS1().toByteArray());
        this.proofS2 = BIG.fromBytes(proto.getProofS2().toByteArray());
        this.issuerNonce = BIG.fromBytes(proto.getIssuerNonce().toByteArray());
    }

    public ECP getNym() {
        return nym;
    }

    /**
     * @return a proto version of this CredRequest
     */
    public Idemix.CredRequest toProto() {
        Idemix.CredRequest proto = Idemix.CredRequest.newBuilder()
                .setNym(Utils.transformToProto(this.nym))
                .setProofC(ByteString.copyFrom(Utils.bigToBytes(this.proofC)))
                .setProofS1(ByteString.copyFrom(Utils.bigToBytes(this.proofS1)))
                .setProofS2(ByteString.copyFrom(Utils.bigToBytes(this.proofS2)))
                .setIssuerNonce(ByteString.copyFrom(Utils.bigToBytes(this.issuerNonce)))
                .build();

        return proto;
    }


    /**
     * check cryptographically verifies the CredRequest
     *
     * @param ipk the issuer public key
     * @return true iff valid
     */
    public boolean check(IssuerPublicKey ipk) {

        if (this.nym == null ||
                this.issuerNonce == null ||
                this.proofC == null ||
                this.proofS1 == null ||
                this.proofS2 == null) {
            return false;
        }

        ECP t = ipk.Hsk.mul2(this.proofS1, ipk.HRand, this.proofS2);
        t.sub(this.nym.mul(this.proofC));

        byte[] proofData = new byte[CREDREQUEST_LABEL.getBytes().length + 3 * (2 * Utils.fieldBytes + 1) + 2 * Utils.fieldBytes];

        int index = 0;
        index = Utils.append(proofData, index, CREDREQUEST_LABEL.getBytes());
        index = Utils.append(proofData, index, Utils.ecpToBytes(t));
        index = Utils.append(proofData, index, Utils.ecpToBytes(ipk.Hsk));
        index = Utils.append(proofData, index, Utils.ecpToBytes(this.nym));
        index = Utils.append(proofData, index, Utils.bigToBytes(issuerNonce));
        index = Utils.append(proofData, index, ipk.Hash);

        // Hash proofData to hproofdata
        byte[] hproofdata = Utils.bigToBytes(Utils.hashModOrder(proofData));

        if (!Arrays.equals(Utils.bigToBytes(this.proofC), hproofdata)) {
            return false;
        }

        return true;
    }
}
