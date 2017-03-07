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
     * @return the {@link Block} index number
     */
    public long getBlockNumber() {
        return block.getNumber();
    }

    /**
     * getTransactionCount
     *
     * @return the number of transactions in this block.
     */

    public int getTransactionCount() {
        return block.getData().getDataCount();
    }

    /**
     * Get transaction info on a specific transaction.
     *
     * @param index index into the block.
     * @return Transacton Info
     * @throws InvalidProtocolBufferException
     */

    public TransactionInfo getTransactionInfo(int index) throws InvalidProtocolBufferException {

        try {
            // block.getData(0).getEnvelope().getSignature();

            final PayloadDeserializer payload = block.getData(index).getPayload();

            return new TransactionInfo(payload.getTransaction(), payload.getHeader());
        } catch (InvalidProtocolBufferRuntimeException e) {
            throw (InvalidProtocolBufferException) e.getCause();
        }

    }

    public Iterable<TransactionInfo> getTransactionInfos() {

        return new TransactionInfoIterable();
    }

    public class TransactionInfo {

        private final TransactionDeserializer transactionDeserializer;
        private final HeaderDeserializer headerDeserializer;

        TransactionInfo(TransactionDeserializer transactionDeserializer, HeaderDeserializer headerDeserializer) {

            this.transactionDeserializer = transactionDeserializer;
            this.headerDeserializer = headerDeserializer;

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

        public int getTransactionActionInfoCount() {
            return transactionDeserializer.getActionsCount();
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

                    for (Endorsement endorsement : transactionAction.getPayload().getAction().getChaincodeEndorsedAction().getEndorsementsList()) {

                        endorserInfos.add(new EndorserInfo(endorsement));

                    }
                }
                return endorserInfos.get(index);

            }

            public TxReadWriteSet getTxReadWriteSet() {

                return transactionAction.getPayload().getAction().getProposalResponsePayload().getExtension().getResults();

            }

        }

        public TransactionActionInfo getTransactionActionInfo(int index) {
            return new TransactionActionInfo(transactionDeserializer.getTransactionAction(index));
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

    public Iterable<TransactionInfo> getTransactionActionInfos() {

        return new TransactionInfoIterable();

    }

    class TransactionInfoIterator implements Iterator<TransactionInfo> {
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
        public TransactionInfo next() {

            try {
                return getTransactionInfo(ci++);
            } catch (InvalidProtocolBufferException e) {
                throw new InvalidProtocolBufferRuntimeException(e);
            }

        }
    }

    class TransactionInfoIterable implements Iterable<TransactionInfo> {

        @Override
        public Iterator<TransactionInfo> iterator() {
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

}


