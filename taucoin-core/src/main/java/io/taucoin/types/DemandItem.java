package io.taucoin.types;

import java.util.ArrayList;
import java.util.List;

public class DemandItem extends HashList {

    public DemandItem(List<byte[]> hashList) {
        super(hashList);
    }

    public DemandItem(byte[] encode) {
        super(encode);
    }

    public static DemandItem with(LocalDemand localDemand) {
        List<byte[]> list = new ArrayList<>();
        list.add(localDemand.getBlockHash());
        list.add(localDemand.getTxHash());
        list.add(localDemand.getHorizontalHash());
        list.add(localDemand.getVerticalHash());

        return new DemandItem(list);
    }

    public static DemandItem with(byte[] blockHash, byte[] txHash, byte[] horizontalHash, byte[] verticalHash) {
        List<byte[]> list = new ArrayList<>();
        list.add(blockHash);
        list.add(txHash);
        list.add(horizontalHash);
        list.add(verticalHash);

        return new DemandItem(list);
    }

    /**
     * 是否合法
     * @return true:合法，false:不合法
     */
    public boolean validate() {
        List<byte[]> list = getHashList();
        return null != list && list.size() == 4;
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

    /**
     * 获取horizontal hash
     * @return horizontal hash
     */
    public byte[] getHorizontalHash() {
        return getHashList().get(2);
    }

    /**
     * 获取vertical hash
     * @return vertical hash
     */
    public byte[] getVerticalHash() {
        return getHashList().get(3);
    }

}
