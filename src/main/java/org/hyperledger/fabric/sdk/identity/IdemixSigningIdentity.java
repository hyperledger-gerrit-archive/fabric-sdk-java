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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.milagro.amcl.FP256BN.BIG;
import org.hyperledger.fabric.protos.idemix.Idemix;
import org.hyperledger.fabric.protos.msp.Identities.SerializedIdentity;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.idemix.IdemixCredential;
import org.hyperledger.fabric.sdk.idemix.IdemixIssuerPublicKey;
import org.hyperledger.fabric.sdk.idemix.IdemixPseudonym;
import org.hyperledger.fabric.sdk.idemix.IdemixPseudonymSignature;
import org.hyperledger.fabric.sdk.idemix.IdemixSignature;

/**
 * IdemixSigningIdentity is an Idemix implementation of the SigningIdentity It
 * contains IdemixIdentity (a public part) and a corresponding secret part that
 * contains the user secret key and the commitment opening (randomness) to the
 * pseudonym value (a commitment to the user secret)
 *
 * We note that since the attributes and their disclosure is fixed we are not
 * adding them as fields here.
 */
public class IdemixSigningIdentity implements SigningIdentity {

    // public part of the signing identity (passed with the signature)
    private IdemixIdentity idemixIdentity;

    // public key of the Idemix CA (issuer)
    private IdemixIssuerPublicKey ipk;

    // user's secret
    private BIG sk;

    // idemix pseudonym (represents Idemix identity)
    private IdemixPseudonym pseudonym;

    // proof that the identity is valid (proof of possession of a credential
    // with respect to a pseudonym.
    private IdemixSignature proof;

    // discloseFlags will be passed to the idemix signing and verification
    // routines.
    // It informs idemix to disclose both attributes (OU and Role) when signing.
    private static final boolean[] disclosedFlags = new boolean[] {true, true, false, false};

    // empty message to sign in the validate identity proof
    private static final byte[] msgEmpty = {};

    private static final Log logger = LogFactory.getLog(IdemixSigningIdentity.class);

    /**
     * Create new Idemix Signing Identity with a fresh pseudonym
     *
     * @param ipk
     *            issuer public key
     * @param mspId
     *            MSP identifier
     * @param sk
     *            user's secret
     * @param cred
     *            idemix credential
     * @throws CryptoException
     */
    public IdemixSigningIdentity(IdemixIssuerPublicKey ipk, String mspId, BIG sk, IdemixCredential cred)
            throws CryptoException, InvalidArgumentException {
        this(ipk, mspId, sk, cred, null, null);
    }

    /**
     * Create new IdemixSigningIdentity object with an existing pseudonym and an
     * existing proof
     *
     * @param ipk
     *            issuer public key
     * @param mspId
     *            MSP identifier
     * @param sk
     *            user's secret
     * @param cred
     *            idemix credential
     * @param pseudonym
     *            pseudonym
     * @param proof
     *            proof
     * @throws CryptoException
     * @throws InvalidArgumentException
     */
    public IdemixSigningIdentity(IdemixIssuerPublicKey ipk, String mspId, BIG sk, IdemixCredential cred,
                                 IdemixPseudonym pseudonym, IdemixSignature proof) throws CryptoException, InvalidArgumentException {
        // input checks
        if ((ipk == null) || (mspId == null) || (mspId.isEmpty()) || (sk == null) || (cred == null)
                || (pseudonym == null && proof != null) || (proof == null && pseudonym != null)) {
            throw new InvalidArgumentException("Input must not be null");
        }

        logger.debug("Verifying public key");

        if (!ipk.check()) {
            throw new CryptoException("Issuer public key is not valid");
        }

        this.ipk = ipk;
        this.sk = sk;

        logger.debug("Checking attributes");

        // attribute checks
        // 4 attributes are expected:
        // - organization unit (disclosed)
        // - role: admin or member (diclosed)
        // - enrollment id (hidden, for future auditing feature and authorization with CA)
        // - revocation handle (hidden, for future revocation support)
        if (cred.getAttrs().length != 4) {
            throw new CryptoException("The number of attributes is wrong");
        }

        byte[] ouBytes = cred.getAttrs()[0];
        byte[] roleBytes = cred.getAttrs()[1];
        byte[] eIdBytes = cred.getAttrs()[2];
        byte[] rHBytes = cred.getAttrs()[3];

        BIG[] attributes = new BIG[4];
        attributes[0] = BIG.fromBytes(ouBytes);
        attributes[1] = BIG.fromBytes(roleBytes);
        attributes[2] = BIG.fromBytes(eIdBytes);
        attributes[3] = BIG.fromBytes(rHBytes);

        // cryptographically verify credential
        // (check is the issuer's signature is valid)
        if (!cred.verify(sk, ipk)) {
            throw new CryptoException("Credential is not cryptographically valid");
        }

        // if no pseudonym and proof are present, generate new ones
        if (pseudonym == null) {
            logger.debug("Generating fresh pseudonym and proof");
            // generate a fresh pseudonym
            this.pseudonym = new IdemixPseudonym(this.sk, this.ipk);

            // generate a fresh proof of possession of a credential
            // with respect to a freshly generated pseudonym
            this.proof = new IdemixSignature(cred, this.sk, this.pseudonym, this.ipk, IdemixSigningIdentity.disclosedFlags,
                    IdemixSigningIdentity.msgEmpty);
        } else {
            this.pseudonym = pseudonym;
            this.proof = proof;
        }

        logger.debug("Verifying the  proof");
        // verify the proof
        if (!this.proof.verify(IdemixSigningIdentity.disclosedFlags, this.ipk, IdemixSigningIdentity.msgEmpty, attributes)) {
            throw new CryptoException("Generated proof of identity is not valid");
        }

        logger.debug("Generating the Identity Object");
        // generate a fresh identity with new pseudonym
        this.idemixIdentity = new IdemixIdentity(mspId, this.pseudonym.getNym(), ouBytes, roleBytes,
                this.proof);
    }

    @Override
    public byte[] sign(byte[] msg) throws CryptoException, InvalidArgumentException {
        if (msg == null) {
            throw new InvalidArgumentException("Input must not be null");
        }
        return new IdemixPseudonymSignature(sk, pseudonym, ipk, msg).toProto().toByteArray();
    }

    @Override
    public SerializedIdentity createSerializedIdentity() {
        return this.idemixIdentity.createSerializedIdentity();
    }

    @Override
    public boolean verifySignature(byte[] msg, byte[] sig) throws CryptoException, InvalidArgumentException {

        if ((msg == null) || (sig == null)) {
            throw new InvalidArgumentException("Input must not be null");
        }

        Idemix.NymSignature nymSigProto = null;
        try {
            nymSigProto = Idemix.NymSignature.parseFrom(sig);
        } catch (InvalidProtocolBufferException e) {
            throw new CryptoException("Could not parse Idemix Nym Signature", e);
        }

        IdemixPseudonymSignature nymSig = new IdemixPseudonymSignature(nymSigProto);
        return nymSig.verify(pseudonym.getNym(), ipk, msg);
    }

    public IdemixPseudonym getNym() {
        return this.pseudonym;
    }

    public IdemixSignature getProof() {
        return this.proof;
    }

}