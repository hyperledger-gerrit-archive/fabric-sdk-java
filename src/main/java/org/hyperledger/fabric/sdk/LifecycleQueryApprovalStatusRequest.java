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

public class LifecycleQueryApprovalStatusRequest extends LifecycleRequest {

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
    private LifecycleChaincodeEndorsementPolicy lifecycleChaincodeEndorsementPolicy;
    private Collection.CollectionConfigPackage collectionConfigPackage;
    private Boolean initRequired;

    LifecycleQueryApprovalStatusRequest(User userContext) {
        super(userContext);
        if (!Utils.isNullOrEmpty(config.getDefaultChaincodeEndorsementPlugin())) {

            endorsementPlugin = config.getDefaultChaincodeEndorsementPlugin();
        }

        if (!Utils.isNullOrEmpty(config.getDefaultChaincodeValidationPlugin())) {

            validationPlugin = config.getDefaultChaincodeValidationPlugin();
        }

        initRequired = lifecycleInitRequiredDefault;
    }

    public ByteString getValidationParameter() {
        return validationParameter;
    }

    public void setValidationParameter(ByteString validationParameter) {
        this.validationParameter = validationParameter;
    }

    private ByteString validationParameter;

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

    public void setChaincodeEndorsementPolicy(LifecycleChaincodeEndorsementPolicy lifecycleChaincodeEndorsementPolicy) {
        this.validationParameter = lifecycleChaincodeEndorsementPolicy.getByteString();
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

    public void setChaincodeCollectionConfiguration(ChaincodeCollectionConfiguration collectionConfigPackage) {

        this.collectionConfigPackage = collectionConfigPackage.getCollectionConfigPackage();
    }

}
