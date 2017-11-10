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
import com.ibm.zurich.amcl.BN254.BIG;
import com.ibm.zurich.amcl.BN254.ECP;
import com.ibm.zurich.amcl.RAND;
import org.hyperledger.fabric.protos.idemix.Idemix;

/**
 * NymSignature is a signature on a message which can be verified with respect to a pseudonym
 */
public class NymSignature {
    private BIG proofC;
    private BIG proofSSk;
    private BIG nonce;
    private BIG proofSRNym;


    /**
     * Creates a new NymSignature
     */
    public NymSignature(BIG sk, ECP nym, BIG rNym, IssuerPublicKey ipk, byte[] msg, RAND rng) {
        this.nonce = Utils.randModOrder(rng);

        //Construct Zero Knowledge Proof
        BIG rsk = Utils.randModOrder(rng);
        BIG rRNym = Utils.randModOrder(rng);
        ECP t = ipk.Hsk.mul2(rsk, ipk.HRand, rRNym);

        // create array for proof data that will contain the sign label, 2 ECPs (each of length 2* fieldBytes + 1), the ipk hash and the message
        byte[] proofData = new byte[Signature.SIGN_LABEL.getBytes().length + 2 * (2 * Utils.fieldBytes + 1) + ipk.Hash.length + msg.length];
        int index = 0;
        index = Utils.append(proofData, index, Signature.SIGN_LABEL.getBytes());
        index = Utils.append(proofData, index, Utils.ecpToBytes(t));
        index = Utils.append(proofData, index, Utils.ecpToBytes(nym));
        index = Utils.append(proofData, index, ipk.Hash);
        index = Utils.append(proofData, index, msg);

        BIG cvalue = Utils.hashModOrder(proofData);

        byte[] finalProofData = new byte[2 * Utils.fieldBytes];
        index = 0;
        index = Utils.append(finalProofData, index, Utils.bigToBytes(cvalue));
        index = Utils.append(finalProofData, index, Utils.bigToBytes(this.nonce));
        this.proofC = Utils.hashModOrder(finalProofData);

        this.proofSSk = new BIG(rsk);
        this.proofSSk.add(BIG.modmul(this.proofC, sk, Utils.groupOrder));
        this.proofSSk.mod(Utils.groupOrder);

        this.proofSRNym = new BIG(rRNym);
        this.proofSRNym.add(BIG.modmul(this.proofC, rNym, Utils.groupOrder));
        this.proofSRNym.mod(Utils.groupOrder);
    }

    /**
     * Construct a new signature from a serialized Signature
     */
    public NymSignature(Idemix.NymSignature proto) {
        this.proofC = BIG.fromBytes(proto.getProofC().toByteArray());
        this.proofSSk = BIG.fromBytes(proto.getProofSSk().toByteArray());
        this.proofSRNym = BIG.fromBytes(proto.getProofSRNym().toByteArray());
        this.nonce = BIG.fromBytes(proto.getNonce().toByteArray());
    }

    /**
     * Verify this NymSignature
     *
     * @param ipk the issuer public key
     * @param msg the message that should be signed in this signature
     * @return true iff valid
     */
    public boolean ver(ECP nym, IssuerPublicKey ipk, byte[] msg) {
        ECP t = ipk.Hsk.mul2(this.proofSSk, ipk.HRand, this.proofSRNym);
        t.sub(nym.mul(this.proofC));

        // create array for proof data that will contain the sign label, 2 ECPs (each of length 2* fieldBytes + 1), the ipk hash and the message
        byte[] proofData = new byte[Signature.SIGN_LABEL.getBytes().length + 2 * (2 * Utils.fieldBytes + 1) + ipk.Hash.length + msg.length];
        int index = 0;
        index = Utils.append(proofData, index, Signature.SIGN_LABEL.getBytes());
        index = Utils.append(proofData, index, Utils.ecpToBytes(t));
        index = Utils.append(proofData, index, Utils.ecpToBytes(nym));
        index = Utils.append(proofData, index, ipk.Hash);
        index = Utils.append(proofData, index, msg);

        BIG cvalue = Utils.hashModOrder(proofData);

        byte[] finalProofData = new byte[2 * Utils.fieldBytes];
        index = 0;
        index = Utils.append(finalProofData, index, Utils.bigToBytes(cvalue));
        index = Utils.append(finalProofData, index, Utils.bigToBytes(this.nonce));

        byte[] hashedProofData = Utils.bigToBytes(Utils.hashModOrder(finalProofData));
        if (!Arrays.equals(Utils.bigToBytes(this.proofC), hashedProofData)) {
            return false;
        }
        return true;
    }

    /**
     * Convert this NymSignature to a proto
     */
    public Idemix.NymSignature toProto() {
        Idemix.NymSignature.Builder builder = Idemix.NymSignature.newBuilder()
                .setProofC(ByteString.copyFrom(Utils.bigToBytes(this.proofC)))
                .setProofSSk(ByteString.copyFrom(Utils.bigToBytes(this.proofSSk)))
                .setProofSRNym(ByteString.copyFrom(Utils.bigToBytes(this.proofSRNym)))
                .setNonce(ByteString.copyFrom(Utils.bigToBytes(this.nonce)));

        return builder.build();
    }
}
