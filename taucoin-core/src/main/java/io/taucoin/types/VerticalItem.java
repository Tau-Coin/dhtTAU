package io.taucoin.types;

import java.util.List;

public class VerticalItem extends HashList {
    public VerticalItem(List<byte[]> hashList) {
        super(hashList);
    }

    public VerticalItem(byte[] encode) {
        super(encode);
    }

    /**
     * get previous hash
     * @return previous block hash
     */
    public byte[] getPreviousHash() {
        List<byte[]> hashList = getHashList();
        if (null != hashList && !hashList.isEmpty()) {
            return hashList.get(0);
        }

        return null;
    }
}
