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

/**
 * LifecycleInstallChaincodeRequest parameters for installing chaincode with lifecycle
 */
public class LifecycleCommitChaincodeDefinitionRequest extends LifecycleRequest {
    private static final Config config = Config.getConfig();
    protected String chaincodeName;
    // The version of the chaincode
    protected String chaincodeVersion;

    private long definitionSequence;
    private ChaincodeCollectionConfiguration chaincodeCollectionConfiguration;

    private String chaincodeCodeEndorsementPlugin = null;
    private Boolean initRequired = null;

    private ByteString validationParameter = null;
    private String chaincodeCodeValidationPlugin;

    LifecycleCommitChaincodeDefinitionRequest(User userContext) {
        super(userContext);
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
    public void setChaincodeName(String chaincodeName) {
        this.chaincodeName = chaincodeName;
    }

    public String getChaincodeVersion() {
        return chaincodeVersion;
    }

    /**
     * @param chaincodeVersion
     */

    public void setChaincodeVersion(String chaincodeVersion) {
        this.chaincodeVersion = chaincodeVersion;

    }

    public long getDefinitionSequence() {
        return definitionSequence;
    }

    public void setDefinitionSequence(long definitionSequence) {
        this.definitionSequence = definitionSequence;
    }

    public ChaincodeCollectionConfiguration getChaincodeCollectionConfiguration() {
        return this.chaincodeCollectionConfiguration;
    }

    public void setChaincodeCollectionConfiguration(ChaincodeCollectionConfiguration chaincodeCollectionConfiguration) throws InvalidArgumentException {
        this.chaincodeCollectionConfiguration = chaincodeCollectionConfiguration;
    }

    public void setChaincodeEndorsementPolicy(LifecycleChaincodeEndorsementPolicy chaincodeEndorsementPolicy) throws InvalidArgumentException {
        validationParameter = chaincodeEndorsementPolicy.getByteString();
    }

    public String chaincodeCodeEndorsementPlugin() {
        return chaincodeCodeEndorsementPlugin;
    }

    public String chaincodeCodeValidationPlugin() {

        return chaincodeCodeValidationPlugin;
    }

    public void setChaincodeCodeEndorsementPlugin(String chaincodeCodeEndorsementPlugin) throws InvalidArgumentException {
        this.chaincodeCodeEndorsementPlugin = chaincodeCodeEndorsementPlugin;
    }

    public void setChaincodeCodeValidationPlugin(String chaincodeCodeValidationPlugin) throws InvalidArgumentException {
        this.chaincodeCodeValidationPlugin = chaincodeCodeValidationPlugin;
    }

    public ByteString getValidationParameter() {
        return validationParameter;
    }

    public void setValidationParameter(byte[] validationParameter) throws InvalidArgumentException {
        this.validationParameter = ByteString.copyFrom(validationParameter);
    }
}
