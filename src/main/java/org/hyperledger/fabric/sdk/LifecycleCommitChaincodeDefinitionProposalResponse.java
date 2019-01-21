/*
 *
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 * /
 */

package org.hyperledger.fabric.sdk;

import org.hyperledger.fabric.sdk.transaction.TransactionContext;

public class LifecycleCommitChaincodeDefinitionProposalResponse extends ProposalResponse {
    public LifecycleCommitChaincodeDefinitionProposalResponse(TransactionContext transactionContext, int status, String message) {
        super(transactionContext, status, message);
    }
}
