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

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.milagro.amcl.FP256BN.BIG;
import org.apache.milagro.amcl.FP256BN.ECP;
import org.hyperledger.fabric.protos.idemix.Idemix;
import org.hyperledger.fabric.protos.msp.Identities;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.idemix.IdemixIssuerPublicKey;
import org.hyperledger.fabric.sdk.idemix.IdemixPseudonymSignature;
import org.hyperledger.fabric.sdk.idemix.IdemixSignature;
import org.hyperledger.fabric.sdk.idemix.IdemixUtils;

/**
 * IdemixIdentity is a public serializable part of the IdemixSigningIdentity.
 * It contains an (un)linkable pseudonym, revealed attribute values, and a
 * corresponding proof of possession of an Idemix credential
 */
public class IdemixIdentity implements Identity {

    private static final Log logger = LogFactory.getLog(IdemixIdentity.class);

    // MSP identifier
    private String mspId;

    // public key of the Idemix CA (issuer)
    private IdemixIssuerPublicKey ipk;

    // Idemix Pseudonym
    private ECP pseudonym;

    // Organization Unit attribute
    private String ou;

    // Role attribute
    private boolean role;

    // Proof of possession of Idemix credential
    // with respect to the pseudonym (nym)
    // and the corresponding attributes (ou, role)
    private IdemixSignature associationProof;

    /**
     * Create Idemix Identity from a Serialized Identity
     *
     * @param proto
     */
    public IdemixIdentity(Identities.SerializedIdentity proto) throws CryptoException, InvalidArgumentException {
        if (proto == null) {
            throw new InvalidArgumentException("Input must not be null");
        }

        this.mspId = proto.getMspid();

        Identities.SerializedIdemixIdentity idemixProto;
        try {
            logger.debug("Fetching Idemix Proto");
            idemixProto = Identities.SerializedIdemixIdentity.parseFrom(proto.getIdBytes());
        } catch (InvalidProtocolBufferException e) {
            throw new CryptoException("Cannot deserialize MSP ID", e);
        }
        if (idemixProto != null) {
            logger.debug("Deserializing Nym and attribute values");
            this.pseudonym = new ECP(BIG.fromBytes(idemixProto.getNymX().toByteArray()),
                    BIG.fromBytes(idemixProto.getNymY().toByteArray()));
            this.ou = idemixProto.getOU().toStringUtf8();
            this.role = Boolean.parseBoolean(idemixProto.getRole().toStringUtf8());
            try {
                logger.debug("Deserializing Proof");
                this.associationProof = new IdemixSignature(Idemix.Signature.parseFrom(idemixProto.getProof().toByteArray()));
            } catch (InvalidProtocolBufferException e) {
                throw new CryptoException("Cannot deserialize proof", e);
            }
        }
    }

    /**
     * Create Idemix Identity from the following inputs:
     *
     * @param mspId is MSP ID sting
     * @param nym   is Identity Mixer Pseudonym
     * @param ou    is OU attribute
     * @param role  is Role attribute
     * @param proof is Proof
     */
    public IdemixIdentity(String mspId, IdemixIssuerPublicKey ipk, ECP nym, String ou, boolean role, IdemixSignature proof)
            throws InvalidArgumentException {

        if ((mspId == null) || (mspId.isEmpty()) || (nym == null) || (ou == null) || (ou.isEmpty()) || (proof == null)) {
            throw new InvalidArgumentException("Input must not be null");
        }

        this.mspId = mspId;
        this.ipk = ipk;
        this.pseudonym = nym;
        this.ou = ou;
        this.role = role;
        this.associationProof = proof;
    }

    /**
     * Serialize Idemix Identity
     */
    @Override
    public Identities.SerializedIdentity createSerializedIdentity() {
        Identities.SerializedIdemixIdentity serializedIdemixIdentity = Identities.SerializedIdemixIdentity.newBuilder()
                .setProof(ByteString.copyFrom(this.associationProof.toProto().toByteArray()))
                .setOU(ByteString.copyFromUtf8(String.valueOf(this.ou)))
                .setRole(ByteString.copyFromUtf8(String.valueOf(this.role)))
                .setNymY(ByteString.copyFrom(IdemixUtils.bigToBytes(this.pseudonym.getY())))
                .setNymX(ByteString.copyFrom(IdemixUtils.bigToBytes(this.pseudonym.getX()))).build();
        return Identities.SerializedIdentity.newBuilder()
                .setIdBytes(ByteString.copyFrom(serializedIdemixIdentity.toByteArray())).setMspid(this.mspId).build();

    }

    public String getOuValue() {
        return this.ou;
    }

    public boolean getRoleValue() {
        return this.role;
    }

}
