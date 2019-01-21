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
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.helper.Config;

import static org.hyperledger.fabric.sdk.helper.Utils.isNullOrEmpty;

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
        if (!isNullOrEmpty(config.getDefaultChaincodeEndorsementPlugin())) {

            endorsementPlugin = config.getDefaultChaincodeEndorsementPlugin();
        }

        if (!isNullOrEmpty(config.getDefaultChaincodeValidationPlugin())) {

            validationPlugin = config.getDefaultChaincodeValidationPlugin();
        }

        initRequired = lifecycleInitRequiredDefault;
    }

    public ByteString getValidationParameter() {
        return validationParameter;
    }

    public void setValidationParameter(ByteString validationParameter) throws InvalidArgumentException {
        if (null == validationParameter) {
            throw new InvalidArgumentException(" The parameter validationParameter may not be null.");
        }
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

    public void setName(String name) throws InvalidArgumentException {
        if (isNullOrEmpty(name)) {
            throw new InvalidArgumentException("The name parameter can not be null or empty.");
        }
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) throws InvalidArgumentException {

        if (isNullOrEmpty(version)) {
            throw new InvalidArgumentException("The version parameter can not be null or empty.");
        }

        this.version = version;
    }

    public String getEndorsementPlugin() {
        return endorsementPlugin;
    }

    public void setEndorsementPlugin(String endorseendorsementPluginmentPlugin) throws InvalidArgumentException {

        if (isNullOrEmpty(endorsementPlugin)) {
            throw new InvalidArgumentException("The endorsementPlugin parameter can not be null or empty.");
        }

        this.endorsementPlugin = endorsementPlugin;
    }

    public String getValidationPlugin() {
        return validationPlugin;
    }

    public void setValidationPlugin(String validationPlugin) throws InvalidArgumentException {

        if (isNullOrEmpty(validationPlugin)) {
            throw new InvalidArgumentException("The validationPlugin parameter can not be null or empty.");
        }

        this.validationPlugin = validationPlugin;
    }

    public void setChaincodeEndorsementPolicy(LifecycleChaincodeEndorsementPolicy lifecycleChaincodeEndorsementPolicy) throws InvalidArgumentException {
        if (null == lifecycleChaincodeEndorsementPolicy) {
            throw new InvalidArgumentException(" The parameter lifecycleChaincodeEndorsementPolicy may not be null.");
        }
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

    public void setChaincodeCollectionConfiguration(ChaincodeCollectionConfiguration collectionConfigPackage) throws InvalidArgumentException {
        if (null == collectionConfigPackage) {
            throw new InvalidArgumentException(" The parameter collectionConfigPackage may not be null.");
        }

        this.collectionConfigPackage = collectionConfigPackage.getCollectionConfigPackage();
    }

}
