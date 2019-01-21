/*
 *
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 * /
 */

package org.hyperledger.fabric.sdk;

/**
 * LifecycleQueryNamespaceDefinitionsRequest parameters for installing chaincode with lifecycle
 */
public class LifecycleQueryNamespaceDefinitionsRequest extends LifecycleRequest {

    private LifecycleChaincodePackage lifecycleChaincodePackage;

    LifecycleQueryNamespaceDefinitionsRequest(User userContext) {
        super(userContext, false);
    }
}
