package io.taucoin.core;

import io.taucoin.util.ByteArrayWrapper;

public class DataIdentifier {
    ByteArrayWrapper chainID;
    DataType dataType;
    ByteArrayWrapper hash;

    public DataIdentifier(ByteArrayWrapper chainID, DataType dataType) {
        this.chainID = chainID;
        this.dataType = dataType;
    }

    public DataIdentifier(ByteArrayWrapper chainID, DataType dataType, ByteArrayWrapper hash) {
        this.chainID = chainID;
        this.dataType = dataType;
        this.hash = hash;
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
}
