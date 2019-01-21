/*
 *
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 * /
 */

package org.hyperledger.fabric.sdk;

import org.hyperledger.fabric.sdk.helper.Config;

/**
 * LifecycleInstallRequest parameters for installing chaincode with lifecycle
 */
public class LifecycleApproveChaincodeDefinitionForMyOrgRequest {
    private static final Config config = Config.getConfig();
    protected String chaincodeName;
    // The version of the chaincode
    protected String chaincodeVersion;
    // The timeout for a single proposal request to endorser in milliseconds
    private long proposalWaitTime = config.getProposalWaitTime();

    private User userContext;
    private boolean submitted = false;
    private byte[] chaincodeHash;
    private long definitionSequence;
    private ChaincodeCollectionConfiguration chaincodeCollectionConfiguration;
    private ChaincodeEndorsementPolicy chaincodeEndorsementPolicy;
    private String chaincodeCodeEndorsementPlugin = null;
    private boolean initRequired;

    private String chaincodeCodeValidationPlugin = null;

    LifecycleApproveChaincodeDefinitionForMyOrgRequest(User userContext) {
        this.userContext = userContext;
    }

    public boolean isInitRequired() {
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

    public byte[] getChaincodeHash() {

        return chaincodeHash;
    }

    /**
     * @param chaincodeHash
     */

    public void setChaincodeHash(byte[] chaincodeHash) {
        this.chaincodeHash = new byte[chaincodeHash.length];
        System.arraycopy(chaincodeHash, 0, this.chaincodeHash, 0, chaincodeHash.length); // have our copy.
    }

    public ChaincodeCollectionConfiguration getChaincodeCollectionConfiguration() {
        return this.chaincodeCollectionConfiguration;
    }

    public void setChaincodeCollectionConfiguration(ChaincodeCollectionConfiguration chaincodeCollectionConfiguration) {
        this.chaincodeCollectionConfiguration = chaincodeCollectionConfiguration;
    }

    public ChaincodeEndorsementPolicy getChaincodeEndorsementPolicy() {
        return chaincodeEndorsementPolicy;
    }

    public void setChaincodeEndorsementPolicy(ChaincodeEndorsementPolicy chaincodeEndorsementPolicy) {
        this.chaincodeEndorsementPolicy = chaincodeEndorsementPolicy;
    }

    public String chaincodeCodeEndorsementPlugin() {
        return chaincodeCodeEndorsementPlugin;
    }

    public String chaincodeCodeValidationPlugin() {

        return chaincodeCodeValidationPlugin;
    }

    public void setChaincodeCodeEndorsementPlugin(String chaincodeCodeEndorsementPlugin) {
        this.chaincodeCodeEndorsementPlugin = chaincodeCodeEndorsementPlugin;
    }

    public void setChaincodeCodeValidationPlugin(String chaincodeCodeValidationPlugin) {
        this.chaincodeCodeValidationPlugin = chaincodeCodeValidationPlugin;
    }
}
