/*
 *
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 * /
 */

package org.hyperledger.fabric.sdk;

public class QueryLifecycleQueryChaincodeDefinitionRequest extends LifecycleRequest {
    public String getName() {
        return name;
    }

    private String name;

    QueryLifecycleQueryChaincodeDefinitionRequest(User userContext) {
        super(userContext);
    }

    public void setName(String name) {
        this.name = name;
    }
}
