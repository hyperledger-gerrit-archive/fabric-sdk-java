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

import org.hyperledger.fabric.protos.common.Common.Block;
import org.hyperledger.fabric.protos.common.Common.BlockMetadata;
import org.hyperledger.fabric.protos.common.Common.BlockMetadataIndex;
import org.hyperledger.fabric.protos.common.Common.Envelope;

import com.google.protobuf.ByteString;

/**
 * A wrapper for the Transaction coming from the event hub.
 *
 */
public class CommittedTransaction {
    private static final int byteSize = 8 ;
    private final String txID ;
    private final Block block;
    private final Envelope envelope;
    private final int blockDataIndex;
    private final BlockMetadata blockMetadata;
    private final ByteString txBitmap ;
    private final byte[] txResults;

    public CommittedTransaction(String txID, Block block, Envelope envelope, int blockDataIndex) {
        this.txID = txID ;
        this.block = block;
        this.envelope = envelope;
        this.blockDataIndex = blockDataIndex;
        blockMetadata = this.block.getMetadata();
        txBitmap = blockMetadata.getMetadata(BlockMetadataIndex.TRANSACTIONS_FILTER_VALUE);
        txResults = txBitmap.toByteArray();
    }

    public String getTxID(){
        return this.txID;
    }

    public Envelope getEnvelope() {
        return envelope;
    }

    public boolean isInvalid() {
        int ix = blockDataIndex/byteSize;
        int x = 1 << blockDataIndex ;

        return (txResults[ix] & (1 << (x))) != 0 ;
    }
}
