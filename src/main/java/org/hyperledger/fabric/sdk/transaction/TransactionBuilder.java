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

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.protos.common.Common;
import org.hyperledger.fabric.protos.peer.ProposalPackage.ChaincodeProposalPayload;
import org.hyperledger.fabric.protos.peer.ProposalPackage.Proposal;
import org.hyperledger.fabric.protos.peer.ProposalResponsePackage.Endorsement;
import org.hyperledger.fabric.protos.peer.TransactionPackage.ChaincodeActionPayload;
import org.hyperledger.fabric.protos.peer.TransactionPackage.ChaincodeEndorsedAction;
import org.hyperledger.fabric.protos.peer.TransactionPackage.Transaction;
import org.hyperledger.fabric.protos.peer.TransactionPackage.TransactionAction;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;


public class TransactionBuilder {

    private final static Log logger = LogFactory.getLog(TransactionBuilder.class);
    private Proposal chaincodeProposal;
    private Collection<Endorsement> endorsements;
    private ByteString proposalResponsePayload;

    public static TransactionBuilder newBuilder() {
        return new TransactionBuilder();
    }

    public TransactionBuilder chaincodeProposal(Proposal chaincodeProposal) {
        this.chaincodeProposal = chaincodeProposal;
        return this;
    }

    public TransactionBuilder endorsements(Collection<Endorsement> endorsements) {
        this.endorsements = endorsements;
        return this;
    }

    public TransactionBuilder proposalResponsePayload(ByteString proposalResponsePayload) {
        this.proposalResponsePayload = proposalResponsePayload;
        return this;
    }


    public Common.Payload build() throws InvalidProtocolBufferException {

        return createTransactionCommonPayload(chaincodeProposal, proposalResponsePayload, endorsements);

    }


    private Common.Payload createTransactionCommonPayload(Proposal chaincodeProposal,  ByteString proposalResponsePayload,
                                                          Collection<Endorsement> endorsements) throws InvalidProtocolBufferException {


        ChaincodeEndorsedAction.Builder chaincodeEndorsedActionBuilder = ChaincodeEndorsedAction.newBuilder();
        chaincodeEndorsedActionBuilder.setProposalResponsePayload(proposalResponsePayload);
        chaincodeEndorsedActionBuilder.addAllEndorsements(endorsements);

        //ChaincodeActionPayload
        ChaincodeActionPayload.Builder chaincodeActionPayloadBuilder = ChaincodeActionPayload.newBuilder();
        chaincodeActionPayloadBuilder.setAction(chaincodeEndorsedActionBuilder.build());

        //We need to remove any transient fields - they are not part of what the peer uses to calculate hash.
        ChaincodeProposalPayload.Builder chaincodeProposalPayloadNoTransBuilder = ChaincodeProposalPayload.newBuilder();
        chaincodeProposalPayloadNoTransBuilder.mergeFrom(chaincodeProposal.getPayload());
       // chaincodeProposalPayloadNoTransBuilder.clearTransient();

       // chaincodeActionPayloadBuilder.setChaincodeProposalPayload(chaincodeProposalPayloadNoTransBuilder.build().toByteString());
        chaincodeActionPayloadBuilder.setChaincodeProposalPayload(chaincodeProposal.getPayload());

        TransactionAction.Builder transactionActionBuilder = TransactionAction.newBuilder();

        Common.Header header = Common.Header.parseFrom(chaincodeProposal.getHeader());

        logger.trace("transaction header bytes:" + Arrays.toString(header.toByteArray()));
        logger.trace("transaction header sig bytes:" + Arrays.toString(header.getSignatureHeader().toByteArray()));

        transactionActionBuilder.setHeader(header.getSignatureHeader());

        ChaincodeActionPayload chaincodeActionPayload = chaincodeActionPayloadBuilder.build();
        logger.trace("transactionActionBuilder.setPayload" + Arrays.toString(chaincodeActionPayload.toByteString().toByteArray()));
        transactionActionBuilder.setPayload(chaincodeActionPayload.toByteString());

        //Transaction
        Transaction.Builder transactionBuilder = Transaction.newBuilder();
        transactionBuilder.addActions(transactionActionBuilder.build());


        Common.Payload.Builder payload = Common.Payload.newBuilder();
        payload.setHeader(header);
        payload.setData(transactionBuilder.build().toByteString());

        return payload.build();


    }

}
