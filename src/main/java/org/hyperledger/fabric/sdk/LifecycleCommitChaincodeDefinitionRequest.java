/*
 *
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 * /
 */

package org.hyperledger.fabric.sdk;

import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.helper.Config;

/**
 * LifecycleInstallRequest parameters for installing chaincode with lifecycle
 */
public class LifecycleCommitChaincodeDefinitionRequest {
    private static final Config config = Config.getConfig();
    protected String chaincodeName;
    // The version of the chaincode
    protected String chaincodeVersion;
    // The timeout for a single proposal request to endorser in milliseconds
    private long proposalWaitTime = config.getProposalWaitTime();

    private User userContext;
    private boolean submitted = false;

    private long definitionSequence;
    private ChaincodeCollectionConfiguration chaincodeCollectionConfiguration;
    private ChaincodeEndorsementPolicy chaincodeEndorsementPolicy;
    private String chaincodeCodeEndorsementPlugin = null;
    private Boolean initRequired = null;

    private String channelConfigPolicyReference = null;

    private String chaincodeCodeValidationPlugin = null;
    private byte[] chaincodeValidationParameter = null;

    LifecycleCommitChaincodeDefinitionRequest(User userContext) {
        this.userContext = userContext;
    }

    public byte[] getChaincodeValidationParameter() {
        return chaincodeValidationParameter;
    }

    public void setChaincodeValidationParameter(byte[] chaincodeValidationParameter) throws InvalidArgumentException {
        if (null != chaincodeEndorsementPolicy || channelConfigPolicyReference != null) {
            throw new InvalidArgumentException("Only one can be set of chaincodeEndorsementPolicy, chaincodeValidationParameter and channelConfigPolicyReference can not be set at the same time.");
        }
        this.chaincodeValidationParameter = new byte[chaincodeValidationParameter.length];

        System.arraycopy(chaincodeValidationParameter, 0, this.chaincodeValidationParameter, 0, chaincodeValidationParameter.length);

    }

    public Boolean isInitRequired() {
        return initRequired;
    }

    public void setInitRequired(boolean initRequired) {
        this.initRequired = initRequired;
    }

    public void setSubmitted() {
        submitted = true;
    }

    public User getUserContext() {

        return userContext;
    }

    /**
     * Set the user context for this request. This context will override the user context set
     * on {@link HFClient#setUserContext(User)}
     *
     * @param userContext The user context for this request used for signing.
     */
    public void setUserContext(User userContext) {
        this.userContext = userContext;
    }

    /**
     * Gets the timeout for a single proposal request to endorser in milliseconds.
     *
     * @return the timeout for a single proposal request to endorser in milliseconds
     */
    public long getProposalWaitTime() {
        return proposalWaitTime;
    }

    /**
     * Sets the timeout for a single proposal request to endorser in milliseconds.
     *
     * @param proposalWaitTime the timeout for a single proposal request to endorser in milliseconds
     */
    public void setProposalWaitTime(long proposalWaitTime) {
        this.proposalWaitTime = proposalWaitTime;
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

    public ChaincodeEndorsementPolicy getChaincodeEndorsementPolicy() {
        return chaincodeEndorsementPolicy;
    }

    public void setChaincodeEndorsementPolicy(ChaincodeEndorsementPolicy chaincodeEndorsementPolicy) throws InvalidArgumentException {

        if (null != chaincodeValidationParameter || channelConfigPolicyReference != null) {
            throw new InvalidArgumentException("Only one can be set of chaincodeEndorsementPolicy, chaincodeValidationParameter and channelConfigPolicyReference can not be set at the same time.");
        }
        this.chaincodeEndorsementPolicy = chaincodeEndorsementPolicy;
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

    public String getChannelConfigPolicyReference() {
        return channelConfigPolicyReference;
    }

    public void setChannelConfigPolicyReference(String channelConfigPolicyReference) throws InvalidArgumentException {
        if (null != chaincodeEndorsementPolicy || chaincodeValidationParameter != null) {
            throw new InvalidArgumentException("Only one can be set of chaincodeEndorsementPolicy, chaincodeValidationParameter and channelConfigPolicyReference can not be set at the same time.");
        }
        this.channelConfigPolicyReference = channelConfigPolicyReference;
    }
}
