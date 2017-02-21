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

import static org.junit.Assert.*;

import java.util.BitSet;

import org.hyperledger.fabric.protos.common.Common.Block;
import org.hyperledger.fabric.protos.common.Common.BlockData;
import org.hyperledger.fabric.protos.common.Common.BlockHeader;
import org.hyperledger.fabric.protos.common.Common.BlockMetadata;
import org.hyperledger.fabric.protos.common.Common.Envelope;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.protobuf.ByteString;

/**
 *
 */
public class CommittedTransactionTest {
    private static Block block ;
    private static BlockHeader blockHeader;
    private static BlockData blockData;
    private static BlockMetadata blockMetadata;
    private static Envelope envelope = null ;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        BlockData.Builder blockDataBuilder = BlockData.newBuilder();
        ByteString testData = ByteString.copyFrom("test data".getBytes());
        blockDataBuilder.addData(testData);
        blockDataBuilder.addData(testData);
        blockDataBuilder.addData(testData);
        blockData = blockDataBuilder.build();

        BlockHeader.Builder blockHeaderBuilder = BlockHeader.newBuilder();
        blockHeaderBuilder.setNumber(1);
        blockHeaderBuilder.setPreviousHash(ByteString.copyFrom("previous_hash".getBytes()));
        blockHeaderBuilder.setDataHash(ByteString.copyFrom("data_hash".getBytes()));
        blockHeader = blockHeaderBuilder.build();

        BlockMetadata.Builder blockMetadataBuilder = BlockMetadata.newBuilder();
        blockMetadataBuilder.addMetadata(ByteString.copyFrom("signatures".getBytes()));   //BlockMetadataIndex.SIGNATURES_VALUE
        blockMetadataBuilder.addMetadata(ByteString.copyFrom("last_config".getBytes()));  //BlockMetadataIndex.LAST_CONFIG_VALUE,
        // mark 2nd transaction in block as invalid
        BitSet txBitMap = new BitSet(3);
        txBitMap.set(1);
        blockMetadataBuilder.addMetadata(ByteString.copyFrom(txBitMap.toByteArray()));    //BlockMetadataIndex.TRANSACTIONS_FILTER_VALUE
        blockMetadataBuilder.addMetadata(ByteString.copyFrom("orderer".getBytes()));     //BlockMetadataIndex.ORDERER_VALUE
        blockMetadata = blockMetadataBuilder.build();

        Block.Builder blockBuilder = Block.newBuilder();
        blockBuilder.setData(blockData);
        blockBuilder.setHeader(blockHeader);
        blockBuilder.setMetadata(blockMetadata);
        block = blockBuilder.build();
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {

    }

    /**
     * Test method for {@link org.hyperledger.fabric.sdk.CommittedTransaction#CommittedTransaction(java.lang.String, org.hyperledger.fabric.protos.common.Common.Block, org.hyperledger.fabric.protos.common.Common.Envelope, int)}.
     */
    @Test
    public void testCommittedTransaction() {
        CommittedTransaction tx = new CommittedTransaction("tx1", block, envelope, 0);
        assertEquals(tx.getTxID(), "tx1");
        assertEquals(tx.getEnvelope(), envelope);
        assertEquals(tx.getIndexInBlock(), 0);
    }

    /**
     * Test method for {@link org.hyperledger.fabric.sdk.CommittedTransaction#isValid()}.
     * transaction number 2 should be invalid
     */
    @Test
    public void testIsValid() {
        CommittedTransaction tx = new CommittedTransaction("tx1", block, envelope, 0);
        assertTrue(tx.isValid()) ;
        CommittedTransaction tx1 = new CommittedTransaction("tx1", block, envelope, 1);
        assertFalse(tx1.isValid());
        CommittedTransaction tx2 = new CommittedTransaction("tx2", block, envelope, 2);
        assertTrue(tx2.isValid());
        // out of bounds. Should return not valid
        CommittedTransaction tx3 = new CommittedTransaction("tx3", block, envelope, 3);
        assertFalse(tx3.isValid());
    }

}
