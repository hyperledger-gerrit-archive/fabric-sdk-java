package org.hyperledger.fabric.sdk;

import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeID;

/**
 * Wrapper to not expose Fabric's ChainCoodeId
 * 
 */
public class ChainCodeID {

	private final ChaincodeID protoChaincodeID;
	
	public static class Builder {
		private final ChaincodeID.Builder protoBuilder = ChaincodeID.newBuilder();

		private Builder(){}
		
		public Builder setName(String value) {
			this.protoBuilder.setName(value);
			return this;
		}

		public ChainCodeID build() {
			return new ChainCodeID(this.protoBuilder.build());
		}
	}

	public static Builder newBuilder() {
		return new Builder();
	}
	
	ChaincodeID getFabricChainCodeID() {
		return protoChaincodeID;
	}

	ChainCodeID(ChaincodeID chaincodeID) {
		this.protoChaincodeID = chaincodeID;
	}

}
