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
import org.apache.milagro.amcl.FP256BN.FP12;
import org.apache.milagro.amcl.FP256BN.PAIR;
import org.apache.milagro.amcl.RAND;
import org.hyperledger.fabric.protos.idemix.Idemix;

/**
 * IdemixSignature represents an idemix signature, which is a zero knowledge proof
 * of knowledge of a BBS+ signature. The Camenisch-Drijvers-Lehmann ZKP (ia.cr/2016/663) is used
 */
public class IdemixSignature {

    private final ECP aPrime;
    private final ECP aBar;
    private final ECP bPrime;
    private final BIG proofC;
    private final BIG proofSSk;
    private final BIG proofSE;
    private final BIG proofSR2;
    private final BIG proofSR3;
    private final BIG proofSSPrime;
    private final BIG[] proofSAttrs;
    private final BIG nonce;
    private final ECP nym;
    private final BIG proofSRNym;

    private static final String SIGN_LABEL = "sign";

    /**
     * Some attributes may be hidden, some disclosed. The indices of the hidden attributes will be passed.
     *
     * @param disclosure an array of length of attributes with 0 and 1 for hide and disclose
     * @return an integer array of the hidden indices
     */
    public int[] hiddenIndices(byte[] disclosure) {
        int counter = 0;
        for (byte aDisclosure : disclosure) {
            if (aDisclosure == 0) {
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
     * Creates a new IdemixSignature by proving knowledge of a credential
     */
    public IdemixSignature(IdemixCredential c, BIG sk, ECP nym, BIG rNym, IssuerPublicKey ipk, byte[] disclosure, byte[] msg, RAND rng) {

        int[] hiddenIndices = hiddenIndices(disclosure);

        // Start signature
        BIG r1 = IdemixUtils.randModOrder(rng);
        BIG r2 = IdemixUtils.randModOrder(rng);
        BIG r3 = new BIG(r1);
        r3.invmodp(IdemixUtils.GROUP_ORDER);

        this.nonce = IdemixUtils.randModOrder(rng);

        this.aPrime = PAIR.G1mul(c.getA(), r1);
        this.aBar = PAIR.G1mul(c.getB(), r1);
        this.aBar.sub(PAIR.G1mul(aPrime, c.getE()));

        this.bPrime = PAIR.G1mul(c.getB(), r1);
        this.bPrime.sub(PAIR.G1mul(ipk.getHRand(), r2));
        BIG sPrime = new BIG(c.getS());
        sPrime.add(BIG.modneg(BIG.modmul(r2, r3, IdemixUtils.GROUP_ORDER), IdemixUtils.GROUP_ORDER));
        sPrime.mod(IdemixUtils.GROUP_ORDER);

        //Construct Zero Knowledge Proof
        BIG rsk = IdemixUtils.randModOrder(rng);
        BIG re = IdemixUtils.randModOrder(rng);
        BIG rR2 = IdemixUtils.randModOrder(rng);
        BIG rR3 = IdemixUtils.randModOrder(rng);
        BIG rSPrime = IdemixUtils.randModOrder(rng);
        BIG rRNym = IdemixUtils.randModOrder(rng);
        BIG[] rAttrs = new BIG[hiddenIndices.length];
        for (int i = 0; i < hiddenIndices.length; i++) {
            rAttrs[i] = IdemixUtils.randModOrder(rng);
        }

        ECP t1 = this.aPrime.mul2(re, ipk.getHRand(), rR2);
        ECP t2 = PAIR.G1mul(ipk.getHRand(), rSPrime);
        t2.add(bPrime.mul2(rR3, ipk.getHsk(), rsk));

        for (int i = 0; i < hiddenIndices.length / 2; i++) {
            t2.add(ipk.getHAttrs()[hiddenIndices[2 * i]].mul2(rAttrs[2 * i], ipk.getHAttrs()[hiddenIndices[2 * i + 1]], rAttrs[2 * i + 1]));
        }
        if (hiddenIndices.length % 2 != 0) {
            t2.add(PAIR.G1mul(ipk.getHAttrs()[hiddenIndices[hiddenIndices.length - 1]], rAttrs[hiddenIndices.length - 1]));
        }

        ECP t3 = ipk.getHsk().mul2(rsk, ipk.getHRand(), rRNym);

        // create proofData such that it can contain the sign label, 7 elements in G1 (each of size 2*FIELD_BYTES+1),
        // the ipk hash, the disclosure array, and the message
        byte[] proofData = new byte[SIGN_LABEL.getBytes().length + 7 * (2 * IdemixUtils.FIELD_BYTES + 1) + ipk.getHash().length + disclosure.length + msg.length];
        int index = 0;
        index = IdemixUtils.append(proofData, index, SIGN_LABEL.getBytes());
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(t1));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(t2));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(t3));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(aPrime));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(aBar));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(bPrime));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(nym));
        index = IdemixUtils.append(proofData, index, ipk.getHash());
        index = IdemixUtils.append(proofData, index, disclosure);
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

        this.proofSE = new BIG(re);
        this.proofSE.add(BIG.modneg(BIG.modmul(this.proofC, c.getE(), IdemixUtils.GROUP_ORDER), IdemixUtils.GROUP_ORDER));
        this.proofSE.mod(IdemixUtils.GROUP_ORDER);

        this.proofSR2 = new BIG(rR2);
        this.proofSR2.add(BIG.modmul(this.proofC, r2, IdemixUtils.GROUP_ORDER));
        this.proofSR2.mod(IdemixUtils.GROUP_ORDER);

        this.proofSR3 = new BIG(rR3);
        this.proofSR3.add(BIG.modneg(BIG.modmul(this.proofC, r3, IdemixUtils.GROUP_ORDER), IdemixUtils.GROUP_ORDER));
        this.proofSR3.mod(IdemixUtils.GROUP_ORDER);

        this.proofSSPrime = new BIG(rSPrime);
        this.proofSSPrime.add(BIG.modmul(this.proofC, sPrime, IdemixUtils.GROUP_ORDER));
        this.proofSSPrime.mod(IdemixUtils.GROUP_ORDER);

        this.proofSRNym = new BIG(rRNym);
        this.proofSRNym.add(BIG.modmul(this.proofC, rNym, IdemixUtils.GROUP_ORDER));
        this.proofSRNym.mod(IdemixUtils.GROUP_ORDER);

        this.nym = new ECP();
        this.nym.copy(nym);

        this.proofSAttrs = new BIG[hiddenIndices.length];
        for (int i = 0; i < hiddenIndices.length; i++) {
            this.proofSAttrs[i] = new BIG(rAttrs[i]);
            this.proofSAttrs[i].add(BIG.modmul(this.proofC, BIG.fromBytes(c.getAttrs()[hiddenIndices[i]]), IdemixUtils.GROUP_ORDER));
            this.proofSAttrs[i].mod(IdemixUtils.GROUP_ORDER);
        }

    }

    /**
     * Construct a new signature from a serialized IdemixSignature
     */
    public IdemixSignature(Idemix.Signature proto) {
        this.aBar = IdemixUtils.transformFromProto(proto.getABar());
        this.aPrime = IdemixUtils.transformFromProto(proto.getAPrime());
        this.bPrime = IdemixUtils.transformFromProto(proto.getBPrime());
        this.nym = IdemixUtils.transformFromProto(proto.getNym());
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

        FP12 temp1 = PAIR.ate(ipk.getW(), this.aPrime);
        FP12 temp2 = PAIR.ate(IdemixUtils.genG2, this.aBar);
        temp2.inverse();
        temp1.mul(temp2);
        if (!PAIR.fexp(temp1).isunity()) {
            return false;
        }

        ECP t1 = this.aPrime.mul2(this.proofSE, ipk.getHRand(), this.proofSR2);
        ECP temp = new ECP();
        temp.copy(this.aBar);
        temp.sub(this.bPrime);
        t1.sub(PAIR.G1mul(temp, this.proofC));

        ECP t2 = PAIR.G1mul(ipk.getHRand(), this.proofSSPrime);
        t2.add(this.bPrime.mul2(this.proofSR3, ipk.getHsk(), this.proofSSk));

        for (int i = 0; i < hiddenIndices.length / 2; i++) {
            t2.add(ipk.getHAttrs()[hiddenIndices[2 * i]].mul2(this.proofSAttrs[2 * i], ipk.getHAttrs()[hiddenIndices[2 * i + 1]], this.proofSAttrs[2 * i + 1]));
        }
        if (hiddenIndices.length % 2 != 0) {
            t2.add(PAIR.G1mul(ipk.getHAttrs()[hiddenIndices[hiddenIndices.length - 1]], this.proofSAttrs[hiddenIndices.length - 1]));
        }

        temp = new ECP();
        temp.copy(IdemixUtils.genG1);

        for (int i = 0; i < disclosure.length; i++) {
            if (disclosure[i] != 0) {
                temp.add(PAIR.G1mul(ipk.getHAttrs()[i], attributeValues[i]));
            }
        }
        t2.add(PAIR.G1mul(temp, this.proofC));

        ECP t3 = ipk.getHsk().mul2(this.proofSSk, ipk.getHRand(), this.proofSRNym);
        t3.sub(this.nym.mul(this.proofC));

        // create proofData such that it can contain the sign label, 7 elements in G1 (each of size 2*FIELD_BYTES+1),
        // the ipk hash, the disclosure array, and the message
        byte[] proofData = new byte[SIGN_LABEL.getBytes().length + 7 * (2 * IdemixUtils.FIELD_BYTES + 1) + ipk.getHash().length + disclosure.length + msg.length];
        int index = 0;
        index = IdemixUtils.append(proofData, index, SIGN_LABEL.getBytes());
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(t1));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(t2));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(t3));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(aPrime));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(aBar));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(bPrime));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(nym));
        index = IdemixUtils.append(proofData, index, ipk.getHash());
        index = IdemixUtils.append(proofData, index, disclosure);
        IdemixUtils.append(proofData, index, msg);

        BIG cvalue = IdemixUtils.hashModOrder(proofData);

        byte[] finalProofData = new byte[2 * IdemixUtils.FIELD_BYTES];
        index = 0;
        index = IdemixUtils.append(finalProofData, index, IdemixUtils.bigToBytes(cvalue));
        IdemixUtils.append(finalProofData, index, IdemixUtils.bigToBytes(this.nonce));

        byte[] hashedProofData = IdemixUtils.bigToBytes(IdemixUtils.hashModOrder(finalProofData));
        return Arrays.equals(IdemixUtils.bigToBytes(this.proofC), hashedProofData);
    }

    /**
     * Convert this signature to a proto
     */
    public Idemix.Signature toProto() {
        Idemix.Signature.Builder builder = Idemix.Signature.newBuilder()
                .setAPrime(IdemixUtils.transformToProto(this.aPrime))
                .setABar(IdemixUtils.transformToProto(this.aBar))
                .setBPrime(IdemixUtils.transformToProto(this.bPrime))
                .setNym(IdemixUtils.transformToProto(this.nym))
                .setProofC(ByteString.copyFrom(IdemixUtils.bigToBytes(this.proofC)))
                .setProofSSk(ByteString.copyFrom(IdemixUtils.bigToBytes(this.proofSSk)))
                .setProofSE(ByteString.copyFrom(IdemixUtils.bigToBytes(this.proofSE)))
                .setProofSR2(ByteString.copyFrom(IdemixUtils.bigToBytes(this.proofSR2)))
                .setProofSR3(ByteString.copyFrom(IdemixUtils.bigToBytes(this.proofSR3)))
                .setProofSRNym(ByteString.copyFrom(IdemixUtils.bigToBytes(this.proofSRNym)))
                .setProofSSPrime(ByteString.copyFrom(IdemixUtils.bigToBytes(this.proofSSPrime)))
                .setNonce(ByteString.copyFrom(IdemixUtils.bigToBytes(this.nonce)));

        for (BIG attr : this.proofSAttrs) {
            builder.addProofSAttrs(ByteString.copyFrom(IdemixUtils.bigToBytes(attr)));
        }

        return builder.build();
    }
}
