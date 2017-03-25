package org.hyperledger.fabric.sdk.transaction;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.protos.peer.Chaincode;
import org.hyperledger.fabric.protos.peer.FabricProposal;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.ProposalException;

import static org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeSpec.Type.GOLANG;

/**
 * Created by rineholt on 3/27/17.
 */
public class CSCCProposalBuilder extends ProposalBuilder {
    private static final String CSCC_CHAIN_NAME = "cscc";
    private static final Chaincode.ChaincodeID CHAINCODE_ID_CSCC =
            Chaincode.ChaincodeID.newBuilder().setName(CSCC_CHAIN_NAME).build();

    @Override
    public CSCCProposalBuilder context(TransactionContext context) {
         super.context(context);
         return  this;
    }

    @Override
    public FabricProposal.Proposal build() throws ProposalException, CryptoException {


        ccType(GOLANG);
        chaincodeID(CHAINCODE_ID_CSCC);

        chainID(""); //no specific chain -- system chain.

        return super.build();

    }
}
