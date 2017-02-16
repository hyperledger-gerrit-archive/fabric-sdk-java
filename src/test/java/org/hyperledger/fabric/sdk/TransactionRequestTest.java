/*
 *  Copyright 2017 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
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
package org.hyperledger.fabric.sdk;

import static org.junit.Assert.*;

import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeID;
import org.junit.Test;

public class TransactionRequestTest {

	
	@Test(expected = IllegalStateException.class)
	public void testSetChaincodeIDAndChaincodePath() {
		final TransactionRequest tr = new TransactionRequest();
		tr.setChaincodeID(new ChainCodeID(ChaincodeID.getDefaultInstance()));
		tr.setChaincodePath("path");
	}
	
	@Test
	public void testSetChaincodePath() {
		final TransactionRequest tr = new TransactionRequest();
		tr.setChaincodePath("path");
		assertEquals("path", tr.getChaincodePath());
	}

	@Test(expected = IllegalStateException.class)
	public void testSetChaincodeIDAndChaincodeName() {
		final TransactionRequest tr = new TransactionRequest();
		tr.setChaincodeID(new ChainCodeID(ChaincodeID.getDefaultInstance()));
		tr.setChaincodeName("name");
	}

	@Test
	public void testSetChaincodeName() {
		final TransactionRequest tr = new TransactionRequest();
		tr.setChaincodeName("name");
		assertEquals("name", tr.getChaincodeName());
	}

	@Test
	public void testSetChaincodeID() {
		final TransactionRequest tr = new TransactionRequest();
		final ChainCodeID ccid = new ChainCodeID(ChaincodeID.getDefaultInstance());
		tr.setChaincodeID(ccid);
		assertEquals(tr.getChaincodeID(), ccid);
	}

	@Test(expected = IllegalStateException.class)
	public void testSetChaincodeNameAndChaincodeID() {
		final TransactionRequest tr = new TransactionRequest();
		tr.setChaincodeName("name");
		tr.setChaincodeID(new ChainCodeID(ChaincodeID.getDefaultInstance()));
	}

	@Test(expected = IllegalStateException.class)
	public void testSetChaincodePathAndChaincodeID() {
		final TransactionRequest tr = new TransactionRequest();
		tr.setChaincodePath("path");
		tr.setChaincodeID(new ChainCodeID(ChaincodeID.getDefaultInstance()));
	}

}
