package io.taucoin.types;

import java.math.BigInteger;

import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class MutableDataWrapper {
    private MutableDataType mutableDataType;
    private byte[] data;

    private byte[] encode; // 缓存编码
    private boolean parsed = false; // 是否解码标志

    public MutableDataWrapper(MutableDataType mutableDataType, byte[] data) {
        this.mutableDataType = mutableDataType;
        this.data = data;
    }

    public MutableDataWrapper(byte[] encode) {
        this.encode = encode;
    }

    public MutableDataType getMutableDataType() {
        if (!this.parsed) {
            parseRLP();
        }

        return mutableDataType;
    }

    public byte[] getData() {
        if (!this.parsed) {
            parseRLP();
        }

        return data;
    }

    private void parseRLP() {
        RLPList params = RLP.decode2(this.encode);
        RLPList list = (RLPList) params.get(0);

        byte[] typeBytes = list.get(0).getRLPData();
        int typeNum = null == typeBytes ? 0: new BigInteger(1, typeBytes).intValue();
        if (typeNum >= MutableDataType.UNKNOWN.ordinal()) {
            this.mutableDataType = MutableDataType.UNKNOWN;
        } else {
            this.mutableDataType = MutableDataType.values()[typeNum];
        }

        this.data = list.get(1).getRLPData();

        this.parsed = true;
    }

    public byte[] getEncoded() {
        if (null == this.encode) {
            byte[] type = RLP.encodeBigInteger(BigInteger.valueOf(this.mutableDataType.ordinal()));
            byte[] data = RLP.encodeElement(this.data);

            this.encode = RLP.encodeList(type, data);
        }

        return this.encode;
    }
}
