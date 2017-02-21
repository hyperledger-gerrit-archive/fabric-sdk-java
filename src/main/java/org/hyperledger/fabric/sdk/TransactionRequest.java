/*
 *  Copyright 2016 DTCC, Fujitsu Australia Software Technology - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 	  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hyperledger.fabric.sdk;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hyperledger.fabric.protos.peer.Chaincode;
import org.hyperledger.fabric.protos.peer.FabricProposal;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.helper.Config;
import org.hyperledger.fabric.sdk.transaction.ProposalBuilder;
import org.hyperledger.fabric.sdk.transaction.TransactionContext;

import com.google.protobuf.ByteString;

/**
 * A base transaction request common for InstallProposalRequest, InvokeRequest, and QueryRequest.
 */
public class TransactionRequest {

    private final Config config = Config.getConfig();
    private boolean noChainID = false;  // calls to QSCC leave the chainID field as a empty string.

    // The local path containing the chaincode to deploy in network mode.
    protected String chaincodePath;
    // The name identifier for the chaincode to deploy in development mode.
    protected String chaincodeName;


    // The version of the chainCode
    protected String chaincodeVersion;
    // The chaincode ID as provided by the 'submitted' event emitted by a TransactionContext
    private ChainCodeID chaincodeID;


    // The name of the function to invoke
    protected String fcn;
    // The arguments to pass to the chaincode invocation
    protected ArrayList<String> args;
    // arguments to be passed as byte[] to the chaincode invocation
    // TODO for now assume that it's always args followed by argBytes in the chaincodeSpec. Need to firm up the protobufs with Fabric folks
    protected ArrayList<byte[]> argBytes;
    // Optionally provide a user certificate which can be used by chaincode to perform access control
    private Certificate userCert;
    // Chaincode language
    protected Type chaincodeLanguage = Type.GO_LANG;
    // The endorsementPolicy associated with this chaincode
    private ChaincodeEndorsementPolicy endorsementPolicy = null;
    // The timeout for a single proposal request to endorser in milliseconds
    protected long proposalWaitTime = config.getProposalWaitTime();

    // Protobuf message builder
    protected ProposalBuilder proposalBuilder = ProposalBuilder.newBuilder();

    /**
     * set the chainID field in the protobuf Proposal to the empty string.
     * Some peer requests (e.g. queries to QSCC) require the field to be blank.
     * Subclasses should override this method as needed.
     * @param proposalBuilder
     */
    protected void clearChainID(ProposalBuilder proposalBuilder) {
        return;
    }

    /**
     * Some proposal responses from Fabric are not signed. We default to always verify a ProposalResponse.
     * Subclasses should override this method if you do not want the response signature to be verified
     * @return true if proposal response is to be checked for a valid signature
     */
    public boolean doVerify() {
        return true;
    }

    public String getChaincodePath() {
        return null == chaincodePath ? "" : chaincodePath;
    }

    public TransactionRequest setChaincodePath(String chaincodePath) {

        this.chaincodePath = chaincodePath;
        return this;
    }

    public String getChaincodeName() {
        return chaincodeName;
    }

    public TransactionRequest setChaincodeName(String chaincodeName) {
        this.chaincodeName = chaincodeName;
        return this;
    }

    public TransactionRequest setChaincodeVersion(String chaincodeVersion) {
        this.chaincodeVersion = chaincodeVersion;
        return this;
    }

    public String getChaincodeVersion() {
        return chaincodeVersion;
    }

    public ChainCodeID getChaincodeID() {
        return chaincodeID;
    }

    public void setChaincodeID(ChainCodeID chaincodeID) {

        if(chaincodeName != null ){

            throw new IllegalArgumentException("Chaincode name has already been set.");
        }
        if(chaincodeVersion != null ){

            throw new IllegalArgumentException("Chaincode version has already been set.");
        }

        if(chaincodePath != null ){

            throw new IllegalArgumentException("Chaincode path has already been set.");
        }

        this.chaincodeID = chaincodeID;
        chaincodeName = chaincodeID.getName();
        chaincodePath = chaincodeID.getPath();
        chaincodeVersion = chaincodeID.getVersion();
    }

    public String getFcn() {
        return fcn;
    }

    public TransactionRequest setFcn(String fcn) {
        this.fcn = fcn;
        return this;
    }

    public ArrayList<String> getArgs() {
        return args;
    }

    public TransactionRequest setArgs(String[] args) {

        this.args = new ArrayList<String>(Arrays.asList(args));
        return this;
    }

    public TransactionRequest setArgs(ArrayList<String> args) {
        this.args = args;
        return this;
    }

    public ArrayList<byte[]> getArgBytes() {
        return argBytes;
    }

    public TransactionRequest setArgBytes(byte[][] argsBytes) {
        this.argBytes = new ArrayList<byte[]>(Arrays.asList(argsBytes));
        return this;
    }

    public TransactionRequest setArgBytes(ArrayList<byte[]> argBytes) {
        this.argBytes = argBytes;
        return this;
    }

    public Certificate getUserCert() {
        return userCert;
    }

    public void setUserCert(Certificate userCert) {
        this.userCert = userCert;
    }


    //Mirror Fabric try not expose and of it's classes
    public enum Type {
        JAVA,
        GO_LANG
    }

    public Type getChaincodeLanguage() {
        return chaincodeLanguage;
    }

    /**
     * The chain code language type: default type Type.GO_LANG
     *
     * @param chaincodeLanguage . Type.Java Type.GO_LANG
     */
    public void setChaincodeLanguage(Type chaincodeLanguage) {
        this.chaincodeLanguage = chaincodeLanguage;
    }

    /**
     * sets the endorsementPolicy associated with the chaincode of this transaction
     * @param policy a Policy object
     *
     * @see ChaincodeEndorsementPolicy
     */
    public void setChaincodeEndorsementPolicy(ChaincodeEndorsementPolicy policy) {
        this.endorsementPolicy = policy;
    }

    /**
     * returns the Policy object associated with the chaincode of this transaction
     * @return a Policy object
     *
     * @see ChaincodeEndorsementPolicy
     */
    public ChaincodeEndorsementPolicy getChaincodeEndorsementPolicy() {
        return this.endorsementPolicy;
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

    public FabricProposal.Proposal buildProposalMessage(TransactionContext transactionContext) throws CryptoException, ProposalException {
        proposalBuilder.context(transactionContext);
        clearChainID(proposalBuilder);

        List<ByteString> argList = new ArrayList<>();
        argList.add(ByteString.copyFrom(getFcn(), StandardCharsets.UTF_8));
        if (args != null && args.size()>0)
            for (String arg : args) {
                argList.add(ByteString.copyFrom(arg.getBytes()));
            }
        if (argBytes != null && argBytes.size()>0)
            for (byte[] arg : argBytes) {
                argList.add(ByteString.copyFrom(arg));
            }

        proposalBuilder.args(argList);
        proposalBuilder.chaincodeID(getChaincodeID().getFabricChainCodeID());
        proposalBuilder.ccType(getChaincodeLanguage() == TransactionRequest.Type.JAVA ?
                Chaincode.ChaincodeSpec.Type.JAVA : Chaincode.ChaincodeSpec.Type.GOLANG);

        return proposalBuilder.build();
    }
}
