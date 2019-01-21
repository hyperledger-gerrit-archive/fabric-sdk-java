/*
 *
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 * /
 */

package org.hyperledger.fabric.sdk;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.protos.common.Collection;
import org.hyperledger.fabric.protos.peer.FabricProposalResponse;
import org.hyperledger.fabric.protos.peer.lifecycle.Lifecycle;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.transaction.TransactionContext;

import static java.lang.String.format;

public class LifecycleQueryChaincodeDefinitionProposalResponse extends ProposalResponse {
    private Lifecycle.QueryChaincodeDefinitionResult queryChaincodeDefinitionResult = null;

    public LifecycleQueryChaincodeDefinitionProposalResponse(TransactionContext transactionContext, int status, String message) {
        super(transactionContext, status, message);
    }

    private Lifecycle.QueryChaincodeDefinitionResult parsePayload() throws ProposalException {

        if (null == queryChaincodeDefinitionResult) {

            if (getStatus() != Status.SUCCESS) {
                throw new ProposalException(format("Fabric response failed on peer %s  %s", getPeer(), getMessage()));
            }

            FabricProposalResponse.ProposalResponse fabricResponse = getProposalResponse();

            //   getChaincodeActionResponsePayload()

            if (null == fabricResponse) {
                throw new ProposalException("Proposal has no Fabric response.");
            }

            ByteString payload = fabricResponse.getPayload();

            if (payload == null) {
                throw new ProposalException("Fabric response has no payload");
            }

            try {
                byte[] chaincodeActionResponsePayload = getChaincodeActionResponsePayload();
                if (null == chaincodeActionResponsePayload) {
                    throw new ProposalException("Fabric chaincode action response payload is null.");
                }
                queryChaincodeDefinitionResult = Lifecycle.QueryChaincodeDefinitionResult.parseFrom(getChaincodeActionResponsePayload());
            } catch (Exception e) {
                throw new ProposalException(format("Failure on peer %s %s", getPeer(), e.getMessage()), e);
            }

        }

        return queryChaincodeDefinitionResult;
    }

    public byte[] getHash() throws ProposalException {

        ByteString hash = parsePayload().getHash();
        if (null == hash) {
            return null;
        }
        return hash.toByteArray();
    }

    public ChaincodeEndorsementPolicy getChaincodeEndorsementPolicy() throws ProposalException {

        ByteString payloadBytes = parsePayload().getValidationParameter();
        if (null == payloadBytes) {
            return null;
        }
        return ChaincodeEndorsementPolicy.fromBytes(payloadBytes.toByteArray());
    }

    public String getVersion() throws ProposalException {
        return parsePayload().getVersion();
    }

    public boolean getInitRequired() throws ProposalException {
        return parsePayload().getInitRequired();
    }

    public long getSequence() throws ProposalException {
        return parsePayload().getSequence();
    }

    public CollectionConfigPackage getCollections() throws ProposalException {
        Collection.CollectionConfigPackage collections = parsePayload().getCollections();

        if (null == collections || !parsePayload().hasCollections()) {
            return null;
        }
        return new CollectionConfigPackage(collections.toByteString());
    }

    public String getEndorsementPlugin() throws ProposalException {
        return parsePayload().getEndorsementPlugin();
    }

    public String getValidationPlugin() throws ProposalException {
        return parsePayload().getValidationPlugin();
    }
}
