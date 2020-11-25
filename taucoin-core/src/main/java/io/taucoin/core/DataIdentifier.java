package io.taucoin.core;

import io.taucoin.util.ByteArrayWrapper;

public class DataIdentifier {
    ByteArrayWrapper chainID;
    DataType dataType;
    ByteArrayWrapper hash; // block hash, txid or pubKey
    ByteArrayWrapper blockHash; // the block hash to which this item belongs

    public DataIdentifier(DataType dataType) {
        this.dataType = dataType;
    }

    public DataIdentifier(ByteArrayWrapper chainID, DataType dataType) {
        this.chainID = chainID;
        this.dataType = dataType;
    }

    public DataIdentifier(ByteArrayWrapper chainID, DataType dataType, ByteArrayWrapper hash) {
        this.chainID = chainID;
        this.dataType = dataType;
        this.hash = hash;
    }

    public DataIdentifier(ByteArrayWrapper chainID, DataType dataType, ByteArrayWrapper hash, ByteArrayWrapper blockHash) {
        this.chainID = chainID;
        this.dataType = dataType;
        this.hash = hash;
        this.blockHash = blockHash;
    }

    public ByteArrayWrapper getChainID() {
        return chainID;
    }

    public DataType getDataType() {
        return dataType;
    }

    public ByteArrayWrapper getHash() {
        return hash;
    }

    public ByteArrayWrapper getBlockHash() {
        return blockHash;
    }
}
