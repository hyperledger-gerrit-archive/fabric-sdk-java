/**
 * 
 */
package org.hyperledger.fabric.sdk.transaction;

import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.QueryException;
import org.hyperledger.protos.Chaincode;
import protos.Fabric;

import io.netty.util.internal.StringUtil;

public class QueryTransactionBuilder extends TransactionBuilder {

	protected QueryTransactionBuilder() {}
	
	public static QueryTransactionBuilder newBuilder() {
		return new QueryTransactionBuilder();
	}

	@Override
	public Transaction build() {
		try {
			return build(Fabric.Transaction.Type.CHAINCODE_QUERY);
		} catch (CryptoException e) {
			throw new QueryException("Error while creating query transaction", e);
		}
	}
	
	protected Transaction build(Fabric.Transaction.Type ccType) throws CryptoException {
		if (context == null || request == null) {
			throw new IllegalArgumentException("Must provide request and context before attempting to call build()");
		}
		
		// Verify that chaincodeID is being passed
        if (StringUtil.isNullOrEmpty(request.getChaincodeID())) {
          throw new RuntimeException("missing chaincodeID in InvokeOrQueryRequest");
        }
        
     	// create transaction
		Fabric.Transaction.Builder txBuilder = createTransactionBuilder(Chaincode.ChaincodeSpec.Type.GOLANG,
					ccType,
					request.getChaincodeID(), request.getArgs(), null, request.getChaincodeName(),
					null);

	     return new Transaction(txBuilder, request.getChaincodeID());
	}

}
