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

/**
 * Deploy request.
 */
public class DeploymentProposalRequest extends TransactionRequest<DeploymentProposalRequest> {

	/* chaincode will be deployed using this name */
	private String chaincodeName;
	
	/* url to chaincode bytes */
	private String chaincodePath;

	public String getChaincodeName() {
		return chaincodeName;
	}
	
	public String getChaincodePath() {
		return chaincodePath;
	}
	
	/** sets the url to the bytes of the chaincode to be deployed */
	public DeploymentProposalRequest setChaincodeName(String chaincodeName) {
		this.chaincodeName = chaincodeName;
		return getThis();
	}
	
	/** set the name that will be given to the chain code once instanciated */
	public DeploymentProposalRequest setChaincodePath(String chaincodePath) {
		this.chaincodePath = chaincodePath;
		return getThis();
	}
		
	@Override
	protected DeploymentProposalRequest getThis() {
		return this;
	}
	
}
