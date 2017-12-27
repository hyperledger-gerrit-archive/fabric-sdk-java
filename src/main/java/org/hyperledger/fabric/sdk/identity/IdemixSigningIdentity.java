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

package org.hyperledger.fabric.sdk.identity;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.milagro.amcl.FP256BN.BIG;
import org.apache.milagro.amcl.RAND;
import org.hyperledger.fabric.protos.idemix.Idemix;
import org.hyperledger.fabric.protos.msp.Identities.SerializedIdentity;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.idemix.Credential;
import org.hyperledger.fabric.sdk.idemix.IssuerPublicKey;
import org.hyperledger.fabric.sdk.idemix.NymSignature;
import org.hyperledger.fabric.sdk.idemix.Pseudonym;
import org.hyperledger.fabric.sdk.idemix.Signature;
import org.hyperledger.fabric.sdk.idemix.Utils;

/**
 * IdemixSigningIdentity is an Idemix implementation of the SigningIdentity
 * It contains IdemixIdentity (a public part) and
 * a corresponding secret part that contains
 * a credential (certificate) that certifies the user secret key and the attributes
 * a pseudonym value (a commitment to the user secret) and the corresponding commitment opening (randomness)
 */
public class IdemixSigningIdentity implements SigningIdentity {
    // public part of the signing identity (passed with the signature)
    private IdemixIdentity idemixIdentity;

    // public key of the Idemix CA (issuer)
    private IssuerPublicKey ipk;

    // random number generator
    private RAND rng;

    // Idemix credential (includes attribute values, user secret,
    // and the Issuer's signature)
    private Credential cred;

    // user's secret
    private BIG sk;

    // idemix pseudonym (represents Idemix identity)
    private Pseudonym pseudonym;

    // MSP identifier
    private String mspId;

    // attribute values
    private BIG[] attributes;

    // attribute names
    public static final String ATTRIBUTE_NAM_ROLE = "Role";
    public static final String ATTRIBUTE_NAM_OU = "OU";

    // organization unit attribute
    private byte[] ouBytes;
    // role attribute
    private byte[] roleBytes;

    //empty message to sign in the validate identity proof
    private byte[] msg = {};

    // proof that the identity is valid (proof of possession of a credential
    // with respect to a pseudonym.
    private Signature proof;

    // discloseFlags will be passed to the idemix signing and verification routines.
    // It informs idemix to disclose both attributes (OU and Role) when signing.
    private byte[] disclosedFlags = new byte[]{1, 1};


    /**
     * Create new Idemix Signing Identity with a fresh pseudonym
     *
     * @param ipk   issuer public key
     * @param mspId MSP identifier
     * @param sk    user's secret
     * @param cred  idemix credential
     * @throws CryptoException
     */
    public IdemixSigningIdentity(IssuerPublicKey ipk, String mspId, BIG sk, Credential cred) throws CryptoException {
        this(ipk, mspId, sk, cred, null, null, Utils.getRand());
    }

    /**
     * Create new IdemixSigningIdentity object with an existing pseudonym
     * and an existing proof
     *
     * @param ipk       issuer public key
     * @param mspId     MSP identifier
     * @param sk        user's secret
     * @param cred      idemix credential
     * @param pseudonym pseudonym
     * @param proof     proof
     * @throws CryptoException
     */
    public IdemixSigningIdentity(IssuerPublicKey ipk, String mspId, BIG sk, Credential cred, Pseudonym pseudonym, Signature proof) throws CryptoException {
        this(ipk, mspId, sk, cred, pseudonym, proof, Utils.getRand());
    }

    /**
     * Constructor in which we pass a seeded RNG. This allows tests to pass on a single RNG,
     * which prevents a test server from running out of entropy while running the unit tests.
     */
    public IdemixSigningIdentity(IssuerPublicKey ipk, String mspId, BIG sk, Credential cred, Pseudonym pseudonym, Signature proof, RAND rng) throws CryptoException {
        // input checks
        if ((ipk == null) || (mspId == null) || (mspId.isEmpty()) || (sk == null) || (cred == null) || (pseudonym == null && proof != null) || (proof == null && pseudonym != null)) {
            throw new CryptoException("Input must not be null");
        }

        if (!ipk.check()) {
            throw new CryptoException("Issuer public key is not valid");
        }

        this.ipk = ipk;
        this.sk = sk;
        this.mspId = mspId;
        this.cred = cred;

        // attribute checks
        if (this.cred.getAttrs().length != 2) {
            throw new CryptoException("The number of attributes is wrong");
        }

        this.ouBytes = this.cred.getAttrs()[0];
        this.roleBytes = this.cred.getAttrs()[1];

        this.attributes = new BIG[2];
        this.attributes[0] = BIG.fromBytes(this.ouBytes);
        this.attributes[1] = BIG.fromBytes(this.roleBytes);

        // cryptographically verify credential
        // (check is the issuer's signature is valid)
        if (!this.cred.ver(sk, ipk)) {
            throw new CryptoException("Credential is not cryptographically valid");
        }

        this.rng = rng;

        // if no pseudonym and proof are present, generate new ones
        if (pseudonym == null && proof == null) {
            // generate a fresh pseudonym
            this.pseudonym = new Pseudonym(this.sk, this.ipk, rng);

            // generate a fresh proof of possession of a credential
            // with respect to a freshly generated pseudonym
            this.proof = new Signature(this.cred, this.sk, this.pseudonym.getNym(), this.pseudonym.getRandNym(), this.ipk, this.disclosedFlags, this.msg, this.rng);
        } else {
            this.pseudonym = pseudonym;
            this.proof = proof;
        }


        // verify the proof
        if (!this.proof.ver(this.disclosedFlags, this.ipk, this.msg, this.attributes)) {
            throw new CryptoException("Generated proof of identity is not valid");
        }

        // generate a fresh identity with new pseudonym
        this.idemixIdentity = new IdemixIdentity(this.mspId, this.pseudonym.getNym(), this.ouBytes, this.roleBytes, this.proof);
    }

    @Override
    public byte[] sign(byte[] msg) throws CryptoException {
        if (msg == null) {
            throw new CryptoException("Input must not be null");
        }
        return new NymSignature(sk, pseudonym.getNym(), pseudonym.getRandNym(), ipk, msg, rng).toProto().toByteArray();
    }

    @Override
    public SerializedIdentity createSerializedIdentity() {
        return this.idemixIdentity.createSerializedIdentity();
    }

    @Override
    public boolean verifySignature(byte[] msg, byte[] sig) throws CryptoException {

        if ((msg == null) || (sig == null)) {
            throw new CryptoException("Input must not be null");
        }

        Idemix.NymSignature nymSigProto = null;
        try {
            nymSigProto = Idemix.NymSignature.parseFrom(sig);
        } catch (InvalidProtocolBufferException e) {
            throw new CryptoException("Could not parse Idemix Nym Signature", e);
        }

        NymSignature nymSig = new NymSignature(nymSigProto);
        return nymSig.ver(pseudonym.getNym(), ipk, msg);
    }

    public Pseudonym getNym() {
        return this.pseudonym;
    }

    public Signature getProof() {
        return this.proof;
    }

}
