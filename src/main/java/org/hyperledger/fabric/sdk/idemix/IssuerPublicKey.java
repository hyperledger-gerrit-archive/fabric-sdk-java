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

    public String[] AttributeNames;
    public ECP Hsk;
    public ECP HRand;
    public ECP[] HAttrs;
    public ECP2 W;
    private ECP BarG1;
    private ECP BarG2;
    private BIG ProofC;
    private BIG ProofS;
    public byte[] Hash = new byte[0];

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
        this.W = Utils.genG2.mul(isk);

        // Filling up HAttributes correctly in Issuer Public Key, length
        // preserving
        this.HAttrs = new ECP[attributeNames.length];

        for (int i = 0; i < attributeNames.length; i++) {
            this.HAttrs[i] = Utils.genG1.mul(Utils.randModOrder(rng));
        }

        // Generating Hsk value
        this.Hsk = Utils.genG1.mul(Utils.randModOrder(rng));

        // Generating HRand value
        this.HRand = Utils.genG1.mul(Utils.randModOrder(rng));

        // Generating BarG1 value
        this.BarG1 = Utils.genG1.mul(Utils.randModOrder(rng));

        // Generating BarG2 value
        this.BarG2 = this.BarG1.mul(isk);

        // Zero Knowledge Proofs

        // Computing t1 and t2 values with random local variable r for later use
        BIG r = Utils.randModOrder(rng);
        ECP2 t1 = Utils.genG2.mul(r);
        ECP t2 = this.BarG1.mul(r);

        // Generating proofData that will contain 3 elements in G1 (of size 2*fieldBytes+1)and 3 elements in G2 (of size 4 * fieldBytes)
        byte[] proofData = new byte[3 * (2 * Utils.fieldBytes + 1) + 3 * (4 * Utils.fieldBytes)];

        int index = 0;
        index = Utils.append(proofData, index, Utils.ecpToBytes(t1));
        index = Utils.append(proofData, index, Utils.ecpToBytes(t2));
        index = Utils.append(proofData, index, Utils.ecpToBytes(Utils.genG2));
        index = Utils.append(proofData, index, Utils.ecpToBytes(this.BarG1));
        index = Utils.append(proofData, index, Utils.ecpToBytes(this.W));
        index = Utils.append(proofData, index, Utils.ecpToBytes(this.BarG2));

        // Hashing proofData to proofC
        this.ProofC = Utils.hashModOrder(proofData);

        // Computing ProofS = (ProofC*isk) + r mod groupOrder
        this.ProofS = BIG.modmul(this.ProofC, isk, Utils.groupOrder).plus(r);
        this.ProofS.mod(Utils.groupOrder);

        // Computing Hash of IssuerPublicKey
        byte[] serializedIpk = this.toProto().toByteArray();
        this.Hash = Utils.bigToBytes(Utils.hashModOrder(serializedIpk));
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
            this.HAttrs[i] = Utils.transformFromProto(proto.getHAttrs(i));
        }

        this.BarG1 = Utils.transformFromProto(proto.getBarG1());
        this.BarG2 = Utils.transformFromProto(proto.getBarG2());
        this.HRand = Utils.transformFromProto(proto.getHRand());
        this.Hash = proto.getHash().toByteArray();
        this.Hsk = Utils.transformFromProto(proto.getHSk());
        this.ProofC = BIG.fromBytes(proto.getProofC().toByteArray());
        this.ProofS = BIG.fromBytes(proto.getProofS().toByteArray());
        this.W = Utils.transformFromProto(proto.getW());
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
        ECP2 t1 = Utils.genG2.mul(this.ProofS);
        ECP t2 = this.BarG1.mul(this.ProofS);

        t1.add(W.mul(BIG.modneg(this.ProofC, Utils.groupOrder)));
        t2.add(BarG2.mul(BIG.modneg(this.ProofC, Utils.groupOrder)));

        // Generating proofData that will contain 3 elements in G1 (of size 2*fieldBytes+1)and 3 elements in G2 (of size 4 * fieldBytes)
        byte[] proofData = new byte[3 * (2 * Utils.fieldBytes + 1) + 3 * (4 * Utils.fieldBytes)];

        int index = 0;
        index = Utils.append(proofData, index, Utils.ecpToBytes(t1));
        index = Utils.append(proofData, index, Utils.ecpToBytes(t2));
        index = Utils.append(proofData, index, Utils.ecpToBytes(Utils.genG2));
        index = Utils.append(proofData, index, Utils.ecpToBytes(this.BarG1));
        index = Utils.append(proofData, index, Utils.ecpToBytes(this.W));
        index = Utils.append(proofData, index, Utils.ecpToBytes(this.BarG2));

        // Hash proofData to hproofdata and compare with proofC
        if (!Arrays.equals(Utils.bigToBytes(Utils.hashModOrder(proofData)), Utils.bigToBytes(this.ProofC))) {
            return false;
        }

        // check Hash
        byte[] hash = this.Hash;
        this.Hash = new byte[0];
        byte[] serializedIpk = this.toProto().toByteArray();
        this.Hash = hash;

        if (!Arrays.equals(hash, Utils.bigToBytes(Utils.hashModOrder(serializedIpk)))) {
            return false;
        }

        return true;
    }

    /**
     * @return A proto version of this issuer public key
     */
    public Idemix.IssuerPublicKey toProto() {

        Idemix.ECP[] ipkHAttrs = new Idemix.ECP[this.HAttrs.length];
        for (int i = 0; i < this.HAttrs.length; i++) {
            ipkHAttrs[i] = Utils.transformToProto(this.HAttrs[i]);
        }

        Idemix.IssuerPublicKey proto = Idemix.IssuerPublicKey.newBuilder()
                .setProofC(ByteString.copyFrom(Utils.bigToBytes(this.ProofC)))
                .setProofS(ByteString.copyFrom(Utils.bigToBytes(this.ProofS)))
                .setW(Utils.transformToProto(this.W))
                .setHSk(Utils.transformToProto(this.Hsk))
                .setHRand(Utils.transformToProto(this.HRand))
                .addAllAttributeNames(Arrays.asList(this.AttributeNames))
                .setHash(ByteString.copyFrom(this.Hash))
                .setBarG1(Utils.transformToProto(this.BarG1))
                .setBarG2(Utils.transformToProto(this.BarG2))
                .addAllHAttrs(Arrays.asList(ipkHAttrs))
                .build();

        return proto;
    }

}
