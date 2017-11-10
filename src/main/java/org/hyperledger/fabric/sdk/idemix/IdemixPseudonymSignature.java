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
 * IdemixPseudonymSignature is a signature on a message which can be verified with respect to a pseudonym
 */
public class IdemixPseudonymSignature {
    private final BIG proofC;
    private final BIG proofSSk;
    private final BIG nonce;
    private final BIG proofSRNym;

    private static final String NYM_SIGN_LABEL = "sign";

    /**
     * Constructor
     *
     * @param sk        the secret key
     * @param pseudonym the pseudonym with respect to which this signature can be verified
     * @param ipk       the issuer public key
     * @param msg       the message to be signed
     */
    public IdemixPseudonymSignature(BIG sk, IdemixPseudonym pseudonym, IdemixIssuerPublicKey ipk, byte[] msg) {
        if (sk == null || pseudonym == null || pseudonym.getNym() == null || pseudonym.getRandNym() == null || ipk == null || msg == null) {
            throw new IllegalArgumentException("Cannot create IdemixPseudonymSignature from null input");
        }
        nonce = IdemixUtils.randModOrder();

        //Construct Zero Knowledge Proof
        BIG rsk = IdemixUtils.randModOrder();
        BIG rRNym = IdemixUtils.randModOrder();
        ECP t = ipk.getHsk().mul2(rsk, ipk.getHRand(), rRNym);

        // create array for proof data that will contain the sign label, 2 ECPs (each of length 2* FIELD_BYTES + 1), the ipk hash and the message
        byte[] proofData = new byte[NYM_SIGN_LABEL.getBytes().length + 2 * (2 * IdemixUtils.FIELD_BYTES + 1) + ipk.getHash().length + msg.length];
        int index = 0;
        index = IdemixUtils.append(proofData, index, NYM_SIGN_LABEL.getBytes());
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(t));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(pseudonym.getNym()));
        index = IdemixUtils.append(proofData, index, ipk.getHash());
        IdemixUtils.append(proofData, index, msg);

        BIG cvalue = IdemixUtils.hashModOrder(proofData);

        byte[] finalProofData = new byte[2 * IdemixUtils.FIELD_BYTES];
        index = 0;
        index = IdemixUtils.append(finalProofData, index, IdemixUtils.bigToBytes(cvalue));
        IdemixUtils.append(finalProofData, index, IdemixUtils.bigToBytes(nonce));
        proofC = IdemixUtils.hashModOrder(finalProofData);

        proofSSk = new BIG(rsk);
        proofSSk.add(BIG.modmul(proofC, sk, IdemixUtils.GROUP_ORDER));
        proofSSk.mod(IdemixUtils.GROUP_ORDER);

        proofSRNym = new BIG(rRNym);
        proofSRNym.add(BIG.modmul(proofC, pseudonym.getRandNym(), IdemixUtils.GROUP_ORDER));
        proofSRNym.mod(IdemixUtils.GROUP_ORDER);
    }

    /**
     * Construct a new signature from a serialized IdemixPseudonymSignature
     *
     * @param proto a protobuf object representing an IdemixPseudonymSignature
     */
    public IdemixPseudonymSignature(Idemix.NymSignature proto) {
        if (proto == null) {
            throw new IllegalArgumentException("Cannot create idemix nym signature from null input");
        }
        proofC = BIG.fromBytes(proto.getProofC().toByteArray());
        proofSSk = BIG.fromBytes(proto.getProofSSk().toByteArray());
        proofSRNym = BIG.fromBytes(proto.getProofSRNym().toByteArray());
        nonce = BIG.fromBytes(proto.getNonce().toByteArray());
    }

    /**
     * Verify this IdemixPseudonymSignature
     *
     * @param nym the pseudonym with respect to which the signature is verified
     * @param ipk the issuer public key
     * @param msg the message that should be signed in this signature
     * @return true iff valid
     */
    public boolean verify(ECP nym, IdemixIssuerPublicKey ipk, byte[] msg) {
        if (nym == null || ipk == null || msg == null) {
            return false;
        }

        ECP t = ipk.getHsk().mul2(proofSSk, ipk.getHRand(), proofSRNym);
        t.sub(nym.mul(proofC));

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
        IdemixUtils.append(finalProofData, index, IdemixUtils.bigToBytes(nonce));

        byte[] hashedProofData = IdemixUtils.bigToBytes(IdemixUtils.hashModOrder(finalProofData));
        if (!Arrays.equals(IdemixUtils.bigToBytes(proofC), hashedProofData)) {
            return false;
        }
        return true;
    }

    /**
     * @return A proto object representing this IdemixPseudonymSignature
     */
    public Idemix.NymSignature toProto() {
        Idemix.NymSignature.Builder builder = Idemix.NymSignature.newBuilder()
                .setProofC(ByteString.copyFrom(IdemixUtils.bigToBytes(proofC)))
                .setProofSSk(ByteString.copyFrom(IdemixUtils.bigToBytes(proofSSk)))
                .setProofSRNym(ByteString.copyFrom(IdemixUtils.bigToBytes(proofSRNym)))
                .setNonce(ByteString.copyFrom(IdemixUtils.bigToBytes(nonce)));

        return builder.build();
    }
}
