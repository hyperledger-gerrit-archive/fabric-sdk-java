/*
 *
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 * /
 */

package org.hyperledger.fabric.sdk;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.protos.peer.FabricProposalResponse;
import org.hyperledger.fabric.protos.peer.lifecycle.Lifecycle;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.transaction.TransactionContext;

import static java.lang.String.format;

public final class LifecycleQueryChaincodeDefinitionsProposalResponse extends ProposalResponse {
    LifecycleQueryChaincodeDefinitionsProposalResponse(TransactionContext transactionContext, int status, String message) {
        super(transactionContext, status, message);
    }

    private Lifecycle.QueryChaincodeDefinitionsResult queryChaincodeDefinitionsResult;

    public Lifecycle.QueryChaincodeDefinitionsResult getChaincodeDefinitions() throws ProposalException {
        if (null == queryChaincodeDefinitionsResult) {
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
                queryChaincodeDefinitionsResult = Lifecycle.QueryChaincodeDefinitionsResult.parseFrom(payload);
            } catch (Exception e) {
                throw new ProposalException(format("Failure on peer %s %s", getPeer(), e.getMessage()), e);
            }
        }

        return queryChaincodeDefinitionsResult;
    }

    /**
     * The names of chaincode that have been committed.
     *
     * @return The names of chaincode that have been committed.
     * @throws ProposalException
     */
    public Collection<String> getChaincodeNames() throws ProposalException {
        final Lifecycle.QueryChaincodeDefinitionsResult queryChaincodeDefinitionsResult = getChaincodeDefinitions();
        if (queryChaincodeDefinitionsResult == null) {
            return Collections.emptySet();
        }

        final List<Lifecycle.QueryChaincodeDefinitionsResult.ChaincodeDefinition> chaincodeDefinitions = queryChaincodeDefinitionsResult.getChaincodeDefinitionsList();
        if (chaincodeDefinitions == null) {
            return Collections.emptyList();
        }

        return chaincodeDefinitions.stream()
                .map(Lifecycle.QueryChaincodeDefinitionsResult.ChaincodeDefinition::getName)
                .collect(Collectors.toList());
    }
}
