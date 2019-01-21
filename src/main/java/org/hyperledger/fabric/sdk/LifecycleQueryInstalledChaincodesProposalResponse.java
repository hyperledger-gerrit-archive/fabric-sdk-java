/*
 *
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 * /
 */

package org.hyperledger.fabric.sdk;

import java.util.ArrayList;
import java.util.Collection;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.protos.peer.FabricProposalResponse;
import org.hyperledger.fabric.protos.peer.lifecycle.Lifecycle;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.transaction.TransactionContext;

import static java.lang.String.format;

public class LifecycleQueryInstalledChaincodesProposalResponse extends ProposalResponse {
    public LifecycleQueryInstalledChaincodesProposalResponse(TransactionContext transactionContext, int status, String message) {
        super(transactionContext, status, message);
    }

    Lifecycle.QueryInstalledChaincodesResult queryChaincodeDefinitionResult;

    private Lifecycle.QueryInstalledChaincodesResult parsePayload() throws ProposalException {

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
//                byte[] chaincodeActionResponsePayload = getChaincodeActionResponsePayload();
//                if (null == chaincodeActionResponsePayload) {
//                    throw new ProposalException("Fabric chaincode action response payload is null.");
//                }
                queryChaincodeDefinitionResult = Lifecycle.QueryInstalledChaincodesResult.parseFrom(payload);
            } catch (Exception e) {
                throw new ProposalException(format("Failure on peer %s %s", getPeer(), e.getMessage()), e);
            }
        }

        return queryChaincodeDefinitionResult;
    }

    public Collection<LifecycleQueryInstalledChaincodesResult> getLifecycleQueryInstalledChaincodesResult() throws ProposalException {

        Lifecycle.QueryInstalledChaincodesResult queryInstalledChaincodesResult = parsePayload();

        Collection<LifecycleQueryInstalledChaincodesResult> ret = new ArrayList<>(queryInstalledChaincodesResult.getInstalledChaincodesCount());
        for (Lifecycle.QueryInstalledChaincodesResult.InstalledChaincode qr : queryInstalledChaincodesResult.getInstalledChaincodesList()) {

            ret.add(new LifecycleQueryInstalledChaincodesResult(qr));
        }
        return ret;

    }

    public class LifecycleQueryInstalledChaincodesResult {

        LifecycleQueryInstalledChaincodesResult(Lifecycle.QueryInstalledChaincodesResult.InstalledChaincode installedChaincode) {

            this.installedChaincode = installedChaincode;
        }

        public String getName() {
            return installedChaincode.getName();
        }

        public String getVersion() {
            return installedChaincode.getVersion();
        }

        public byte[] getHash() {
            return installedChaincode.getHash().toByteArray();
        }

        private final Lifecycle.QueryInstalledChaincodesResult.InstalledChaincode installedChaincode;

    }
}
