package io.taucoin.types;

public class LocalDemand {
    private byte[] blockHash = null;
    private byte[] txHash = null;
    private byte[] horizontalHash = null;
    private byte[] verticalHash = null;

    public void clearBlockHash() {
        this.blockHash = null;
    }

    public void clearTxHash() {
        this.txHash = null;
    }

    public void clearHorizontalHash() {
        this.horizontalHash = null;
    }

    public void clearVerticalHash() {
        this.verticalHash = null;
    }

    public byte[] getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(byte[] blockHash) {
        this.blockHash = blockHash;
    }

    public byte[] getTxHash() {
        return txHash;
    }

    public void setTxHash(byte[] txHash) {
        this.txHash = txHash;
    }

    public byte[] getHorizontalHash() {
        return horizontalHash;
    }

    public void setHorizontalHash(byte[] horizontalHash) {
        this.horizontalHash = horizontalHash;
    }

    public byte[] getVerticalHash() {
        return verticalHash;
    }

    public void setVerticalHash(byte[] verticalHash) {
        this.verticalHash = verticalHash;
    }
}
