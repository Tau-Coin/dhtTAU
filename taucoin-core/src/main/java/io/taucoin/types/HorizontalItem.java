package io.taucoin.types;

import java.util.ArrayList;
import java.util.List;

public class HorizontalItem extends HashList {
    public HorizontalItem(List<byte[]> hashList) {
        super(hashList);
    }

    public HorizontalItem(byte[] encode) {
        super(encode);
    }

    /**
     * create horizontal item with tx hash
     * @param txHash tx hash
     * @return horizontal item
     */
    public static HorizontalItem with(byte[] txHash) {
        List<byte[]> list = new ArrayList<>();
        list.add(txHash);

        return new HorizontalItem(list);
    }

    /**
     * get tx hash
     * tx hash放在第一个位置
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
