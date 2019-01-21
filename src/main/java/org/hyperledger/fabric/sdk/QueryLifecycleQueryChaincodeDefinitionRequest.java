/*
 *
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 * /
 */

package org.hyperledger.fabric.sdk;

import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.helper.Utils;

/**
 * The QueryLifecycleQueryChaincodeDefinitionRequest returns for a specific chaincode name it's
 * latest sequence change, version, collections and if init is required.
 * See {@link LifecycleQueryChaincodeDefinitionProposalResponse}
 */
public class QueryLifecycleQueryChaincodeDefinitionRequest extends LifecycleRequest {
    public String getName() {
        return name;
    }

    private String name;

    QueryLifecycleQueryChaincodeDefinitionRequest(User userContext) {
        super(userContext);
    }

    public void setName(String name) throws InvalidArgumentException {
        if (Utils.isNullOrEmpty(name)) {
            throw new InvalidArgumentException("The chaincodeName parameter can not be null or empty.");
        }

        this.name = name;
    }
}
