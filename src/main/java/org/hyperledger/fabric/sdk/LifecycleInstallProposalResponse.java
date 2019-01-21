/*
 *
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 * /
 */

package org.hyperledger.fabric.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.hyperledger.fabric.protos.peer.lifecycle.Lifecycle;
import org.hyperledger.fabric.sdk.transaction.TransactionContext;

public class LifecycleInstallProposalResponse extends ProposalResponse {
    public LifecycleInstallProposalResponse(TransactionContext transactionContext, int status, String message) {
        super(transactionContext, status, message);
    }

    public byte[] getChaincodeHash() throws InvalidProtocolBufferException {
        ByteString payload = getProposalResponse().getResponse().getPayload();
        Lifecycle.InstallChaincodeResult installChaincodeResult = Lifecycle.InstallChaincodeResult.parseFrom(payload);
        return installChaincodeResult.getHash().toByteArray();
    }
}
