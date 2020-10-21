package io.taucoin.types;

import java.util.ArrayList;
import java.util.List;

public class VerticalItem extends HashList {
    public VerticalItem(List<byte[]> hashList) {
        super(hashList);
    }

    public VerticalItem(byte[] encode) {
        super(encode);
    }

    /**
     * create vertical item with previous block hash
     * @param previousHash previous block hash
     * @return vertical item
     */
    public static VerticalItem with(byte[] previousHash) {
        List<byte[]> list = new ArrayList<>();
        list.add(previousHash);

        return new VerticalItem(list);
    }

    /**
     * get previous hash
     * previous hash放在第一个位置
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
