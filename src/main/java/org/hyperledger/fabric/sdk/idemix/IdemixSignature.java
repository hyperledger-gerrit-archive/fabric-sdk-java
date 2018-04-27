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
        if (disclosure == null) {
            throw new IllegalArgumentException("cannot compute hidden indices of null disclosure");
        }
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
    public IdemixSignature(IdemixCredential c, BIG sk, IdemixPseudonym pseudonym, IdemixIssuerPublicKey ipk, byte[] disclosure, byte[] msg) {
        if (c == null || sk == null || pseudonym == null || pseudonym.getNym() == null || pseudonym.getRandNym() == null || ipk == null || disclosure == null || msg == null) {
            throw new IllegalArgumentException("Cannot construct idemix signature from null input");
        }
        int[] hiddenIndices = hiddenIndices(disclosure);

        // Start signature
        BIG r1 = IdemixUtils.randModOrder();
        BIG r2 = IdemixUtils.randModOrder();
        BIG r3 = new BIG(r1);
        r3.invmodp(IdemixUtils.GROUP_ORDER);

        nonce = IdemixUtils.randModOrder();

        aPrime = PAIR.G1mul(c.getA(), r1);
        aBar = PAIR.G1mul(c.getB(), r1);
        aBar.sub(PAIR.G1mul(aPrime, c.getE()));

        bPrime = PAIR.G1mul(c.getB(), r1);
        bPrime.sub(PAIR.G1mul(ipk.getHRand(), r2));
        BIG sPrime = new BIG(c.getS());
        sPrime.add(BIG.modneg(BIG.modmul(r2, r3, IdemixUtils.GROUP_ORDER), IdemixUtils.GROUP_ORDER));
        sPrime.mod(IdemixUtils.GROUP_ORDER);

        //Construct Zero Knowledge Proof
        BIG rsk = IdemixUtils.randModOrder();
        BIG re = IdemixUtils.randModOrder();
        BIG rR2 = IdemixUtils.randModOrder();
        BIG rR3 = IdemixUtils.randModOrder();
        BIG rSPrime = IdemixUtils.randModOrder();
        BIG rRNym = IdemixUtils.randModOrder();
        BIG[] rAttrs = new BIG[hiddenIndices.length];
        for (int i = 0; i < hiddenIndices.length; i++) {
            rAttrs[i] = IdemixUtils.randModOrder();
        }

        ECP t1 = aPrime.mul2(re, ipk.getHRand(), rR2);
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
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(pseudonym.getNym()));
        index = IdemixUtils.append(proofData, index, ipk.getHash());
        index = IdemixUtils.append(proofData, index, disclosure);
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

        proofSE = new BIG(re);
        proofSE.add(BIG.modneg(BIG.modmul(proofC, c.getE(), IdemixUtils.GROUP_ORDER), IdemixUtils.GROUP_ORDER));
        proofSE.mod(IdemixUtils.GROUP_ORDER);

        proofSR2 = new BIG(rR2);
        proofSR2.add(BIG.modmul(proofC, r2, IdemixUtils.GROUP_ORDER));
        proofSR2.mod(IdemixUtils.GROUP_ORDER);

        proofSR3 = new BIG(rR3);
        proofSR3.add(BIG.modneg(BIG.modmul(proofC, r3, IdemixUtils.GROUP_ORDER), IdemixUtils.GROUP_ORDER));
        proofSR3.mod(IdemixUtils.GROUP_ORDER);

        proofSSPrime = new BIG(rSPrime);
        proofSSPrime.add(BIG.modmul(proofC, sPrime, IdemixUtils.GROUP_ORDER));
        proofSSPrime.mod(IdemixUtils.GROUP_ORDER);

        proofSRNym = new BIG(rRNym);
        proofSRNym.add(BIG.modmul(proofC, pseudonym.getRandNym(), IdemixUtils.GROUP_ORDER));
        proofSRNym.mod(IdemixUtils.GROUP_ORDER);

        nym = new ECP();
        nym.copy(pseudonym.getNym());

        proofSAttrs = new BIG[hiddenIndices.length];
        for (int i = 0; i < hiddenIndices.length; i++) {
            proofSAttrs[i] = new BIG(rAttrs[i]);
            proofSAttrs[i].add(BIG.modmul(proofC, BIG.fromBytes(c.getAttrs()[hiddenIndices[i]]), IdemixUtils.GROUP_ORDER));
            proofSAttrs[i].mod(IdemixUtils.GROUP_ORDER);
        }

    }

    /**
     * Construct a new signature from a serialized IdemixSignature
     */
    public IdemixSignature(Idemix.Signature proto) {
        if (proto == null) {
            throw new IllegalArgumentException("Cannot construct idemix signature from null input");
        }
        aBar = IdemixUtils.transformFromProto(proto.getABar());
        aPrime = IdemixUtils.transformFromProto(proto.getAPrime());
        bPrime = IdemixUtils.transformFromProto(proto.getBPrime());
        nym = IdemixUtils.transformFromProto(proto.getNym());
        proofC = BIG.fromBytes(proto.getProofC().toByteArray());
        proofSSk = BIG.fromBytes(proto.getProofSSk().toByteArray());
        proofSE = BIG.fromBytes(proto.getProofSE().toByteArray());
        proofSR2 = BIG.fromBytes(proto.getProofSR2().toByteArray());
        proofSR3 = BIG.fromBytes(proto.getProofSR3().toByteArray());
        proofSSPrime = BIG.fromBytes(proto.getProofSSPrime().toByteArray());
        proofSRNym = BIG.fromBytes(proto.getProofSRNym().toByteArray());
        nonce = BIG.fromBytes(proto.getNonce().toByteArray());
        proofSAttrs = new BIG[proto.getProofSAttrsCount()];
        for (int i = 0; i < proto.getProofSAttrsCount(); i++) {
            proofSAttrs[i] = BIG.fromBytes(proto.getProofSAttrs(i).toByteArray());
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
    public boolean ver(byte[] disclosure, IdemixIssuerPublicKey ipk, byte[] msg, BIG[] attributeValues) {
        if (disclosure == null || ipk == null || msg == null || attributeValues == null || attributeValues.length != ipk.getAttributeNames().length || disclosure.length != ipk.getAttributeNames().length) {
            return false;
        }
        for (int i = 0; i < ipk.getAttributeNames().length; i++) {
            if (disclosure[i] == 1 && attributeValues[i] == null) {
                return false;
            }
        }

        int[] hiddenIndices = hiddenIndices(disclosure);

        if (proofSAttrs.length != hiddenIndices.length) {
            return false;
        }

        if (aPrime.is_infinity()) {
            return false;
        }

        FP12 temp1 = PAIR.ate(ipk.getW(), aPrime);
        FP12 temp2 = PAIR.ate(IdemixUtils.genG2, aBar);
        temp2.inverse();
        temp1.mul(temp2);
        if (!PAIR.fexp(temp1).isunity()) {
            return false;
        }

        ECP t1 = aPrime.mul2(proofSE, ipk.getHRand(), proofSR2);
        ECP temp = new ECP();
        temp.copy(aBar);
        temp.sub(bPrime);
        t1.sub(PAIR.G1mul(temp, proofC));

        ECP t2 = PAIR.G1mul(ipk.getHRand(), proofSSPrime);
        t2.add(bPrime.mul2(proofSR3, ipk.getHsk(), proofSSk));

        for (int i = 0; i < hiddenIndices.length / 2; i++) {
            t2.add(ipk.getHAttrs()[hiddenIndices[2 * i]].mul2(proofSAttrs[2 * i], ipk.getHAttrs()[hiddenIndices[2 * i + 1]], proofSAttrs[2 * i + 1]));
        }
        if (hiddenIndices.length % 2 != 0) {
            t2.add(PAIR.G1mul(ipk.getHAttrs()[hiddenIndices[hiddenIndices.length - 1]], proofSAttrs[hiddenIndices.length - 1]));
        }

        temp = new ECP();
        temp.copy(IdemixUtils.genG1);

        for (int i = 0; i < disclosure.length; i++) {
            if (disclosure[i] != 0) {
                temp.add(PAIR.G1mul(ipk.getHAttrs()[i], attributeValues[i]));
            }
        }
        t2.add(PAIR.G1mul(temp, proofC));

        ECP t3 = ipk.getHsk().mul2(proofSSk, ipk.getHRand(), proofSRNym);
        t3.sub(nym.mul(proofC));

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
        IdemixUtils.append(finalProofData, index, IdemixUtils.bigToBytes(nonce));

        byte[] hashedProofData = IdemixUtils.bigToBytes(IdemixUtils.hashModOrder(finalProofData));
        return Arrays.equals(IdemixUtils.bigToBytes(proofC), hashedProofData);
    }

    /**
     * Convert this signature to a proto
     */
    public Idemix.Signature toProto() {
        Idemix.Signature.Builder builder = Idemix.Signature.newBuilder()
                .setAPrime(IdemixUtils.transformToProto(aPrime))
                .setABar(IdemixUtils.transformToProto(aBar))
                .setBPrime(IdemixUtils.transformToProto(bPrime))
                .setNym(IdemixUtils.transformToProto(nym))
                .setProofC(ByteString.copyFrom(IdemixUtils.bigToBytes(proofC)))
                .setProofSSk(ByteString.copyFrom(IdemixUtils.bigToBytes(proofSSk)))
                .setProofSE(ByteString.copyFrom(IdemixUtils.bigToBytes(proofSE)))
                .setProofSR2(ByteString.copyFrom(IdemixUtils.bigToBytes(proofSR2)))
                .setProofSR3(ByteString.copyFrom(IdemixUtils.bigToBytes(proofSR3)))
                .setProofSRNym(ByteString.copyFrom(IdemixUtils.bigToBytes(proofSRNym)))
                .setProofSSPrime(ByteString.copyFrom(IdemixUtils.bigToBytes(proofSSPrime)))
                .setNonce(ByteString.copyFrom(IdemixUtils.bigToBytes(nonce)));

        for (BIG attr : proofSAttrs) {
            builder.addProofSAttrs(ByteString.copyFrom(IdemixUtils.bigToBytes(attr)));
        }

        return builder.build();
    }
}
