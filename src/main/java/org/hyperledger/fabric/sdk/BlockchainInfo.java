package org.hyperledger.fabric.sdk;

/**
 * Wrapper for fabric BlockchainInfo proto.
 * Contains information about the blockchain ledger.
 */
public class BlockchainInfo {

    private long height;
    private byte[] currentBlockHash;
    private byte[] previousBlockHash;

    public BlockchainInfo(long height, byte[] currentBlockHash, byte[] previousBlockHash) {
        this.height = height;
        this.currentBlockHash = currentBlockHash;
        this.previousBlockHash = previousBlockHash;
    }

    public long getHeight() {
        return height;
    }

    public byte[] getCurrentBlockHash() {
        return currentBlockHash;
    }

    public byte[] getPreviousBlockHash() {
        return previousBlockHash;
    }
}
