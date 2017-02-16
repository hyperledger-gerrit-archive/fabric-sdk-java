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

import java.util.ArrayList;
import java.util.Arrays;

import org.hyperledger.fabric.sdk.helper.Config;

import io.netty.util.internal.StringUtil;

/**
 * A base transaction request common for DeploymentProposalRequest, InvokeRequest, and QueryRequest.
 */
public abstract class TransactionRequest<T extends TransactionRequest<T>> {

	private final Config config = Config.getConfig();

	// The chaincode ID as provided by the 'submitted' event emitted by a TransactionContext
	private ChainCodeID chaincodeID;
	
	// The name of the function to invoke
	private String fcn;
	// The arguments to pass to the chaincode invocation
	private ArrayList<String> args;
	// Specify whether the transaction is confidential or not.  The default value is false.
	private boolean confidential = false;
	// Optionally provide a user certificate which can be used by chaincode to perform access control
	private Certificate userCert;
	// Optionally provide additional metadata
	private byte[] metadata;
	// Chaincode language
	private Type chaincodeLanguage = Type.GO_LANG;
	// The timeout for a single proposal request to endorser in milliseconds
	private long proposalWaitTime = config.getProposalWaitTime();


	public ChainCodeID getChaincodeID() {
		return chaincodeID;
	}
	
	public void setChaincodeID(ChainCodeID chaincodeID) {
		if(!StringUtil.isNullOrEmpty(chaincodeID.getFabricChainCodeID().getPath())) {
			throw new IllegalArgumentException("chaincode ID path must not be set");
		}
		this.chaincodeID = chaincodeID;
	}
	
	public String getFcn() {
		return fcn;
	}
	public T setFcn(String fcn) {
		this.fcn = fcn;
		return getThis();
	}
	
	protected abstract T getThis();
	
	public ArrayList<String> getArgs() {
		return args;
	}

	public T setArgs(String[] args) {
		this.args = new ArrayList<String>( Arrays.asList( args ) );
		return getThis();
	}
	public T setArgs(ArrayList<String> args) {
		this.args = args;
		return getThis();
	}
	public boolean isConfidential() {
		return confidential;
	}
	public void setConfidential(boolean confidential) {
		this.confidential = confidential;
	}
	public Certificate getUserCert() {
		return userCert;
	}
	public void setUserCert(Certificate userCert) {
		this.userCert = userCert;
	}
	public byte[] getMetadata() {
		return metadata;
	}
	public void setMetadata(byte[] metadata) {
		this.metadata = metadata;
	}


	//Mirror Fabric try not expose and of it's classes
	public enum Type{
		JAVA,
		GO_LANG
	}

	public Type getChaincodeLanguage() {
		return chaincodeLanguage;
	}

	/**
	 * The chain code language type: default type Type.GO_LANG
	 * @param chaincodeLanguage . Type.Java Type.GO_LANG
	 */
	public void setChaincodeLanguage(Type chaincodeLanguage) {
		this.chaincodeLanguage = chaincodeLanguage;
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
}
