/*
 *  Copyright 2016 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
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

package org.hyperledger.fabric.sdk;

import java.util.BitSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.protos.common.Common.Block;
import org.hyperledger.fabric.protos.common.Common.BlockMetadata;
import org.hyperledger.fabric.protos.common.Common.BlockMetadataIndex;
import org.hyperledger.fabric.protos.common.Common.Envelope;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * A wrapper for the Transaction event coming from the event hub.
 *
 */
public class CommittedTransaction {
    private static final Log logger = LogFactory.getLog(CommittedTransaction.class);

    //private static final int byteSize = 8 ; // TODO verify that Fabric will always use byte size 8 ( which is the Java default )
    private final int blockCount;
    private final String txID ;
    private final Block block;
    private final Envelope envelope;
    private final int blockDataIndex;
    private BitSet txResults ;

    public CommittedTransaction(String txID, Block block, Envelope envelope, int blockDataIndex) {

        /* TODO SDK controls the creation of this class. Check input args ?
        if (txID == null) throw new IllegalArgumentException("CommittedTransaction requires a transaction ID");
        if (block == null) throw new IllegalArgumentException("CommitedTransaction requires a org.hyperledger.fabric.protos.common.Common.Block");
        if (blockDataIndex < 0) throw new IllegalArgumentException("CommittedTransation requires a valid Block.BlockData index");
        */

        this.txID = txID ;
        this.block = block;
        this.envelope = envelope;
        this.blockDataIndex = blockDataIndex;
        blockCount = block.getData().getDataCount();
    }

        private void populateResultsBitMap() {
            if (txResults == null) {
                BlockMetadata blockMetadata = this.block.getMetadata();
                ByteString txBitmap = blockMetadata.getMetadata(BlockMetadataIndex.TRANSACTIONS_FILTER_VALUE);
                txResults = BitSet.valueOf(txBitmap.toByteArray());
            }
        }

    public String getTxID(){
        return this.txID;
    }

    public Envelope getEnvelope() {
        return envelope;
    }

    public int getIndexInBlock() {
        return this.blockDataIndex;
    }

    public boolean isValid() {
        if (blockDataIndex >= blockCount) {
            return false;
        }

        populateResultsBitMap() ;

        boolean bitValue = txResults.get(this.blockDataIndex);
        return !bitValue ;  // a 0 at bit blockDataIndex indicates the transaction is valid
    }
}
