package org.hyperledger.fabric.sdk.transaction;

import org.hyperledger.fabric.protos.peer.Chaincode;
import org.hyperledger.fabric.protos.peer.FabricProposal;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.ProposalException;

import static org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeSpec.Type.GOLANG;


public class LCCCProposalBuilder extends ProposalBuilder {
    private static final String LCCC_CHAIN_NAME = "lccc";
    private static final Chaincode.ChaincodeID CHAINCODE_ID_LCCC =
            Chaincode.ChaincodeID.newBuilder().setName(LCCC_CHAIN_NAME).build();

    @Override
    public LCCCProposalBuilder context(TransactionContext context) {
        super.context(context);
        return this;
    }

    @Override
    public FabricProposal.Proposal build() throws ProposalException, CryptoException {

        ccType(GOLANG);
        chaincodeID(CHAINCODE_ID_LCCC);

        return super.build();

    }
}
