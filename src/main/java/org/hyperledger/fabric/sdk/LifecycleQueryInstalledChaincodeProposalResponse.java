/*
 *
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 * /
 */

package org.hyperledger.fabric.sdk;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.protos.peer.FabricProposalResponse;
import org.hyperledger.fabric.protos.peer.lifecycle.Lifecycle;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.transaction.TransactionContext;

import static java.lang.String.format;

public class LifecycleQueryInstalledChaincodeProposalResponse extends ProposalResponse {
    public LifecycleQueryInstalledChaincodeProposalResponse(TransactionContext transactionContext, int status, String message) {
        super(transactionContext, status, message);
    }

    Lifecycle.QueryInstalledChaincodeResult queryChaincodeDefinitionResult;

    private Lifecycle.QueryInstalledChaincodeResult parsePayload() throws ProposalException {

        if (null == queryChaincodeDefinitionResult) {

            if (getStatus() != Status.SUCCESS) {
                throw new ProposalException(format("Fabric response failed on peer %s  %s", getPeer(), getMessage()));
            }

            FabricProposalResponse.ProposalResponse fabricResponse = getProposalResponse();

            //   getChaincodeActionResponsePayload()

            if (null == fabricResponse) {
                throw new ProposalException(format("Proposal has no Fabric response. %s", getPeer()));
            }

            ByteString payload = fabricResponse.getResponse().getPayload();

            if (payload == null) {
                throw new ProposalException(format("Fabric response has no payload  %s", getPeer()));
            }

            try {
                queryChaincodeDefinitionResult = Lifecycle.QueryInstalledChaincodeResult.parseFrom(payload);
            } catch (Exception e) {
                throw new ProposalException(format("Failure on peer %s %s", getPeer(), e.getMessage()), e);
            }
        }

        return queryChaincodeDefinitionResult;
    }

    public byte[] getChaincodeHash() throws ProposalException {

        Lifecycle.QueryInstalledChaincodeResult queryInstalledChaincodesResult = parsePayload();

        Lifecycle.QueryInstalledChaincodeResult queryInstalledChaincodeResult = parsePayload();
        if (queryInstalledChaincodeResult == null) {
            return null;
        }
        ByteString hash = queryInstalledChaincodeResult.getHash();
        if (null == hash) {
            return null;
        }
        return hash.toByteArray();

    }

}
