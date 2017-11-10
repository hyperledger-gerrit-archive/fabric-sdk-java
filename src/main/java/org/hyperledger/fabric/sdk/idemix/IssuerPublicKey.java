/**
 * Copyright 2017 IBM Corp.
 */

package org.hyperledger.fabric.sdk.idemix;

import com.google.protobuf.ByteString;
import com.ibm.zurich.amcl.BN254.BIG;
import com.ibm.zurich.amcl.BN254.ECP;
import com.ibm.zurich.amcl.BN254.ECP2;
import com.ibm.zurich.amcl.RAND;
import org.hyperledger.fabric.protos.idemix.Idemix;
import org.hyperledger.fabric.sdk.exception.CryptoException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
     * @param AttributeNames the names of attributes as String array (must not contain duplicates)
     * @param rng a random number generator
     * @param isk the issuer secret key
     */
    public IssuerPublicKey(String[] AttributeNames, RAND rng, BIG isk) throws CryptoException {
        // Checking if attribute names are unique
        Set<String> map = new HashSet<>();
        for (String item : AttributeNames) {
            if (!map.add(item)) {
                throw new CryptoException("Attribute " + item + " appears multiple times in AttributeNames");
            }
        }

        // Attaching Attribute Names array correctly
        this.AttributeNames = AttributeNames;

        // Computing W value
        this.W = Utils.GenG2.mul(isk);

        // Filling up HAttributes correctly in Issuer Public Key, length
        // preserving
        this.HAttrs = new ECP[AttributeNames.length];

        for (int i = 0; i < AttributeNames.length; i++) {
            this.HAttrs[i] = Utils.GenG1.mul(Utils.RandModOrder(rng));
        }

        // Generating Hsk value
        this.Hsk = Utils.GenG1.mul(Utils.RandModOrder(rng));

        // Generating HRand value
        this.HRand = Utils.GenG1.mul(Utils.RandModOrder(rng));

        // Generating BarG1 value
        this.BarG1 = Utils.GenG1.mul(Utils.RandModOrder(rng));

        // Generating BarG2 value
        this.BarG2 = this.BarG1.mul(isk);

        // Zero Knowledge Proofs

        // Computing t1 and t2 values with random local variable r for later use
        BIG r = Utils.RandModOrder(rng);
        ECP2 t1 = Utils.GenG2.mul(r);
        ECP t2 = this.BarG1.mul(r);

        // Generating proofData that will contain 3 elements in G1 (of size 2*FieldBytes+1)and 3 elements in G2 (of size 4 * FieldBytes)
        byte[] proofData = new byte[3 * (2 * Utils.FieldBytes + 1) + 3 * (4 * Utils.FieldBytes)];

        int index = 0;
        index = Utils.append(proofData, index, Utils.EcpToBytes(t1));
        index = Utils.append(proofData, index, Utils.EcpToBytes(t2));
        index = Utils.append(proofData, index, Utils.EcpToBytes(Utils.GenG2));
        index = Utils.append(proofData, index, Utils.EcpToBytes(this.BarG1));
        index = Utils.append(proofData, index, Utils.EcpToBytes(this.W));
        index = Utils.append(proofData, index, Utils.EcpToBytes(this.BarG2));

        // Hashing proofData to proofC
        this.ProofC = Utils.HashModOrder(proofData);

        // Computing ProofS = (ProofC*isk) + r mod GroupOrder
        this.ProofS = BIG.modmul(this.ProofC, isk, Utils.GroupOrder).plus(r);
        this.ProofS.mod(Utils.GroupOrder);

        // Computing Hash of IssuerPublicKey
        byte[] serializedIpk = this.toProto().toByteArray();
        this.Hash = Utils.BigToBytes(Utils.HashModOrder(serializedIpk));
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
     * Check whether the issuer public key is correct
     *
     * @return true iff valid
     */
    public boolean Check() {
        // Check formalities of IssuerPublicKey
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

        // Check proofs
        ECP2 t1 = Utils.GenG2.mul(this.ProofS);
        ECP t2 = this.BarG1.mul(this.ProofS);

        t1.add(W.mul(BIG.modneg(this.ProofC, Utils.GroupOrder)));
        t2.add(BarG2.mul(BIG.modneg(this.ProofC, Utils.GroupOrder)));

        // Generating proofData that will contain 3 elements in G1 (of size 2*FieldBytes+1)and 3 elements in G2 (of size 4 * FieldBytes)
        byte[] proofData = new byte[3 * (2 * Utils.FieldBytes + 1) + 3 * (4 * Utils.FieldBytes)];

        int index = 0;
        index = Utils.append(proofData, index, Utils.EcpToBytes(t1));
        index = Utils.append(proofData, index, Utils.EcpToBytes(t2));
        index = Utils.append(proofData, index, Utils.EcpToBytes(Utils.GenG2));
        index = Utils.append(proofData, index, Utils.EcpToBytes(this.BarG1));
        index = Utils.append(proofData, index, Utils.EcpToBytes(this.W));
        index = Utils.append(proofData, index, Utils.EcpToBytes(this.BarG2));

        // Hash proofData to hproofdata and compare with proofC
        if (!Arrays.equals(Utils.BigToBytes(Utils.HashModOrder(proofData)), Utils.BigToBytes(this.ProofC))) {
            return false;
        }

        // Check Hash
        byte[] hash = this.Hash;
        this.Hash = new byte[0];
        byte[] serializedIpk = this.toProto().toByteArray();
        this.Hash = hash;

        if (!Arrays.equals(hash, Utils.BigToBytes(Utils.HashModOrder(serializedIpk)))) {
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
                .setProofC(ByteString.copyFrom(Utils.BigToBytes(this.ProofC)))
                .setProofS(ByteString.copyFrom(Utils.BigToBytes(this.ProofS)))
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
