/*
 *  Copyright 2016,2017 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hyperledger.fabric.sdk;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;
import org.hyperledger.fabric.protos.common.Common.Block;
import org.hyperledger.fabric.protos.ledger.rwset.Rwset.TxReadWriteSet;
import org.hyperledger.fabric.sdk.exception.InvalidProtocolBufferRuntimeException;
import org.hyperledger.fabric.sdk.transaction.ProtoUtils;

import static org.hyperledger.fabric.protos.peer.FabricProposalResponse.Endorsement;

/**
 * BlockInfo contains the data from a {@link Block}
 */
public class BlockInfo {
    private final BlockDeserializer block;

    BlockInfo(Block block) {
        this.block = new BlockDeserializer(block);
    }

    /**
     * @return the raw {@link Block}
     */
    public Block getBlock() {
        return block.getBlock();
    }

    /**
     * @return the {@link Block} previousHash value
     */
    public byte[] getPreviousHash() {
        return block.getPreviousHash().toByteArray();
    }

    /**
     * @return the {@link Block} data hash value
     */
    public byte[] getDataHash() {

        return block.getDataHash().toByteArray();
    }

    /**
     * @return the {@link Block} tansaction metadata value
     */
    public byte[] getTransActionsMetaData() {

        return block.getTransActionsMetaData();
    }

    /**
     * @return the {@link Block} index number
     */
    public long getBlockNumber() {
        return block.getNumber();
    }

    /**
     * getEnvelopCount
     *
     * @return the number of transactions in this block.
     */

    public int getEnvelopCount() {
        return block.getData().getDataCount();
    }

//    /**
//     * Get transaction info on a specific transaction.
//     *
//     * @param index index into the block.
//     * @return Transaction Info
//     * @throws InvalidProtocolBufferException
//     */

//    public TansactionEnvelopeInfo getEnvelopeInfo(int index) throws InvalidProtocolBufferException {
//
//        try {
//            // block.getData(0).getEnvelope().getSignature();
//
//            EnvelopeDeserializer.newInstance(block.getBlock().getData().getData(index));
//
//            final PayloadDeserializer payload = block.getData(index).getPayload();
//
//            return new TansactionEnvelopeInfo(null, payload.getHeader());
//        } catch (InvalidProtocolBufferRuntimeException e) {
//            throw (InvalidProtocolBufferException) e.getCause();
//        }
//
//    }
//

    public class EnvelopeInfo {
        HeaderDeserializer headerDeserializer;

        //private final EnvelopeDeserializer envelopeDeserializer;

        EnvelopeInfo(EnvelopeDeserializer envelopeDeserializer) {
            //     this.envelopeDeserializer = envelopeDeserializer;
            headerDeserializer = envelopeDeserializer.getPayload().getHeader();
            headerDeserializer.getChannelHeader().getType();
        }

        public String getChannelId() {

            return headerDeserializer.getChannelHeader().getChannelId();
        }

        public String getTxId() {

            return headerDeserializer.getChannelHeader().getTxId();
        }

        public long getEpoch() {

            return headerDeserializer.getChannelHeader().getEpoch();
        }

        public Date getTimestamp() {

            return ProtoUtils.getDateFromTimestamp(headerDeserializer.getChannelHeader().getTimestamp());
        }

        public EnvelopeType getType() {

            switch (headerDeserializer.getChannelHeader().getType()) {
                case 3:
                    return EnvelopeType.TRANSACTION_ENVELOPE_INFO;

                default:
                    return EnvelopeType.ENVELOPE_INFO;
            }

        }

    }

    public EnvelopeInfo getEnvelopeInfo(int index) throws InvalidProtocolBufferException {

        try {
            // block.getData(0).getEnvelope().getSignature();

            EnvelopeInfo ret = null;

            EnvelopeDeserializer ed = EnvelopeDeserializer.newInstance(block.getBlock().getData().getData(index));

            switch (ed.getType()) {
                case 3:
                    ret = new TansactionEnvelopeInfo((EndorserTransactionEnvDeserializer) ed);
                    break;
                default: //just assume base properties.
                    ret = new EnvelopeInfo(ed);
                    break;
            }
            return ret;

        } catch (InvalidProtocolBufferRuntimeException e) {
            throw (InvalidProtocolBufferException) e.getCause();
        }

    }

    public Iterable<EnvelopeInfo> getEnvelopeInfos() {

        return new TransactionInfoIterable();
    }

    public class TansactionEnvelopeInfo extends EnvelopeInfo {

        private final EndorserTransactionEnvDeserializer transactionDeserializer;

        public TansactionEnvelopeInfo(EndorserTransactionEnvDeserializer transactionDeserializer) {
            super(transactionDeserializer);

            this.transactionDeserializer = transactionDeserializer;
            this.headerDeserializer = transactionDeserializer.getPayload().getHeader();

        }

        public int getTransactionActionInfoCount() {
            return transactionDeserializer.getPayload().getTransaction().getActionsCount();
        }

        public Iterable<TransactionActionInfo> getTransactionActionInfos() {

            return new TransactionActionIterable();
        }

        public class TransactionActionInfo {

            private final TransactionActionDeserializer transactionAction;
            List<EndorserInfo> endorserInfos = null;

            TransactionActionInfo(TransactionActionDeserializer transactionAction) {

                this.transactionAction = transactionAction;
            }

            public byte[] getResponseMessageBytes() {
                return transactionAction.getPayload().getAction().getProposalResponsePayload().getExtension().getResponseMessageBytes();
            }

            public String getResponseMessage() {
                return transactionAction.getPayload().getAction().getProposalResponsePayload().getExtension().getResponseMessage();
            }

            public int getResponseStatus() {
                return transactionAction.getPayload().getAction().getProposalResponsePayload().getExtension().getResponseStatus();
            }

            int getEndorsementsCount = -1;

            public int getEndorsementsCount() {
                if (getEndorsementsCount < 0) {
                    getEndorsementsCount = transactionAction.getPayload().getAction().getEndorsementsCount();
                }
                return getEndorsementsCount;
            }

            public EndorserInfo getEndorsementInfo(int index) {
                if (null == endorserInfos) {
                    endorserInfos = new ArrayList<>();

                    for (Endorsement endorsement : transactionAction.getPayload().getAction()
                            .getChaincodeEndorsedAction().getEndorsementsList()) {

                        endorserInfos.add(new EndorserInfo(endorsement));

                    }
                }
                return endorserInfos.get(index);

            }

            public TxReadWriteSetInfo getTxReadWriteSet() {

                TxReadWriteSet txReadWriteSet = transactionAction.getPayload().getAction().getProposalResponsePayload()
                        .getExtension().getResults();
                if (txReadWriteSet == null) {
                    return null;
                }

                return new TxReadWriteSetInfo(txReadWriteSet);

            }

        }

        public TransactionActionInfo getTransactionActionInfo(int index) {
            return new TransactionActionInfo(transactionDeserializer.getPayload().getTransaction().getTransactionAction(index));
        }

        public class TransactionActionInfoIterator implements Iterator<TransactionActionInfo> {
            int ci = 0;
            final int max;

            TransactionActionInfoIterator() {
                max = getTransactionActionInfoCount();

            }

            @Override
            public boolean hasNext() {
                return ci < max;

            }

            @Override
            public TransactionActionInfo next() {

                return getTransactionActionInfo(ci++);

            }
        }

        public class TransactionActionIterable implements Iterable<TransactionActionInfo> {

            @Override
            public Iterator<TransactionActionInfo> iterator() {
                return new TransactionActionInfoIterator();
            }
        }

    }

    /**
     * Iterable interface over transaction info in the block.
     *
     * @return iterator
     */

    public Iterable<EnvelopeInfo> getTransactionActionInfos() {

        return new TransactionInfoIterable();

    }

    class TransactionInfoIterator implements Iterator<EnvelopeInfo> {
        int ci = 0;
        final int max;

        TransactionInfoIterator() {
            max = block.getData().getDataCount();

        }

        @Override
        public boolean hasNext() {
            return ci < max;

        }

        @Override
        public EnvelopeInfo next() {

            try {
                return getEnvelopeInfo(ci++);
            } catch (InvalidProtocolBufferException e) {
                throw new InvalidProtocolBufferRuntimeException(e);
            }

        }
    }

    class TransactionInfoIterable implements Iterable<EnvelopeInfo> {

        @Override
        public Iterator<EnvelopeInfo> iterator() {
            return new TransactionInfoIterator();
        }
    }

    public static class EndorserInfo {
        private Endorsement endorsement;

        EndorserInfo(Endorsement endorsement) {

            this.endorsement = endorsement;
        }

        public byte[] getSignature() {
            return endorsement.getSignature().toByteArray();
        }

        public byte[] getEndorser() {
            return endorsement.getEndorser().toByteArray();
        }

    }

    public enum EnvelopeType {

        TRANSACTION_ENVELOPE_INFO,
        ENVELOPE_INFO

    }

}


