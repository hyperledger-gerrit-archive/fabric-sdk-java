package org.hyperledger.fabric.sdk;

import org.hyperledger.fabric.protos.peer.Chaincode;

/**
 * Request for getting information about the blockchain ledger.
 */
public class QueryBlockchainInfoRequest extends TransactionRequest {

    @Override
    public ChainCodeID getChaincodeID() {
        return new ChainCodeID(
                Chaincode.ChaincodeID.newBuilder().setName("qscc").build());
    }

    @Override
    public String getChainId() {
        return "";
    }
}
