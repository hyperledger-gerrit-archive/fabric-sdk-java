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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.protos.common.Common;
import org.hyperledger.protos.peer.ChaincodeTransaction;
import org.hyperledger.protos.peer.FabricProposal;
import org.hyperledger.protos.peer.FabricProposalResponse;
import org.hyperledger.protos.peer.FabricTransaction;

import java.util.Collection;

public class TransactionBuilder {

    private Log logger = LogFactory.getLog(TransactionBuilder.class);
    private FabricProposal.Proposal chaincodeProposal;
    private Collection<FabricProposalResponse.Endorsement> endorsements;
    private ByteString proposalResponcePayload;
    private TransactionContext context;



    public static TransactionBuilder newBuilder() {
        return new TransactionBuilder();
    }


    public TransactionBuilder context(TransactionContext context ){
        this.context = context;
        return this;
    }


    public TransactionBuilder chaincodeProposal(FabricProposal.Proposal chaincodeProposal ){
        this.chaincodeProposal = chaincodeProposal;
        return this;
    }

    public TransactionBuilder endorsements(Collection<FabricProposalResponse.Endorsement> endorsements ){
        this.endorsements = endorsements;
        return this;
    }

    public TransactionBuilder proposalResponcePayload(ByteString proposalResponcePayload ){
        this.proposalResponcePayload = proposalResponcePayload;
        return this;
    }


    public Common.Envelope build(){

        return createTransactionEnvelope(chaincodeProposal, proposalResponcePayload, endorsements );

    }



    private Common.Envelope createTransactionEnvelope(FabricProposal.Proposal chaincodeProposal, ByteString proposalResponcePayload,
                                                     Collection<FabricProposalResponse.Endorsement> endorsements) {


        ChaincodeTransaction.ChaincodeEndorsedAction.Builder cceab = ChaincodeTransaction.ChaincodeEndorsedAction.newBuilder();
        cceab.setProposalResponsePayload(proposalResponcePayload);

        cceab.addAllEndorsements(endorsements);

        ChaincodeTransaction.ChaincodeActionPayload.Builder ccapb = ChaincodeTransaction.ChaincodeActionPayload.newBuilder();
        ccapb.setAction(cceab.build());
        ccapb.setChaincodeProposalPayload(chaincodeProposal.toByteString());

        FabricTransaction.TransactionAction.Builder tab = FabricTransaction.TransactionAction.newBuilder();
        tab.setHeader(chaincodeProposal.getHeader());
        tab.setPayload(ccapb.build().toByteString());

        FabricTransaction.Transaction2.Builder tb = FabricTransaction.Transaction2.newBuilder();

        tb.addActions(tab.build());

        FabricTransaction.Transaction2 transaction = tb.build();


        Common.Envelope.Builder ceb = Common.Envelope.newBuilder();
        Common.Payload.Builder payload = Common.Payload.newBuilder();
        Common.Header.Builder header = Common.Header.newBuilder();

        Common.ChainHeader.Builder chainHeader = Common.ChainHeader.newBuilder();
        chainHeader.setType(Common.HeaderType.ENDORSER_TRANSACTION.getNumber());
        chainHeader.setVersion(0);
        //   chainHeader.setChainID(byte[0])

        header.setChainHeader(chainHeader.build());

        payload.setHeader(header.build());
        payload.setData(transaction.toByteString());

        ceb.setPayload(payload.build().toByteString());


        logger.debug("Done creating transaction ready for orderer");

        return ceb.build();

    }

}
