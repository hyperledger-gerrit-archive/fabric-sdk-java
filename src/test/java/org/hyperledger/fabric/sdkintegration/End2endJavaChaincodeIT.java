/*
 *  Copyright 2016, 2017 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hyperledger.fabric.sdkintegration;

import static org.hyperledger.fabric.sdk.TransactionRequest.Type.JAVA;

import java.io.File;

import org.hyperledger.fabric.sdk.TransactionRequest.Type;

/**
 * Test end to end scenario
 */
public class End2endJavaChaincodeIT extends End2endIT {

	@Override
	protected String getChainCodeName() {
		return "end2end_cc_java";
	}
	
	@Override
	protected String getChainCodePath() {
		/* relative to getChaincodeSourceLocation() */
		return "SimpleChaincode";
	}
	
	@Override
	protected String getChainCodeVersion() {
		return "1.0.0.0";
	}
	
	@Override
	protected Type getChaincodeLanguage() {
		return JAVA;
	}
	
	@Override
	protected File getChaincodeSourceLocation() {
		return new File("src/test/fixture/chaincode/java");
	}
	
}
