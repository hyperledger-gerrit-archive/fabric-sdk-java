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
import org.hyperledger.fabric.sdk.helper.Config;
import org.hyperledger.fabric.sdk.helper.Utils;

public class LifecycleQueryApprovalStatusRequest {

    static Config config = Config.getConfig();
    static Boolean lifecycleInitRequiredDefault = null;

    static {
        lifecycleInitRequiredDefault = config.getLifecycleInitRequiredDefault();
    }

    private long sequence;
    private String name;
    private String version;
    private String endorsementPlugin;
    private String validationPlugin;
    private ByteString validationParameter;
    private Collection.CollectionConfigPackage collectionConfigPackage;
    private Boolean initRequired;

    LifecycleQueryApprovalStatusRequest() {
        if (!Utils.isNullOrEmpty(config.getDefaultChaincodeEndorsementPlugin())) {

            endorsementPlugin = config.getDefaultChaincodeEndorsementPlugin();
        }

        if (!Utils.isNullOrEmpty(config.getDefaultChaincodeValidationPlugin())) {

            validationPlugin = config.getDefaultChaincodeValidationPlugin();
        }

        initRequired = lifecycleInitRequiredDefault;

    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {

        this.sequence = sequence;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {

        this.version = version;
    }

    public String getEndorsementPlugin() {
        return endorsementPlugin;
    }

    public void setEndorsementPlugin(String endorsement) {

        this.endorsementPlugin = endorsement;
    }

    public String getValidationPlugin() {
        return validationPlugin;
    }

    public void setValidationPlugin(String validationPlugin) {

        this.validationPlugin = validationPlugin;
    }

    public ByteString getValidationParameter() {
        return validationParameter;
    }

    public void setChaincodeEndorsementPolicy(ChaincodeEndorsementPolicy validationParameter) {
        this.validationParameter = ByteString.copyFrom(validationParameter.getChaincodeEndorsementPolicyAsBytes());
    }

    public Collection.CollectionConfigPackage getCollectionConfigPackage() {
        return collectionConfigPackage;
    }

    public Boolean getInitRequired() {
        return initRequired;
    }

    public void setInitRequired(boolean initRequired) {

        this.initRequired = initRequired;
    }

    public void setCollections(Collection.CollectionConfigPackage collectionConfigPackage) {

        this.collectionConfigPackage = collectionConfigPackage;
    }

}
