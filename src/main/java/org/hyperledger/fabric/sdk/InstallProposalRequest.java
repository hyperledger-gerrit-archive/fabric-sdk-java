/*
 *  Copyright 2016, 2017 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hyperledger.fabric.sdk;

import java.io.File;
import java.io.InputStream;

import org.hyperledger.fabric.sdk.TransactionRequest.Type;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.helper.Config;

/**
 * InstallProposalRequest.
 */
//public class InstallProposalRequest extends TransactionRequest {
public class InstallProposalRequest {
    private final Config config = Config.getConfig();

    private File chaincodeSourceLocation = null;
    private InputStream chaincodeInputStream = null;
    private File chaincodeMetaInfLocation = null;

    public LifecycleChaincodePackage getLifecycleChaincodePackage() {
        return lifecycleChaincodePackage;
    }

    private LifecycleChaincodePackage lifecycleChaincodePackage;
    private User userContext;
    private boolean submitted = false;

    protected String chaincodePath;
    // The name identifier for the chaincode to deploy in development mode.
    protected String chaincodeName;

    // The version of the chaincode
    protected String chaincodeVersion;
    // The chaincode ID as provided by the 'submitted' event emitted by a TransactionContext
    private ChaincodeID chaincodeID;

    // Chaincode language
    protected Type chaincodeLanguage = Type.GO_LANG;

    // The timeout for a single proposal request to endorser in milliseconds
    protected long proposalWaitTime = config.getProposalWaitTime();

    File getChaincodeMetaInfLocation() {
        return chaincodeMetaInfLocation;
    }

    /**
     * Set the META-INF directory to be used for packaging chaincode.
     * Only applies if source location {@link #chaincodeSourceLocation} for the chaincode is set.
     *
     * @param chaincodeMetaInfLocation The directory where the "META-INF" directory is located..
     * @see <a href="http://hyperledger-fabric.readthedocs.io/en/master/couchdb_as_state_database.html#using-couchdb-from-chaincode">
     * Fabric Read the docs couchdb as a state database
     * </a>
     * @deprecated Use new Lifecycle chaincode package {@link #setLifecycleChaincodePackage(LifecycleChaincodePackage)}
     */

    public void setChaincodeMetaInfLocation(File chaincodeMetaInfLocation) throws InvalidArgumentException {
        if (chaincodeMetaInfLocation == null) {
            throw new InvalidArgumentException("Chaincode META-INF location may not be null.");
        }

        if (chaincodeInputStream != null) {
            throw new InvalidArgumentException("Chaincode META-INF location may not be set with chaincode input stream set.");
        }
        this.chaincodeMetaInfLocation = chaincodeMetaInfLocation;
    }

    InstallProposalRequest(User userContext) {
        this.userContext = userContext;
    }

    public InputStream getChaincodeInputStream() {
        return chaincodeInputStream;
    }

    /**
     * Chaincode input stream containing the actual chaincode. Only format supported is a tar zip compressed input of the source.
     * Only input stream or source location maybe used at the same time.
     * The contents of the stream are not validated or inspected by the SDK.
     *
     * @param chaincodeInputStream
     * @throws InvalidArgumentException
     * @deprecated Use new Lifecycle chaincode package {@link #setLifecycleChaincodePackage(LifecycleChaincodePackage)}
     */

    public void setChaincodeInputStream(InputStream chaincodeInputStream) throws InvalidArgumentException {
        if (chaincodeInputStream == null) {
            throw new InvalidArgumentException("Chaincode input stream may not be null.");
        }
        if (chaincodeSourceLocation != null) {
            throw new InvalidArgumentException("Error setting chaincode input stream. Chaincode source location already set. Only one or the other maybe set.");
        }
        if (chaincodeMetaInfLocation != null) {
            throw new InvalidArgumentException("Error setting chaincode input stream. Chaincode META-INF location  already set. Only one or the other maybe set.");
        }
        this.chaincodeInputStream = chaincodeInputStream;
    }

    public File getChaincodeSourceLocation() {
        return chaincodeSourceLocation;
    }

    /**
     * The location of the chaincode.
     * Chaincode input stream and source location can not both be set.
     *
     * @param chaincodeSourceLocation
     * @throws InvalidArgumentException
     * @deprecated Use new Lifecycle chaincode package {@link #setLifecycleChaincodePackage(LifecycleChaincodePackage)}
     */
    public void setChaincodeSourceLocation(File chaincodeSourceLocation) throws InvalidArgumentException {
        if (chaincodeSourceLocation == null) {
            throw new InvalidArgumentException("Chaincode source location may not be null.");
        }
        if (chaincodeInputStream != null) {
            throw new InvalidArgumentException("Error setting chaincode location. Chaincode input stream already set. Only one or the other maybe set.");
        }

        this.chaincodeSourceLocation = chaincodeSourceLocation;
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

    /**
     * @return
     * @deprecated Use new Lifecycle chaincode package {@link #setLifecycleChaincodePackage(LifecycleChaincodePackage)}
     */
    public String getChaincodePath() {
        return null == chaincodePath ? "" : chaincodePath;
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

    /**
     * @param chaincodeVersion
     */

    public void setChaincodeVersion(String chaincodeVersion) {
        this.chaincodeVersion = chaincodeVersion;

    }

    public String getChaincodeVersion() {
        return chaincodeVersion;
    }

    /**
     * @return
     * @deprecated Use new Lifecycle chaincode package {@link #setLifecycleChaincodePackage(LifecycleChaincodePackage)}
     */
    public ChaincodeID getChaincodeID() {
        return chaincodeID;
    }

    /**
     * @return
     * @deprecated Use new Lifecycle chaincode package {@link #setLifecycleChaincodePackage(LifecycleChaincodePackage)}
     */
    public Type getChaincodeLanguage() {
        return chaincodeLanguage;
    }

    /**
     * The chaincode language type: default type Type.GO_LANG
     *
     * @param chaincodeLanguage . Type.Java Type.GO_LANG Type.NODE
     * @deprecated Use new Lifecycle chaincode package {@link #setLifecycleChaincodePackage(LifecycleChaincodePackage)}
     */
    public void setChaincodeLanguage(Type chaincodeLanguage) {
        this.chaincodeLanguage = chaincodeLanguage;
    }

    /**
     * @param chaincodeID
     * @deprecated Use new Lifecycle chaincode package {@link #setLifecycleChaincodePackage(LifecycleChaincodePackage)}
     */

    public void setChaincodeID(ChaincodeID chaincodeID) {

        if (chaincodeName != null) {

            throw new IllegalArgumentException("Chaincode name has already been set.");
        }
        if (chaincodeVersion != null) {

            throw new IllegalArgumentException("Chaincode version has already been set.");
        }

        if (chaincodePath != null) {

            throw new IllegalArgumentException("Chaincode path has already been set.");
        }

        this.chaincodeID = chaincodeID;
        chaincodeName = chaincodeID.getName();
        chaincodePath = chaincodeID.getPath();
        chaincodeVersion = chaincodeID.getVersion();
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

}
