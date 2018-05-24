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
import org.hyperledger.fabric.protos.idemix.Idemix;

/**
 * IdemixCredRequest represents the first message of the idemix issuance protocol,
 * in which the user requests a credential from the issuer.
 */
public class IdemixCredRequest {
    private final ECP nym;
    private final BIG issuerNonce;
    private final BIG proofC;
    private final BIG proofS;

    private static final String CREDREQUEST_LABEL = "credRequest";


    /**
     * Constructor
     *
     * @param sk          the secret key of the user
     * @param issuerNonce a nonce
     * @param ipk         the issuer public key
     */
    public IdemixCredRequest(BIG sk, BIG issuerNonce, IdemixIssuerPublicKey ipk) {
        if (sk == null || issuerNonce == null || ipk == null) {
            throw new IllegalArgumentException("Cannot create idemix credrequest from null input");
        }
        nym = ipk.getHsk().mul(sk);
        this.issuerNonce = new BIG(issuerNonce);

        // Create Zero Knowledge Proof
        BIG rsk = IdemixUtils.randModOrder();
        ECP t = ipk.getHsk().mul(rsk);

        // Make proofData: total 3 elements of G1, each 2*FIELD_BYTES+1 (ECP),
        // plus length of String array,
        // plus one BIG
        byte[] proofData = new byte[CREDREQUEST_LABEL.getBytes().length + 3 * (2 * IdemixUtils.FIELD_BYTES + 1) + 2 * IdemixUtils.FIELD_BYTES];

        int index = 0;
        index = IdemixUtils.append(proofData, index, CREDREQUEST_LABEL.getBytes());
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(t));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(ipk.getHsk()));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(nym));
        index = IdemixUtils.append(proofData, index, IdemixUtils.bigToBytes(issuerNonce));
        IdemixUtils.append(proofData, index, ipk.getHash());

        proofC = IdemixUtils.hashModOrder(proofData);

        // Compute proofS = ...
        proofS = BIG.modmul(proofC, sk, IdemixUtils.GROUP_ORDER).plus(rsk);
        proofS.mod(IdemixUtils.GROUP_ORDER);
    }

    /**
     * Construct a IdemixCredRequest from a serialized credrequest
     *
     * @param proto a protobuf representation of a credential request
     * @return an IdemixCredRequest created from the proto
     */
    public IdemixCredRequest(Idemix.CredRequest proto) {
        if (proto == null) {
            throw new IllegalArgumentException("Cannot create idemix credrequest from null input");
        }
        nym = IdemixUtils.transformFromProto(proto.getNym());
        proofC = BIG.fromBytes(proto.getProofC().toByteArray());
        proofS = BIG.fromBytes(proto.getProofS().toByteArray());
        issuerNonce = BIG.fromBytes(proto.getIssuerNonce().toByteArray());
    }

    /**
     * @return a pseudonym of the credential requester
     */
    public ECP getNym() {
        return nym;
    }

    /**
     * @return a proto version of this IdemixCredRequest
     */
    public Idemix.CredRequest toProto() {
        Idemix.CredRequest proto = Idemix.CredRequest.newBuilder()
                .setNym(IdemixUtils.transformToProto(nym))
                .setProofC(ByteString.copyFrom(IdemixUtils.bigToBytes(proofC)))
                .setProofS(ByteString.copyFrom(IdemixUtils.bigToBytes(proofS)))
                .setIssuerNonce(ByteString.copyFrom(IdemixUtils.bigToBytes(issuerNonce)))
                .build();

        return proto;
    }


    /**
     * Cryptographically verify the IdemixCredRequest
     *
     * @param ipk the issuer public key
     * @return true iff valid
     */
    public boolean check(IdemixIssuerPublicKey ipk) {

        if (nym == null ||
                issuerNonce == null ||
                proofC == null ||
                proofS == null ||
                ipk == null) {
            return false;
        }

        ECP t = ipk.getHsk().mul(proofS);
        t.sub(nym.mul(proofC));

        byte[] proofData = new byte[CREDREQUEST_LABEL.getBytes().length + 3 * (2 * IdemixUtils.FIELD_BYTES + 1) + 2 * IdemixUtils.FIELD_BYTES];

        int index = 0;
        index = IdemixUtils.append(proofData, index, CREDREQUEST_LABEL.getBytes());
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(t));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(ipk.getHsk()));
        index = IdemixUtils.append(proofData, index, IdemixUtils.ecpToBytes(nym));
        index = IdemixUtils.append(proofData, index, IdemixUtils.bigToBytes(issuerNonce));
        IdemixUtils.append(proofData, index, ipk.getHash());

        // Hash proofData to hproofdata
        byte[] hproofdata = IdemixUtils.bigToBytes(IdemixUtils.hashModOrder(proofData));

        return Arrays.equals(IdemixUtils.bigToBytes(proofC), hproofdata);
    }
}

