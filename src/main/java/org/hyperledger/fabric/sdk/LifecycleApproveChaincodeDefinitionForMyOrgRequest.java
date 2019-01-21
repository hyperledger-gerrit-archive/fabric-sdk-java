/*
 *
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 * /
 */

package org.hyperledger.fabric.sdk;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.helper.Config;
import org.hyperledger.fabric.sdk.helper.Utils;

/**
 * LifecycleInstallChaincodeRequest parameters for installing chaincode with lifecycle
 */
public class LifecycleApproveChaincodeDefinitionForMyOrgRequest extends LifecycleRequest {
    private static final Config config = Config.getConfig();
    protected String chaincodeName;
    // The version of the chaincode
    protected String chaincodeVersion;

    private String packageId;
    private boolean sourceNone = false; // there is no packageId source
    private long definitionSequence;
    private ChaincodeCollectionConfiguration chaincodeCollectionConfiguration;
    private String chaincodeCodeEndorsementPlugin = null;
    private Boolean initRequired = null;
    private String chaincodeCodeValidationPlugin = null;
    private ByteString validationParameter = null;

    LifecycleApproveChaincodeDefinitionForMyOrgRequest(User userContext) {
        super(userContext);
    }

    public boolean isSourceNone() {
        return sourceNone;
    }

    /**
     * There is no specific packageId for this approval.
     *
     * @param sourceNone
     * @throws InvalidArgumentException
     */
    public void setSourceNone(boolean sourceNone) throws InvalidArgumentException {
        if (packageId != null) {
            throw new InvalidArgumentException("Source none can not be set to true if packageId has been provided already");
        }
        this.sourceNone = sourceNone;
    }

    /**
     * The chaincode validation parameter. Only this or chancode endorsment policy may be set at one time.
     *
     * @param validationParameter
     * @throws InvalidArgumentException
     */
    public void setValidationParameter(byte[] validationParameter) throws InvalidArgumentException {
        if (null == validationParameter) {
            throw new InvalidArgumentException("The valdiationParameter parameter can not be null.");
        }
        this.validationParameter = ByteString.copyFrom(validationParameter);

    }

    /**
     * The chaincode endorsement policy. Only this or setValdationParamter maybe set at one time.
     *
     * @param lifecycleChaincodeEndorsementPolicy
     * @throws InvalidArgumentException
     */

    public void setChaincodeEndorsementPolicy(LifecycleChaincodeEndorsementPolicy lifecycleChaincodeEndorsementPolicy) throws InvalidArgumentException {
        if (null == lifecycleChaincodeEndorsementPolicy) {
            throw new InvalidArgumentException("The lifecycleChaincodeEndorsementPolicy parameter can not be null.");
        }
        this.validationParameter = lifecycleChaincodeEndorsementPolicy.getByteString();
    }

    public Boolean isInitRequired() {
        return initRequired;
    }

    public void setInitRequired(boolean initRequired) {
        this.initRequired = initRequired;
    }

    public String getChaincodeName() {
        return chaincodeName;
    }

    /**
     * @param chaincodeName
     */
    public void setChaincodeName(String chaincodeName) throws InvalidArgumentException {
        if (Utils.isNullOrEmpty(chaincodeName)) {
            throw new InvalidArgumentException("The chaincodeName parameter can not be null or empty.");
        }
        this.chaincodeName = chaincodeName;
    }

    public String getChaincodeVersion() {

        return chaincodeVersion;
    }

    /**
     * @param chaincodeVersion
     */

    public void setChaincodeVersion(String chaincodeVersion) throws InvalidArgumentException {
        if (Utils.isNullOrEmpty(chaincodeVersion)) {
            throw new InvalidArgumentException("The chaincodeVersion parameter can not be null or empty.");
        }
        this.chaincodeVersion = chaincodeVersion;

    }

    public long getDefinitionSequence() {
        return definitionSequence;
    }

    public void setDefinitionSequence(long definitionSequence) {
        this.definitionSequence = definitionSequence;
    }

    public String getPackageId() {

        return packageId;
    }

    public void setPackageId(String packageId) throws InvalidArgumentException {
        if (sourceNone) {
            throw new InvalidArgumentException("The source none has be set to true already. Can not have packageId set when source none set to true.");
        }
        if (Utils.isNullOrEmpty(packageId)) {
            throw new InvalidArgumentException("The packageId parameter can not be null or empty.");
        }

        this.packageId = packageId;

    }

    public ChaincodeCollectionConfiguration getChaincodeCollectionConfiguration() {
        return this.chaincodeCollectionConfiguration;
    }

    public void setChaincodeCollectionConfiguration(ChaincodeCollectionConfiguration chaincodeCollectionConfiguration) throws InvalidArgumentException {
        if (null == chaincodeCollectionConfiguration) {
            throw new InvalidArgumentException("The chaincodeCollectionConfiguration may not be null");
        }
        this.chaincodeCollectionConfiguration = chaincodeCollectionConfiguration;
    }

    public String chaincodeCodeEndorsementPlugin() {
        return chaincodeCodeEndorsementPlugin;
    }

    public String chaincodeCodeValidationPlugin() {

        return chaincodeCodeValidationPlugin;
    }

    public void setChaincodeCodeEndorsementPlugin(String chaincodeCodeEndorsementPlugin) throws InvalidArgumentException {
        if (Utils.isNullOrEmpty(chaincodeCodeEndorsementPlugin)) {
            throw new InvalidArgumentException("The chaincodeCodeEndorsementPlugin parameter can not be null or empty.");
        }
        this.chaincodeCodeEndorsementPlugin = chaincodeCodeEndorsementPlugin;
    }

    public void setChaincodeCodeValidationPlugin(String chaincodeCodeValidationPlugin) throws InvalidArgumentException {
        if (Utils.isNullOrEmpty(chaincodeCodeValidationPlugin)) {
            throw new InvalidArgumentException("The chaincodeCodeValidationPlugin parameter can not be null or empty.");
        }
        this.chaincodeCodeValidationPlugin = chaincodeCodeValidationPlugin;
    }

    public ByteString getValidationParamter() {
        return validationParameter;
    }
}
