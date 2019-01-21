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

public class LifecycleQueryNamespaceDefinitionsProposalResponse extends ProposalResponse {
    public LifecycleQueryNamespaceDefinitionsProposalResponse(TransactionContext transactionContext, int status, String message) {
        super(transactionContext, status, message);
    }

    Lifecycle.QueryNamespaceDefinitionsResult queryNamespaceDefinitionsResult;

    private Lifecycle.QueryNamespaceDefinitionsResult parsePayload() throws ProposalException {

        if (null == queryNamespaceDefinitionsResult) {

            if (getStatus() != Status.SUCCESS) {
                throw new ProposalException(format("Fabric response failed on peer %s  %s", getPeer(), getMessage()));
            }

            FabricProposalResponse.ProposalResponse fabricResponse = getProposalResponse();

            if (null == fabricResponse) {
                throw new ProposalException(format("Proposal has no Fabric response. %s", getPeer()));
            }

            ByteString payload = fabricResponse.getResponse().getPayload();

            if (payload == null) {
                throw new ProposalException(format("Fabric response has no payload  %s", getPeer()));
            }

            try {
                queryNamespaceDefinitionsResult = Lifecycle.QueryNamespaceDefinitionsResult.parseFrom(payload);
            } catch (Exception e) {
                throw new ProposalException(format("Failure on peer %s %s", getPeer(), e.getMessage()), e);
            }
        }

        return queryNamespaceDefinitionsResult;
    }

    public Lifecycle.QueryNamespaceDefinitionsResult getNamespaceDefinitions() throws ProposalException {

        return parsePayload();

    }

}
