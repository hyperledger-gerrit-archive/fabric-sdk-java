/*
 *
<<<<<<< HEAD
 *  Copyright 2017, 2018 IBM Corp. All Rights Reserved.
=======
 *  Copyright IBM Corp. All Rights Reserved.
>>>>>>> [FAB-6682] Adds identity mixer crypto in java
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
import java.util.HashSet;
import java.util.Set;

import com.google.protobuf.ByteString;
import org.apache.milagro.amcl.FP256BN.BIG;
import org.apache.milagro.amcl.FP256BN.ECP;
import org.apache.milagro.amcl.FP256BN.ECP2;
import org.apache.milagro.amcl.RAND;
import org.hyperledger.fabric.protos.idemix.Idemix;
import org.hyperledger.fabric.sdk.exception.CryptoException;

/**
 * IssuerPublicKey represents the idemix public key of an issuer (Certificate Authority).
 */
public class IssuerPublicKey {

    private final String[] AttributeNames;
    private final ECP Hsk;
    private final ECP HRand;
    private final ECP[] HAttrs;
    private final ECP2 W;
    private final ECP BarG1;
    private final ECP BarG2;
    private final BIG ProofC;
    private final BIG ProofS;
    private byte[] Hash = new byte[0];

    /**
     * Constructor
     * @param attributeNames the names of attributes as String array (must not contain duplicates)
     * @param rng a random number generator
     * @param isk the issuer secret key
     */
    public IssuerPublicKey(String[] attributeNames, RAND rng, BIG isk) throws CryptoException {
        // Checking if attribute names are unique
        Set<String> map = new HashSet<>();
        for (String item : attributeNames) {
            if (!map.add(item)) {
                throw new CryptoException("Attribute " + item + " appears multiple times in attributeNames");
            }
        }

        // Attaching Attribute Names array correctly
        this.AttributeNames = attributeNames;

        // Computing W value
        this.W = IdemixUtils.genG2.mul(isk);

        // Filling up HAttributes correctly in Issuer Public Key, length
        // preserving
        this.HAttrs = new ECP[attributeNames.length];

        for (int i = 0; i < attributeNames.length; i++) {
            this.HAttrs[i] = IdemixUtils.genG1.mul(IdemixUtils.randModOrder(rng));
        }

        // Generating Hsk value
        this.Hsk = IdemixUtils.genG1.mul(IdemixUtils.randModOrder(rng));

        // Generating HRand value
        this.HRand = IdemixUtils.genG1.mul(IdemixUtils.randModOrder(rng));

        // Generating BarG1 value
        this.BarG1 = IdemixUtils.genG1.mul(IdemixUtils.randModOrder(rng));

        // Generating BarG2 value
        this.BarG2 = this.BarG1.mul(isk);

        // Zero Knowledge Proofs

        // Computing t1 and t2 values with random local variable r for later use
        BIG r = IdemixUtils.randModOrder(rng);
        ECP2 t1 = IdemixUtils.genG2.mul(r);
        ECP t2 = this.BarG1.mul(r);

        // Generating proofData that will contain 3 elements in G1 (of size 2*FIELD_BYTES+1)and 3 elements in G2 (of size 4 * FIELD_BYTES)
        byte[] proofData = new byte[3 * (2 * IdemixUtils.FIELD_BYTES + 1) + 3 * (4 * IdemixUtils.FIELD_BYTES)];

        int index = 0;
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(t1));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(t2));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(IdemixUtils.genG2));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(this.BarG1));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(this.W));
        IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(this.BarG2));

        // Hashing proofData to proofC
        this.ProofC = IdemixUtils.hashModOrder(proofData);

        // Computing ProofS = (ProofC*isk) + r mod GROUP_ORDER
        this.ProofS = BIG.modmul(this.ProofC, isk, IdemixUtils.GROUP_ORDER).plus(r);
        this.ProofS.mod(IdemixUtils.GROUP_ORDER);

        // Computing Hash of IssuerPublicKey
        byte[] serializedIpk = this.toProto().toByteArray();
        this.Hash = IdemixUtils.bigToBytes(IdemixUtils.hashModOrder(serializedIpk));
    }

    /**
     * Construct an IssuerPublicKey from a serialized issuer public key
     */
    public IssuerPublicKey(Idemix.IssuerPublicKey proto) {
        this.AttributeNames = new String[proto.getAttributeNamesCount()];
        for (int i = 0; i < proto.getAttributeNamesCount(); i++) {
            this.AttributeNames[i] = proto.getAttributeNames(i);
        }

        this.HAttrs = new ECP[proto.getHAttrsCount()];
        for (int i = 0; i < proto.getHAttrsCount(); i++) {
            this.HAttrs[i] = IdemixUtils.transformFromProto(proto.getHAttrs(i));
        }

        this.BarG1 = IdemixUtils.transformFromProto(proto.getBarG1());
        this.BarG2 = IdemixUtils.transformFromProto(proto.getBarG2());
        this.HRand = IdemixUtils.transformFromProto(proto.getHRand());
        this.Hash = proto.getHash().toByteArray();
        this.Hsk = IdemixUtils.transformFromProto(proto.getHSk());
        this.ProofC = BIG.fromBytes(proto.getProofC().toByteArray());
        this.ProofS = BIG.fromBytes(proto.getProofS().toByteArray());
        this.W = IdemixUtils.transformFromProto(proto.getW());
    }


    /**
     * check whether the issuer public key is correct
     *
     * @return true iff valid
     */
    public boolean check() {
        // check formalities of IssuerPublicKey
        if (this.AttributeNames.length < 0 || this.Hsk == null || this.HRand == null || this.HAttrs == null
                || this.BarG1 == null || this.BarG1.is_infinity() || this.BarG2 == null
                || this.HAttrs.length < this.AttributeNames.length) {
            return false;
        }

        for (int i = 0; i < this.AttributeNames.length; i++) {
            if (this.HAttrs[i] == null) {
                return false;
            }
        }

        // check proofs
        ECP2 t1 = IdemixUtils.genG2.mul(this.ProofS);
        ECP t2 = this.BarG1.mul(this.ProofS);

        t1.add(W.mul(BIG.modneg(this.ProofC, IdemixUtils.GROUP_ORDER)));
        t2.add(BarG2.mul(BIG.modneg(this.ProofC, IdemixUtils.GROUP_ORDER)));

        // Generating proofData that will contain 3 elements in G1 (of size 2*FIELD_BYTES+1)and 3 elements in G2 (of size 4 * FIELD_BYTES)
        byte[] proofData = new byte[3 * (2 * IdemixUtils.FIELD_BYTES + 1) + 3 * (4 * IdemixUtils.FIELD_BYTES)];

        int index = 0;
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(t1));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(t2));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(IdemixUtils.genG2));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(this.BarG1));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(this.W));
        IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(this.BarG2));

        // Hash proofData to hproofdata and compare with proofC
        if (!Arrays.equals(IdemixUtils.bigToBytes(IdemixUtils.hashModOrder(proofData)), IdemixUtils.bigToBytes(this.ProofC))) {
            return false;
        }

        // check Hash
        byte[] hash = this.Hash;
        this.Hash = new byte[0];
        byte[] serializedIpk = this.toProto().toByteArray();
        this.Hash = hash;

        return Arrays.equals(hash, IdemixUtils.bigToBytes(IdemixUtils.hashModOrder(serializedIpk)));
    }

    /**
     * @return A proto version of this issuer public key
     */
    public Idemix.IssuerPublicKey toProto() {

        Idemix.ECP[] ipkHAttrs = new Idemix.ECP[this.HAttrs.length];
        for (int i = 0; i < this.HAttrs.length; i++) {
            ipkHAttrs[i] = IdemixUtils.transformToProto(this.HAttrs[i]);
        }

        return Idemix.IssuerPublicKey.newBuilder()
                .setProofC(ByteString.copyFrom(IdemixUtils.bigToBytes(this.ProofC)))
                .setProofS(ByteString.copyFrom(IdemixUtils.bigToBytes(this.ProofS)))
                .setW(IdemixUtils.transformToProto(this.W))
                .setHSk(IdemixUtils.transformToProto(this.Hsk))
                .setHRand(IdemixUtils.transformToProto(this.HRand))
                .addAllAttributeNames(Arrays.asList(this.AttributeNames))
                .setHash(ByteString.copyFrom(this.Hash))
                .setBarG1(IdemixUtils.transformToProto(this.BarG1))
                .setBarG2(IdemixUtils.transformToProto(this.BarG2))
                .addAllHAttrs(Arrays.asList(ipkHAttrs))
                .build();
    }

    public String[] getAttributeNames() {
        return AttributeNames;
    }

    public ECP getHsk() {
        return Hsk;
    }

    public ECP getHRand() {
        return HRand;
    }

    public ECP[] getHAttrs() {
        return HAttrs;
    }

    public ECP2 getW() {
        return W;
    }

    public byte[] getHash() {
        return Hash;
    }
}
