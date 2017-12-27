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
import org.apache.milagro.amcl.FP256BN.BIG;
import org.apache.milagro.amcl.FP256BN.ECP;
import org.hyperledger.fabric.protos.idemix.Idemix;
import org.hyperledger.fabric.protos.msp.Identities;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.idemix.Signature;
import org.hyperledger.fabric.sdk.idemix.Utils;

/**
 * IdemixIdentity is a public serializable part of the IdemixSigningIdentity It
 * contains an (un)linkable pseudonym, revealed attribute values, and a
 * corresponding proof of possession of an Idemix credential
 */
public class IdemixIdentity implements Identity {

    // MSP identifier
    private String mspId;

    // Idemix Pseudonym
    private ECP nym;

    // Organization Unit attribute
    private byte[] ou;

    // Role attribute
    private byte[] role;

    // Proof of possession of Idemix credential
    // with respect to the pseudonym (nym)
    // and the corresponding attributes (ou, role)
    private Signature associationProof;

    /**
     * Create Idemix Identity from a Serialized Identity
     *
     * @param proto
     */
    public IdemixIdentity(Identities.SerializedIdentity proto) throws CryptoException {
        if (proto == null) {
            throw new CryptoException("Input must not be null");
        }

        this.mspId = proto.getMspid();

        Identities.SerializedIdemixIdentity idemixProto;
        try {
            idemixProto = Identities.SerializedIdemixIdentity.parseFrom(proto.getIdBytes());
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            throw new CryptoException("Cannot deserialize MSP ID", e);
        }
        if (idemixProto != null) {
            this.nym = new ECP(BIG.fromBytes(idemixProto.getNymX().toByteArray()),
                    BIG.fromBytes(idemixProto.getNymY().toByteArray()));
            this.ou = idemixProto.getOU().toByteArray();
            this.role = idemixProto.getRole().toByteArray();
            try {
                this.associationProof = new Signature(Idemix.Signature.parseFrom(idemixProto.getProof().toByteArray()));
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
                throw new CryptoException("Cannot deserialize proof", e);
            }
        }
    }

    /**
     * Create Idemix Identity from
     *
     * @param mspId is MSP ID sting
     * @param nym   is Identity Mixer Pseudonym
     * @param ou    is OU attribute
     * @param role  is Role attribute
     * @param proof is Proof
     */
    public IdemixIdentity(String mspId, ECP nym, byte[] ou, byte[] role, Signature proof) throws CryptoException {

        if ((mspId == null) || (mspId.isEmpty()) || (nym == null) || (ou == null) || (role == null) || (proof == null)) {
            throw new CryptoException("Input must not be null");
        }

        this.mspId = mspId;
        this.nym = nym;
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
                .setRole(ByteString.copyFrom(this.role)).setOU(ByteString.copyFrom(this.ou))
                .setNymY(ByteString.copyFrom(Utils.bigToBytes(this.nym.getY())))
                .setNymX(ByteString.copyFrom(Utils.bigToBytes(this.nym.getX()))).build();
        return Identities.SerializedIdentity.newBuilder()
                .setIdBytes(ByteString.copyFrom(serializedIdemixIdentity.toByteArray())).setMspid(this.mspId).build();

    }

}
