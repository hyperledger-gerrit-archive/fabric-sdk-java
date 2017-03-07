/*
 *  Copyright 2016,2017 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hyperledger.fabric.sdk;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.protos.common.Common.Block;
import org.hyperledger.fabric.protos.common.Common.BlockData;
import org.hyperledger.fabric.protos.common.Common.BlockMetadata;
import org.hyperledger.fabric.protos.common.Common.Envelope;
import org.hyperledger.fabric.protos.peer.FabricTransaction.Transaction;
import org.hyperledger.fabric.protos.peer.FabricTransaction.TxValidationCode;
import org.hyperledger.fabric.sdk.exception.InvalidProtocolBufferRuntimeException;

import static org.hyperledger.fabric.protos.peer.PeerEvents.Event;

/**
 * A wrapper for the Block returned in an Event
 *
 * @see Block
 */
public class BlockEvent {
    private static final Log logger = LogFactory.getLog(BlockEvent.class);

    private final Block block;
    private final BlockDeserializer blockdes;
    //private final BlockDeserializer blockdes;

    /**
     * Get eventhub that received the event
     *
     * @return eventhub that received the event
     */

    public EventHub getEventHub() {
        return eventHub;
    }

    private final EventHub eventHub;
    private final Event event;
    private final BlockMetadata blockMetadata;

    private BlockData blockData;

    private String channelID;  // TODO a block contains payloads from a single channel ??????
    private final ArrayList<TransactionEvent> txList = new ArrayList<>();
    private int transactionsInBlock;

    public Event getEvent() {
        return event;
    }

    /**
     * creates a BlockEvent object by parsing the input Block and retrieving its constituent Transactions
     *
     * @param eventHub a Hyperledger Fabric Block message
     * @throws InvalidProtocolBufferException
     * @see Block
     */
    BlockEvent(EventHub eventHub, Event event) throws InvalidProtocolBufferException {
        try {
            this.event = event;
            this.block = event.getBlock();
            this.blockdes = new BlockDeserializer(this.block);

            this.eventHub = eventHub;
            blockMetadata = this.block.getMetadata();
            getChannelIDFromBlock();

            //      populateResultsMap();
            processTransactions();
        } catch (InvalidProtocolBufferRuntimeException e) {
            logger.error("blockeven ",e);
            throw  e.getCause();
        }
    }

    /**
     * getChannelIDFromBlock retrieves the channel ID from the Block by parsing
     * the header of the first transaction in the block
     *
     * @throws InvalidProtocolBufferException
     */
    private void getChannelIDFromBlock() throws InvalidProtocolBufferException {
        channelID = blockdes.getData(0).getPayload().getHeader().getChannelHeader().getChannelId();
        blockData = blockdes.getData();
//        ByteString data = blockData.getData(0);
//        Envelope envelope = Envelope.parseFrom(data);
//        Payload payload = Payload.parseFrom(envelope.getPayload());
//        Header plh = payload.getHeader();
//        ChannelHeader channelHeader = ChannelHeader.parseFrom(plh.getChannelHeader());
//        channelID = channelHeader.getChannelId();
    }

//    /**
//     * populateResultsMap parses the Block and retrieves the bit string that lists the transaction results
//     */
//    private void populateResultsMap() {
//        ByteString txResultsBytes = blockMetadata.getMetadata(BlockMetadataIndex.TRANSACTIONS_FILTER_VALUE);
//        transActionsMetaData = txResultsBytes.toByteArray();
//    }

    /**
     * processTransactions retrieves the Transactions from the Block and wrappers each into
     * its own TransactionEvent
     *
     * @throws InvalidProtocolBufferException
     * @see Block
     * @see Transaction
     * @see TransactionEvent
     */
    private void processTransactions() throws InvalidProtocolBufferException {
        int blockIndex = -1;
        transactionsInBlock = blockData.getDataCount();
        for (ByteString db : blockData.getDataList()) {
            blockIndex++;
            Envelope env = Envelope.parseFrom(db);
            txList.add(new TransactionEvent(blockIndex));
        }
    }

    /**
     * @return the Block associated with this BlockEvent
     */
    public Block getBlock() {
        return block;
    }

    /**
     * @return the channel ID from the Block
     */
    public String getChannelID() {
        return channelID;
    }

    /**
     * @return a List of the TransactionEvents contained in this Block
     */
    public List<BlockEvent.TransactionEvent> getTransactionEvents() {
        return txList;
    }

    /**
     * A wrapper of a Transaction contained in the Block of this event.
     */
    public class TransactionEvent {
        private final int txIndex;
//        private final Block enclosingBlock;
//        private final Envelope txEnvelope;
        private final String txID;
        private final EnvelopeDeserializer ed;

        /**
         * constructs a TransactionEvent by parsing the given Envelope
         *
         * @param index      the position of this Transaction in the Block
         * @throws InvalidProtocolBufferException
         */
        TransactionEvent(int index) throws InvalidProtocolBufferException {
            txIndex = index;
            ed = blockdes.getData(txIndex);

           // this.enclosingBlock = block;
//            this.txEnvelope = txEnvelope;
//             Payload payload = Payload.parseFrom(blockdes.getData(txIndex).getEnvelope().getPayload());
//            Header plh = payload.getHeader();
          //  ChannelHeader channelHeader = ChannelHeader.parseFrom(plh.getChannelHeader());
            txID = ed.getPayload().getHeader().getChannelHeader().getTxId();
//// NEW....................
//
//            //   ByteString bdb = payload.getData();
////            String ho = Hex.encodeHexString(bdb.toByteArray());
////            System.out.println(ho);
//
//            Transaction tx = Transaction.parseFrom(payload.getData());
//            List<FabricTransaction.TransactionAction> al = tx.getActionsList();
//            for (FabricTransaction.TransactionAction ta : al) {
//
//
////                Common.SignatureHeader sh = Common.SignatureHeader.parseFrom(ta.getHeader());
////                ByteString nob = sh.getNonce();
////                String nobs = nob.toStringUtf8();
////
//
//
//
//                //         FabricTransaction.ChaincodeActionPayload tap = ta.getHeader();
//
//                FabricTransaction.ChaincodeActionPayload tap = FabricTransaction.ChaincodeActionPayload.parseFrom(ta.getPayload());//<<<
//                FabricProposal.ChaincodeProposalPayload ccpp = FabricProposal.ChaincodeProposalPayload.parseFrom(tap.getChaincodeProposalPayload());
//                Chaincode.ChaincodeInput cinput = Chaincode.ChaincodeInput.parseFrom(ccpp.getInput());
//
//                for (ByteString x : cinput.getArgsList()) {
//
//                    System.out.println("x " + x);
//
//                }
//
//                FabricTransaction.ChaincodeEndorsedAction cae = tap.getAction();
//
//                // FabricProposalResponse.ProposalResponsePayload cpr = FabricProposalResponse.ProposalResponsePayload.parseFrom(cae.getProposalResponsePayload());
//                FabricProposalResponse.ProposalResponsePayload cpr = FabricProposalResponse.ProposalResponsePayload.parseFrom(cae.getProposalResponsePayload());
//                FabricProposal.ChaincodeAction ca = FabricProposal.ChaincodeAction.parseFrom(cpr.getExtension());
//
//                FabricProposalResponse.Response rsp = ca.getResponse();
//                System.out.println(String.format(" resp message= %s,  status=%d", new String(rsp.getPayload().toByteArray()), rsp.getStatus()));
//
//                ByteString rwset = ca.getResults();  ///<<<<<<<<<<<<<<
//
//                FabricProposalResponse.Response a = ca.getResponse();
//
//                //cae.getProposalResponsePayload();r
//                System.out.println("rwset:'" + Hex.encodeHexString(rwset.toByteArray()) + "'");
//
//            }
//
//            /*
//            ChaincodeEndorsedAction.getAction
//            ProposalResponsePayload
//               ProposalResponsePayload.getExtension
//               ChaincodeAction.getResults()
//             */
//
////
////            FabricProposal.Proposal sp = FabricProposal.Proposal.parseFrom(bdb);
////            Header ph = Header.parseFrom(sp.getHeader());
////
////            ChannelHeader pch = ChannelHeader.parseFrom(ph.getChannelHeader());
//
        }

        /**
         * @return the transaction ID
         */
        public String getTransactionID() {
            return this.txID;
        }

//        /**
//         * @return the Envelope wrapper of this Transaction payload
//         */
//        public Envelope getEnvelope() {
//            return this.txEnvelope;
//        }

        /**
         * @return the Block that contains this Transaction
         */
        public Block getBlock() {
            return blockdes.getBlock();
        }

        /**
         * @return the position of this Transaction in the Block
         */
        public int getIndexInBlock() {
            return txIndex;
        }

        /**
         * @return whether this Transaction is marked as TxValidationCode.VALID
         */
        public boolean isValid() {
            if (txIndex >= transactionsInBlock) {
                return false;
            }
            byte txResult = blockdes.getTransActionsMetaData()[txIndex];
            logger.debug("TxID " + this.txID + " txResult = " + txResult);

            return txResult == TxValidationCode.VALID_VALUE;
        }

        /**
         * @return the validation code of this Transaction (enumeration TxValidationCode in Transaction.proto)
         */
        public byte validationCode() {
            if (txIndex >= transactionsInBlock) {
                return (byte) TxValidationCode.INVALID_OTHER_REASON_VALUE;
            }
            return blockdes.getTransActionsMetaData()[txIndex];
        }

        /**
         * Return eventhub that received the event
         *
         * @return
         */

        public EventHub getEventHub() {
            return BlockEvent.this.getEventHub();
        }
    } // TransactionEvent

} // BlockEvent
