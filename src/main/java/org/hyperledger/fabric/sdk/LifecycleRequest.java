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
 * LifecycleInstallChaincodeRequest parameters for installing chaincode with lifecycle
 */
public class LifecycleRequest {

    private static final long defaultConfigWaitTime = Config.getConfig().getProposalWaitTime();
    protected boolean verifiable;

    // The timeout for a single proposal request to endorser in milliseconds
    private long proposalWaitTime = defaultConfigWaitTime;

    private User userContext;
    private boolean submitted = false;

    LifecycleRequest(User userContext) {
        this.userContext = userContext;
        verifiable = true;
    }

    LifecycleRequest(User userContext, boolean verifiable) {
        this.userContext = userContext;
        this.verifiable = verifiable;
    }

    void setSubmitted() {
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

    boolean isVerifiable() {

        return verifiable;

    }
}
