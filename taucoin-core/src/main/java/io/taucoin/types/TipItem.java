package io.taucoin.types;

import java.util.ArrayList;
import java.util.List;

public class TipItem extends HashList {

    public TipItem(List<byte[]> hashList) {
        super(hashList);
    }

    public TipItem(byte[] encode) {
        super(encode);
    }

    public static TipItem with(byte[] blockHash, byte[] txHash) {
        List<byte[]> list = new ArrayList<>();
        list.add(blockHash);
        list.add(txHash);

        return new TipItem(list);
    }

    /**
     * 是否合法
     * @return true:合法，false:不合法
     */
    public boolean validate() {
        List<byte[]> list = getHashList();
        return null != list && list.size() == 2;
    }

    /**
     * 获取block hash
     * @return block hash
     */
    public byte[] getBlockHash() {
        return getHashList().get(0);
    }

    /**
     * 获取tx hash
     * @return tx hash
     */
    public byte[] getTxHash() {
        return getHashList().get(1);
    }

}
