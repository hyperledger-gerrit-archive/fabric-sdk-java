/*
 *
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 * /
 */

package org.hyperledger.fabric.sdk;

/**
 * LifecycleInstallChaincodeRequest parameters for installing chaincode with lifecycle
 */
public class LifecycleQueryInstalledChaincodesRequest extends LifecycleRequest {

    LifecycleQueryInstalledChaincodesRequest(User userContext) {
        super(userContext, false);
    }
}
