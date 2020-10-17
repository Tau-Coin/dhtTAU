package io.taucoin.types;

import java.util.List;

public class HorizontalItem extends HashList {
    public HorizontalItem(List<byte[]> hashList) {
        super(hashList);
    }

    public HorizontalItem(byte[] encode) {
        super(encode);
    }

    /**
     * get tx hash
     * @return tx hash
     */
    public byte[] getTxHash() {
        List<byte[]> hashList = getHashList();
        if (null != hashList && !hashList.isEmpty()) {
            return hashList.get(0);
        }

        return null;
    }
}
