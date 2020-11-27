package io.taucoin.core;

import io.taucoin.util.ByteArrayWrapper;

public class DataIdentifier {
    ByteArrayWrapper chainID;
    DataType dataType;
    ByteArrayWrapper key; // block hash, txid or pubKey
    ByteArrayWrapper blockHash; // the block hash to which this item belongs

    public DataIdentifier(DataType dataType) {
        this.dataType = dataType;
    }

    public DataIdentifier(DataType dataType, ByteArrayWrapper key) {
        this.dataType = dataType;
        this.key = key;
    }

    public DataIdentifier(ByteArrayWrapper chainID, DataType dataType) {
        this.chainID = chainID;
        this.dataType = dataType;
    }

    public DataIdentifier(ByteArrayWrapper chainID, DataType dataType, ByteArrayWrapper key) {
        this.chainID = chainID;
        this.dataType = dataType;
        this.key = key;
    }

    public DataIdentifier(ByteArrayWrapper chainID, DataType dataType, ByteArrayWrapper key, ByteArrayWrapper blockHash) {
        this.chainID = chainID;
        this.dataType = dataType;
        this.key = key;
        this.blockHash = blockHash;
    }

    public ByteArrayWrapper getChainID() {
        return chainID;
    }

    public DataType getDataType() {
        return dataType;
    }

    public ByteArrayWrapper getKey() {
        return key;
    }

    public ByteArrayWrapper getBlockHash() {
        return blockHash;
    }
}
