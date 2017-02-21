package org.hyperledger.fabric.sdk;

import org.hyperledger.fabric.protos.peer.Chaincode;

/**
 * Request for getting information about the blockchain ledger.
 */
public class QuerySCCRequest extends TransactionRequest {

    public static final String GETCHAININFO = "GetChainInfo";
    public static final String GETBLOCKBYNUMBER = "GetBlockByNumber";
    public static final String GETBLOCKBYHASH = "GetBlockByHash";
    public static final String GETTRANSACTIONBYID = "GetTransactionByID";
    public static final String GETBLOCKBYTXID = "GetBlockByTxID";

    @Override
    public ChainCodeID getChaincodeID() {
        return new ChainCodeID(
                Chaincode.ChaincodeID.newBuilder().setName("qscc").build());
    }

    /* (non-Javadoc)
     * Responses from QSCC are not signed
     * @see org.hyperledger.fabric.sdk.TransactionRequest#getVerify()
     */
    @Override
    public boolean doVerify() {
        return false;
    }
}
