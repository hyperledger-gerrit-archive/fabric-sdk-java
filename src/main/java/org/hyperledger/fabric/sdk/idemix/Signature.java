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

import java.util.Arrays;

import com.google.protobuf.ByteString;
import org.apache.milagro.amcl.FP256BN.BIG;
import org.apache.milagro.amcl.FP256BN.ECP;
import org.apache.milagro.amcl.FP256BN.FP12;
import org.apache.milagro.amcl.FP256BN.PAIR;
import org.apache.milagro.amcl.RAND;
import org.hyperledger.fabric.protos.idemix.Idemix;

/**
 * Signature represents an idemix signature, which is a zero knowledge proof
 * of knowledge of a BBS+ signature. The ZK proof is taken from http:/ia.cr/2016/663.pdf.
 */
public class Signature {

    private ECP aPrime;
    private ECP aBar;
    private ECP bPrime;
    private BIG proofC;
    private BIG proofSSk;
    private BIG proofSE;
    private BIG proofSR2;
    private BIG proofSR3;
    private BIG proofSSPrime;
    private BIG[] proofSAttrs;
    private BIG nonce;
    private ECP nym;
    private BIG proofSRNym;

    public static final String SIGN_LABEL = "sign";

    /**
     * Some attributes may be hidden, some disclosed. The indices of the hidden attributes will be passed.
     *
     * @param disclosure an array of length of attributes with 0 and 1 for hide and disclose
     * @return an integer array of the hidden indices
     */
    public int[] hiddenIndices(byte[] disclosure) {

        int counter = 0;
        for (int i = 0; i < disclosure.length; i++) {
            if (disclosure[i] == 0) {
                counter++;
            }
        }

        int[] hiddenIndices = new int[counter];

        for (int i = 0; i < disclosure.length; i++) {
            if (disclosure[i] == 0) {
                hiddenIndices[i] = i;
            }
        }
        return hiddenIndices;
    }

    /**
     * Creates a new Signature by proving knowledge of a credential
     */
    public Signature(Credential c, BIG sk, ECP nym, BIG rNym, IssuerPublicKey ipk, byte[] disclosure, byte[] msg, RAND rng) {

        int[] hiddenIndices = hiddenIndices(disclosure);

        // Start signature
        BIG r1 = Utils.randModOrder(rng);
        BIG r2 = Utils.randModOrder(rng);
        BIG r3 = new BIG(r1);
        r3.invmodp(Utils.groupOrder);

        this.nonce = Utils.randModOrder(rng);

        this.aPrime = PAIR.G1mul(c.getA(), r1);
        this.aBar = PAIR.G1mul(c.getB(), r1);
        this.aBar.sub(PAIR.G1mul(aPrime, c.getE()));

        this.bPrime = PAIR.G1mul(c.getB(), r1);
        this.bPrime.sub(PAIR.G1mul(ipk.HRand, r2));
        BIG sPrime = new BIG(c.getS());
        sPrime.add(BIG.modneg(BIG.modmul(r2, r3, Utils.groupOrder), Utils.groupOrder));
        sPrime.mod(Utils.groupOrder);

        //Construct Zero Knowledge Proof
        BIG rsk = Utils.randModOrder(rng);
        BIG re = Utils.randModOrder(rng);
        BIG rR2 = Utils.randModOrder(rng);
        BIG rR3 = Utils.randModOrder(rng);
        BIG rSPrime = Utils.randModOrder(rng);
        BIG rRNym = Utils.randModOrder(rng);
        BIG[] rAttrs = new BIG[hiddenIndices.length];
        for (int i = 0; i < hiddenIndices.length; i++) {
            rAttrs[i] = Utils.randModOrder(rng);
        }

        ECP t1 = this.aPrime.mul2(re, ipk.HRand, rR2);
        ECP t2 = PAIR.G1mul(ipk.HRand, rSPrime);
        t2.add(bPrime.mul2(rR3, ipk.Hsk, rsk));

        for (int i = 0; i < hiddenIndices.length / 2; i++) {
            t2.add(ipk.HAttrs[hiddenIndices[2 * i]].mul2(rAttrs[2 * i], ipk.HAttrs[hiddenIndices[2 * i + 1]], rAttrs[2 * i + 1]));
        }
        if (hiddenIndices.length % 2 != 0) {
            t2.add(PAIR.G1mul(ipk.HAttrs[hiddenIndices[hiddenIndices.length - 1]], rAttrs[hiddenIndices.length - 1]));
        }

        ECP t3 = ipk.Hsk.mul2(rsk, ipk.HRand, rRNym);

        // create proofData such that it can contain the sign label, 7 elements in G1 (each of size 2*fieldBytes+1),
        // the ipk hash, the disclosure array, and the message
        byte[] proofData = new byte[SIGN_LABEL.getBytes().length + 7 * (2 * Utils.fieldBytes + 1) + ipk.Hash.length + disclosure.length + msg.length];
        int index = 0;
        index = Utils.append(proofData, index, SIGN_LABEL.getBytes());
        index = Utils.append(proofData, index, Utils.ecpToBytes(t1));
        index = Utils.append(proofData, index, Utils.ecpToBytes(t2));
        index = Utils.append(proofData, index, Utils.ecpToBytes(t3));
        index = Utils.append(proofData, index, Utils.ecpToBytes(aPrime));
        index = Utils.append(proofData, index, Utils.ecpToBytes(aBar));
        index = Utils.append(proofData, index, Utils.ecpToBytes(bPrime));
        index = Utils.append(proofData, index, Utils.ecpToBytes(nym));
        index = Utils.append(proofData, index, ipk.Hash);
        index = Utils.append(proofData, index, disclosure);
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

        this.proofSE = new BIG(re);
        this.proofSE.add(BIG.modneg(BIG.modmul(this.proofC, c.getE(), Utils.groupOrder), Utils.groupOrder));
        this.proofSE.mod(Utils.groupOrder);

        this.proofSR2 = new BIG(rR2);
        this.proofSR2.add(BIG.modmul(this.proofC, r2, Utils.groupOrder));
        this.proofSR2.mod(Utils.groupOrder);

        this.proofSR3 = new BIG(rR3);
        this.proofSR3.add(BIG.modneg(BIG.modmul(this.proofC, r3, Utils.groupOrder), Utils.groupOrder));
        this.proofSR3.mod(Utils.groupOrder);

        this.proofSSPrime = new BIG(rSPrime);
        this.proofSSPrime.add(BIG.modmul(this.proofC, sPrime, Utils.groupOrder));
        this.proofSSPrime.mod(Utils.groupOrder);

        this.proofSRNym = new BIG(rRNym);
        this.proofSRNym.add(BIG.modmul(this.proofC, rNym, Utils.groupOrder));
        this.proofSRNym.mod(Utils.groupOrder);

        this.nym = new ECP();
        this.nym.copy(nym);

        this.proofSAttrs = new BIG[hiddenIndices.length];
        byte[] b = new byte[Utils.fieldBytes];
        for (int i = 0; i < hiddenIndices.length; i++) {
            this.proofSAttrs[i] = new BIG(rAttrs[i]);
            this.proofSAttrs[i].add(BIG.modmul(this.proofC, BIG.fromBytes(c.getAttrs()[hiddenIndices[i]]), Utils.groupOrder));
            this.proofSAttrs[i].mod(Utils.groupOrder);
        }

    }

    /**
     * Construct a new signature from a serialized Signature
     */
    public Signature(Idemix.Signature proto) {
        this.aBar = Utils.transformFromProto(proto.getABar());
        this.aPrime = Utils.transformFromProto(proto.getAPrime());
        this.bPrime = Utils.transformFromProto(proto.getBPrime());
        this.nym = Utils.transformFromProto(proto.getNym());
        this.proofC = BIG.fromBytes(proto.getProofC().toByteArray());
        this.proofSSk = BIG.fromBytes(proto.getProofSSk().toByteArray());
        this.proofSE = BIG.fromBytes(proto.getProofSE().toByteArray());
        this.proofSR2 = BIG.fromBytes(proto.getProofSR2().toByteArray());
        this.proofSR3 = BIG.fromBytes(proto.getProofSR3().toByteArray());
        this.proofSSPrime = BIG.fromBytes(proto.getProofSSPrime().toByteArray());
        this.proofSRNym = BIG.fromBytes(proto.getProofSRNym().toByteArray());
        this.nonce = BIG.fromBytes(proto.getNonce().toByteArray());
        this.proofSAttrs = new BIG[proto.getProofSAttrsCount()];
        for (int i = 0; i < proto.getProofSAttrsCount(); i++) {
            this.proofSAttrs[i] = BIG.fromBytes(proto.getProofSAttrs(i).toByteArray());
        }
    }

    /**
     * Verify this signature
     *
     * @param disclosure      an array indicating which attributes it expects to be disclosed
     * @param ipk             the issuer public key
     * @param msg             the message that should be signed in this signature
     * @param attributeValues BIG array with attributeValues[i] contains the desired attribute value for the i-th undisclosed attribute in disclosure
     * @return true iff valid
     */
    public boolean ver(byte[] disclosure, IssuerPublicKey ipk, byte[] msg, BIG[] attributeValues) {

        int[] hiddenIndices = hiddenIndices(disclosure);

        if (this.proofSAttrs.length != hiddenIndices.length) {
            return false;
        }

        if (this.aPrime.is_infinity()) {
            return false;
        }

        FP12 temp1 = PAIR.ate(ipk.W, this.aPrime);
        FP12 temp2 = PAIR.ate(Utils.genG2, this.aBar);
        temp2.inverse();
        temp1.mul(temp2);
        if (!PAIR.fexp(temp1).isunity()) {
            return false;
        }

        ECP t1 = this.aPrime.mul2(this.proofSE, ipk.HRand, this.proofSR2);
        ECP temp = new ECP();
        temp.copy(this.aBar);
        temp.sub(this.bPrime);
        t1.sub(PAIR.G1mul(temp, this.proofC));

        ECP t2 = PAIR.G1mul(ipk.HRand, this.proofSSPrime);
        t2.add(this.bPrime.mul2(this.proofSR3, ipk.Hsk, this.proofSSk));

        for (int i = 0; i < hiddenIndices.length / 2; i++) {
            t2.add(ipk.HAttrs[hiddenIndices[2 * i]].mul2(this.proofSAttrs[2 * i], ipk.HAttrs[hiddenIndices[2 * i + 1]], this.proofSAttrs[2 * i + 1]));
        }
        if (hiddenIndices.length % 2 != 0) {
            t2.add(PAIR.G1mul(ipk.HAttrs[hiddenIndices[hiddenIndices.length - 1]], this.proofSAttrs[hiddenIndices.length - 1]));
        }

        temp = new ECP();
        temp.copy(Utils.genG1);

        for (int i = 0; i < disclosure.length; i++) {
            if (disclosure[i] != 0) {
                temp.add(PAIR.G1mul(ipk.HAttrs[i], attributeValues[i]));
            }
        }
        t2.add(PAIR.G1mul(temp, this.proofC));

        ECP t3 = ipk.Hsk.mul2(this.proofSSk, ipk.HRand, this.proofSRNym);
        t3.sub(this.nym.mul(this.proofC));

        // create proofData such that it can contain the sign label, 7 elements in G1 (each of size 2*fieldBytes+1),
        // the ipk hash, the disclosure array, and the message
        byte[] proofData = new byte[SIGN_LABEL.getBytes().length + 7 * (2 * Utils.fieldBytes + 1) + ipk.Hash.length + disclosure.length + msg.length];
        int index = 0;
        index = Utils.append(proofData, index, SIGN_LABEL.getBytes());
        index = Utils.append(proofData, index, Utils.ecpToBytes(t1));
        index = Utils.append(proofData, index, Utils.ecpToBytes(t2));
        index = Utils.append(proofData, index, Utils.ecpToBytes(t3));
        index = Utils.append(proofData, index, Utils.ecpToBytes(aPrime));
        index = Utils.append(proofData, index, Utils.ecpToBytes(aBar));
        index = Utils.append(proofData, index, Utils.ecpToBytes(bPrime));
        index = Utils.append(proofData, index, Utils.ecpToBytes(nym));
        index = Utils.append(proofData, index, ipk.Hash);
        index = Utils.append(proofData, index, disclosure);
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
     * Convert this signature to a proto
     */
    public Idemix.Signature toProto() {
        Idemix.Signature.Builder builder = Idemix.Signature.newBuilder()
                .setAPrime(Utils.transformToProto(this.aPrime))
                .setABar(Utils.transformToProto(this.aBar))
                .setBPrime(Utils.transformToProto(this.bPrime))
                .setNym(Utils.transformToProto(this.nym))
                .setProofC(ByteString.copyFrom(Utils.bigToBytes(this.proofC)))
                .setProofSSk(ByteString.copyFrom(Utils.bigToBytes(this.proofSSk)))
                .setProofSE(ByteString.copyFrom(Utils.bigToBytes(this.proofSE)))
                .setProofSR2(ByteString.copyFrom(Utils.bigToBytes(this.proofSR2)))
                .setProofSR3(ByteString.copyFrom(Utils.bigToBytes(this.proofSR3)))
                .setProofSRNym(ByteString.copyFrom(Utils.bigToBytes(this.proofSRNym)))
                .setProofSSPrime(ByteString.copyFrom(Utils.bigToBytes(this.proofSSPrime)))
                .setNonce(ByteString.copyFrom(Utils.bigToBytes(this.nonce)));

        for (BIG attr : this.proofSAttrs) {
            builder.addProofSAttrs(ByteString.copyFrom(Utils.bigToBytes(attr)));
        }

        return builder.build();
    }
}
