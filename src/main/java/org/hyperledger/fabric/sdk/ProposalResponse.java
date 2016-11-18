package org.hyperledger.fabric.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.hyperledger.protos.peer.Chaincode;
import org.hyperledger.protos.peer.ChaincodeProposal;
import org.hyperledger.protos.peer.FabricProposal;
import org.hyperledger.protos.peer.FabricProposalResponse;


public class ProposalResponse extends ChainCodeResponse{

    ProposalResponse(String chainCodeID, Status status, String message){
        super(null, chainCodeID, status, message);

    }
    ProposalResponse(String chainCodeID, int status, String message){
        super(null, chainCodeID, status, message);

    }
    FabricProposal.Proposal proposal;

    public FabricProposal.Proposal getProposal() {
        return proposal;
    }

    public FabricProposalResponse.ProposalResponse getProposalResponse() {
        return proposalResponse;
    }

    FabricProposalResponse.ProposalResponse proposalResponse;


    public void setProposal(FabricProposal.Proposal proposal) {
        this.proposal = proposal;
    }

    public void setProposalResponse(FabricProposalResponse.ProposalResponse proposalResponse) {
        this.proposalResponse = proposalResponse;
    }

    Peer peer = null;

    public void setPeer(Peer peer) {
        this.peer = peer;
    }

     public ByteString getPayload(){
        return proposalResponse.getResponse().getPayload();
    }

//    public ByteString getPayload2(){
//        ByteString x = proposalResponse.getPayload();
//        return proposalResponse.getPayload();
//    }

    public ChainCodeID getChainCodeID(){

        Chaincode.ChaincodeID chaincodeID = null; //TODO NEED to clean up
        try {
            ChaincodeProposal.ChaincodeProposalPayload ppl = ChaincodeProposal.ChaincodeProposalPayload.parseFrom(proposal.getPayload());
            Chaincode.ChaincodeInvocationSpec ccis = Chaincode.ChaincodeInvocationSpec.parseFrom(ppl.getInput());
            Chaincode.ChaincodeSpec scs = ccis.getChaincodeSpec();
            Chaincode.ChaincodeInput cci = scs.getCtorMsg();
            ByteString deps = cci.getArgs(2);
            Chaincode.ChaincodeDeploymentSpec chaincodeDeploymentSpec = Chaincode.ChaincodeDeploymentSpec.parseFrom(deps.toByteArray());
            chaincodeID = chaincodeDeploymentSpec.getChaincodeSpec().getChaincodeID();
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        return  new ChainCodeID(chaincodeID);
    }


}
