package io.taucoin.core;

public class DataIdentifier {
    byte[] chainID;
    DataType dataType;

    public DataIdentifier(byte[] chainID, DataType dataType) {
        this.chainID = chainID;
        this.dataType = dataType;
    }

    public byte[] getChainID() {
        return chainID;
    }

    public DataType getDataType() {
        return dataType;
    }
}
