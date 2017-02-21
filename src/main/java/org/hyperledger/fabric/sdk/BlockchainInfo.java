package org.hyperledger.fabric.sdk;

/**
 * Wrapper for fabric BlockchainInfo proto.
 * Contains information about the blockchain ledger.
 */
public class BlockchainInfo {

    private final long height;
    private final byte[] currentBlockHash;
    private final byte[] previousBlockHash;

    BlockchainInfo(long height, byte[] currentBlockHash, byte[] previousBlockHash) {
        this.height = height;
        this.currentBlockHash = currentBlockHash;
        this.previousBlockHash = previousBlockHash;
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
