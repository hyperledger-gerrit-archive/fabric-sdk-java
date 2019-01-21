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
public class LifecycleInstallRequest {
    private static final Config config = Config.getConfig();
    protected String chaincodeName;
    // The version of the chaincode
    protected String chaincodeVersion;
    // The timeout for a single proposal request to endorser in milliseconds
    private long proposalWaitTime = config.getProposalWaitTime();
    private LifecycleChaincodePackage lifecycleChaincodePackage;
    private User userContext;
    private boolean submitted = false;

    LifecycleInstallRequest(User userContext) {
        this.userContext = userContext;
    }

    public LifecycleChaincodePackage getLifecycleChaincodePackage() {
        return lifecycleChaincodePackage;
    }

    public void setLifecycleChaincodePackage(LifecycleChaincodePackage lifecycleChaincodePackage) throws InvalidArgumentException {

        this.lifecycleChaincodePackage = lifecycleChaincodePackage;
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

}
