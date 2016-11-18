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

package org.hyperledger.fabric.sdk.transaction;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.protos.common.Common;
import org.hyperledger.protos.peer.Chaincode;
import org.hyperledger.protos.peer.ChaincodeProposal;
import org.hyperledger.protos.peer.FabricProposal;

import java.time.Instant;
import java.util.List;


public class ProposalBuilder {



    private Log logger = LogFactory.getLog(ProposalBuilder.class);


    private Chaincode.ChaincodeID chaincodeID;
    private List<ByteString> argList;
    protected TransactionContext context;
    private Chaincode.ChaincodeSpec.Type ccType= Chaincode.ChaincodeSpec.Type.GOLANG ;

    protected ProposalBuilder() {}

    public static ProposalBuilder newBuilder() {
        return new ProposalBuilder();
    }

    public ProposalBuilder chaincodeID(Chaincode.ChaincodeID chaincodeID ) {
        this.chaincodeID = chaincodeID;
        return this;
    }

    public ProposalBuilder args(List<ByteString> argList ) {
        this.argList = argList;
        return this;
    }

    public ProposalBuilder context(TransactionContext context) {
        this.context = context;
        return this;
    }


    public FabricProposal.Proposal build() throws Exception {
       return createFabricProposal(chaincodeID, argList);
    }


    private  FabricProposal.Proposal createFabricProposal(Chaincode.ChaincodeID chaincodeID, List<ByteString> argList) throws Exception {

        Chaincode.ChaincodeInvocationSpec chaincodeInvocationSpec = createChaincodeInvocationSpec(
                chaincodeID,
                ccType, argList);


        ChaincodeProposal.ChaincodeHeaderExtension.Builder chaincodeHeaderExtension = ChaincodeProposal.ChaincodeHeaderExtension.newBuilder();

        chaincodeHeaderExtension.setChaincodeID(chaincodeID);


        Common.Header.Builder headerbldr =  Common.Header.newBuilder();
        Common.ChainHeader.Builder chainHeaderBldr = Common.ChainHeader.newBuilder();
        chainHeaderBldr.setType(Common.HeaderType.ENDORSER_TRANSACTION.getNumber());
        chainHeaderBldr.setVersion(0);
        chainHeaderBldr.setChainID(chaincodeID.toByteString());
        chainHeaderBldr.setExtension(chaincodeHeaderExtension.build().toByteString());
        Common.SignatureHeader.Builder sigHeaderBldr = Common.SignatureHeader.newBuilder();
        sigHeaderBldr.setCreator(context.getCreator());
        sigHeaderBldr.setNonce(context.getNonce());

        headerbldr.setSignatureHeader(sigHeaderBldr);
        headerbldr.setChainHeader(chainHeaderBldr);

        ChaincodeProposal.ChaincodeProposalPayload.Builder payloadBuilder = ChaincodeProposal.ChaincodeProposalPayload.newBuilder();

        payloadBuilder.setInput(chaincodeInvocationSpec.toByteString());

        FabricProposal.Proposal.Builder propbu = FabricProposal.Proposal.newBuilder();

        propbu.setHeader(headerbldr.build().toByteString());
        propbu.setPayload(payloadBuilder.build().toByteString());

        return propbu.build();

    }


    private Chaincode.ChaincodeInvocationSpec createChaincodeInvocationSpec(Chaincode.ChaincodeID chainCodeId, Chaincode.ChaincodeSpec.Type langType, List<ByteString> args) throws Exception {

        Chaincode.ChaincodeInvocationSpec.Builder invocationSpecBuilder = Chaincode.ChaincodeInvocationSpec.newBuilder();
        Chaincode.ChaincodeSpec.Builder chaincodeSpecBuilder = Chaincode.ChaincodeSpec.newBuilder();

        chaincodeSpecBuilder.setType(langType);

        chaincodeSpecBuilder.setChaincodeID(chainCodeId);

        Chaincode.ChaincodeInput chaincodeInput = Chaincode.ChaincodeInput.newBuilder().addAllArgs(args).build();

        chaincodeSpecBuilder.setCtorMsg(chaincodeInput);

        invocationSpecBuilder.setChaincodeSpec(chaincodeSpecBuilder.build());

        invocationSpecBuilder.setIdGenerationAlg("");


        return invocationSpecBuilder.build();

    }




    private Timestamp getFabricTimestamp() {
        Timestamp.Builder ts = Timestamp.newBuilder();
        ts.setSeconds(Instant.now().toEpochMilli());
        return ts.build();
    }


    public ProposalBuilder ccType(Chaincode.ChaincodeSpec.Type ccType) {
        this.ccType= ccType;
        return this;
    }
}