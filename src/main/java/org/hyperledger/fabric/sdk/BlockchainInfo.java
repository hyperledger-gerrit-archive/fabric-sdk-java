/*
 *  Copyright 2016, 2017 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hyperledger.fabric.sdk;

import org.hyperledger.fabric.protos.common.Ledger;

/**
 * BlockchainInfo contains information about the blockchain ledger.
 */
public class BlockchainInfo {

    private final Ledger.BlockchainInfo blockchainInfo;
    private final long height;
    private final byte[] currentBlockHash;
    private final byte[] previousBlockHash;

    BlockchainInfo(Ledger.BlockchainInfo blockchainInfo) {
        this.blockchainInfo = blockchainInfo;
        this.height = blockchainInfo.getHeight();
        this.currentBlockHash = blockchainInfo.getCurrentBlockHash().toByteArray() ;
        this.previousBlockHash = blockchainInfo.getPreviousBlockHash().toByteArray() ;
    }

    /**
     * Gets the current ledger blocks height.
     *
     * @return the current ledger blocks height
     */
    public long getHeight() {
        return height;
    }

    /**
     * Gets the current block hash
     *
     * @return the current bloch hash
     */
    public byte[] getCurrentBlockHash() {
        return currentBlockHash;
    }

    /**
     * Gets the previous block hash
     *
     * @return the previous block hash
     */
    public byte[] getPreviousBlockHash() {
        return previousBlockHash;
    }
}

