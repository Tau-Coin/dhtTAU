package io.taucoin.core;

import io.taucoin.util.ByteArrayWrapper;

public class DataIdentifier {
    ByteArrayWrapper chainID;
    DataType dataType;
    // extra info can be block hash, tx hash, pubKey, the block hash to which this item belongs or its own hash etc.
    ByteArrayWrapper extraInfo1;
    ByteArrayWrapper extraInfo2;

    public DataIdentifier(DataType dataType) {
        this.dataType = dataType;
    }

    public DataIdentifier(DataType dataType, ByteArrayWrapper extraInfo1) {
        this.dataType = dataType;
        this.extraInfo1 = extraInfo1;
    }

    public DataIdentifier(DataType dataType, ByteArrayWrapper extraInfo1, ByteArrayWrapper extraInfo2) {
        this.dataType = dataType;
        this.extraInfo1 = extraInfo1;
        this.extraInfo2 = extraInfo2;
    }

    public DataIdentifier(ByteArrayWrapper chainID, DataType dataType) {
        this.chainID = chainID;
        this.dataType = dataType;
    }

    public DataIdentifier(ByteArrayWrapper chainID, DataType dataType, ByteArrayWrapper extraInfo1) {
        this.chainID = chainID;
        this.dataType = dataType;
        this.extraInfo1 = extraInfo1;
    }

    public DataIdentifier(ByteArrayWrapper chainID, DataType dataType, ByteArrayWrapper extraInfo1, ByteArrayWrapper extraInfo2) {
        this.chainID = chainID;
        this.dataType = dataType;
        this.extraInfo1 = extraInfo1;
        this.extraInfo2 = extraInfo2;
    }

    public ByteArrayWrapper getChainID() {
        return chainID;
    }

    public DataType getDataType() {
        return dataType;
    }

    public ByteArrayWrapper getExtraInfo1() {
        return extraInfo1;
    }

    public ByteArrayWrapper getExtraInfo2() {
        return extraInfo2;
    }
}
