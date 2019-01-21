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

public class LifecycleQueryInstalledChaincodeRequest extends LifecycleRequest {
    private String packageId;

    LifecycleQueryInstalledChaincodeRequest(User userContext) {
        super(userContext, false);
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageID(String packageId) throws InvalidArgumentException {

        if (Utils.isNullOrEmpty(packageId)) {
            throw new InvalidArgumentException("The packageId parameter can not be null or empty.");
        }
        this.packageId = packageId;
    }
}
