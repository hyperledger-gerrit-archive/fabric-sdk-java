/*
 *
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 * /
 */

package org.hyperledger.fabric.sdk;

public class LifecycleQueryInstalledChaincode extends LifecycleRequest {
    private String packageId;

    LifecycleQueryInstalledChaincode(User userContext) {
        super(userContext, false);
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageID(String packageId) {

        this.packageId = packageId;
    }
}
