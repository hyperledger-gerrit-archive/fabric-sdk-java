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

import java.util.Arrays;

import com.google.protobuf.ByteString;
import org.apache.milagro.amcl.FP256BN.BIG;
import org.apache.milagro.amcl.FP256BN.ECP;
import org.apache.milagro.amcl.RAND;
import org.hyperledger.fabric.protos.idemix.Idemix;

/**
 * IdemixNymSignature is a signature on a message which can be verified with respect to a pseudonym
 */
public class IdemixNymSignature {
    private final BIG proofC;
    private final BIG proofSSk;
    private final BIG nonce;
    private final BIG proofSRNym;

    private static final String NYM_SIGN_LABEL = "sign";

    /**
     * Creates a new IdemixNymSignature
     */
    public IdemixNymSignature(BIG sk, ECP nym, BIG rNym, IssuerPublicKey ipk, byte[] msg, RAND rng) {
        this.nonce = IdemixUtils.randModOrder(rng);

        //Construct Zero Knowledge Proof
        BIG rsk = IdemixUtils.randModOrder(rng);
        BIG rRNym = IdemixUtils.randModOrder(rng);
        ECP t = ipk.getHsk().mul2(rsk, ipk.getHRand(), rRNym);

        // create array for proof data that will contain the sign label, 2 ECPs (each of length 2* FIELD_BYTES + 1), the ipk hash and the message
        byte[] proofData = new byte[NYM_SIGN_LABEL.getBytes().length + 2 * (2 * IdemixUtils.FIELD_BYTES + 1) + ipk.getHash().length + msg.length];
        int index = 0;
        index = IdemixUtils.append(proofData, index, NYM_SIGN_LABEL.getBytes());
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(t));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(nym));
        index = IdemixUtils.append(proofData, index, ipk.getHash());
        IdemixUtils.append(proofData, index, msg);

        BIG cvalue = IdemixUtils.hashModOrder(proofData);

        byte[] finalProofData = new byte[2 * IdemixUtils.FIELD_BYTES];
        index = 0;
        index = IdemixUtils.append(finalProofData, index, IdemixUtils.bigToBytes(cvalue));
        IdemixUtils.append(finalProofData, index, IdemixUtils.bigToBytes(this.nonce));
        this.proofC = IdemixUtils.hashModOrder(finalProofData);

        this.proofSSk = new BIG(rsk);
        this.proofSSk.add(BIG.modmul(this.proofC, sk, IdemixUtils.GROUP_ORDER));
        this.proofSSk.mod(IdemixUtils.GROUP_ORDER);

        this.proofSRNym = new BIG(rRNym);
        this.proofSRNym.add(BIG.modmul(this.proofC, rNym, IdemixUtils.GROUP_ORDER));
        this.proofSRNym.mod(IdemixUtils.GROUP_ORDER);
    }

    /**
     * Construct a new signature from a serialized IdemixNymSignature
     */
    public IdemixNymSignature(Idemix.NymSignature proto) {
        this.proofC = BIG.fromBytes(proto.getProofC().toByteArray());
        this.proofSSk = BIG.fromBytes(proto.getProofSSk().toByteArray());
        this.proofSRNym = BIG.fromBytes(proto.getProofSRNym().toByteArray());
        this.nonce = BIG.fromBytes(proto.getNonce().toByteArray());
    }

    /**
     * Verify this IdemixNymSignature
     *
     * @param ipk the issuer public key
     * @param msg the message that should be signed in this signature
     * @return true iff valid
     */
    public boolean ver(ECP nym, IssuerPublicKey ipk, byte[] msg) {
        ECP t = ipk.getHsk().mul2(this.proofSSk, ipk.getHRand(), this.proofSRNym);
        t.sub(nym.mul(this.proofC));

        // create array for proof data that will contain the sign label, 2 ECPs (each of length 2* FIELD_BYTES + 1), the ipk hash and the message
        byte[] proofData = new byte[NYM_SIGN_LABEL.getBytes().length + 2 * (2 * IdemixUtils.FIELD_BYTES + 1) + ipk.getHash().length + msg.length];
        int index = 0;
        index = IdemixUtils.append(proofData, index, NYM_SIGN_LABEL.getBytes());
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(t));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(nym));
        index = IdemixUtils.append(proofData, index, ipk.getHash());
        IdemixUtils.append(proofData, index, msg);

        BIG cvalue = IdemixUtils.hashModOrder(proofData);

        byte[] finalProofData = new byte[2 * IdemixUtils.FIELD_BYTES];
        index = 0;
        index = IdemixUtils.append(finalProofData, index, IdemixUtils.bigToBytes(cvalue));
        IdemixUtils.append(finalProofData, index, IdemixUtils.bigToBytes(this.nonce));

        byte[] hashedProofData = IdemixUtils.bigToBytes(IdemixUtils.hashModOrder(finalProofData));
        if (!Arrays.equals(IdemixUtils.bigToBytes(this.proofC), hashedProofData)) {
            return false;
        }
        return true;
    }

    /**
     * Convert this IdemixNymSignature to a proto
     */
    public Idemix.NymSignature toProto() {
        Idemix.NymSignature.Builder builder = Idemix.NymSignature.newBuilder()
                .setProofC(ByteString.copyFrom(IdemixUtils.bigToBytes(this.proofC)))
                .setProofSSk(ByteString.copyFrom(IdemixUtils.bigToBytes(this.proofSSk)))
                .setProofSRNym(ByteString.copyFrom(IdemixUtils.bigToBytes(this.proofSRNym)))
                .setNonce(ByteString.copyFrom(IdemixUtils.bigToBytes(this.nonce)));

        return builder.build();
    }
}
