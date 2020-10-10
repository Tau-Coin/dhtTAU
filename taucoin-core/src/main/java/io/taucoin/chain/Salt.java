package io.taucoin.chain;

import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteUtil;

public class Salt {
    /**
     * make block tip salt
     * @param chainID chain ID
     * @return block tip salt
     */
    public static byte[] makeBlockTipSalt(byte[] chainID) {
        byte[] salt = new byte[chainID.length + ChainParam.BLOCK_TIP_CHANNEL.length];

        System.arraycopy(chainID, 0, salt, 0, chainID.length);
        System.arraycopy(ChainParam.BLOCK_TIP_CHANNEL, 0, salt, chainID.length,
                ChainParam.BLOCK_TIP_CHANNEL.length);

        return salt;
    }

    /**
     * make block demand salt
     * @param chainID chain ID
     * @return block demand salt
     */
    public static byte[] makeBlockDemandSalt(byte[] chainID) {
        long time = System.currentTimeMillis() / 1000 / ChainParam.DEFAULT_BLOCK_TIME;
        byte[] timeBytes = ByteUtil.longToBytes(time);

        byte[] salt = new byte[chainID.length + ChainParam.BLOCK_DEMAND_CHANNEL.length + timeBytes.length];
        System.arraycopy(chainID, 0, salt, 0, chainID.length);
        System.arraycopy(ChainParam.BLOCK_DEMAND_CHANNEL, 0, salt, chainID.length,
                ChainParam.BLOCK_DEMAND_CHANNEL.length);
        System.arraycopy(timeBytes, 0, salt, chainID.length + ChainParam.BLOCK_DEMAND_CHANNEL.length, timeBytes.length);
        return salt;
    }

    /**
     * make block response salt
     * @param chainID chain ID
     * @return block response salt
     */
    public static byte[] makeBlockResponseSalt(byte[] chainID, byte[] blockHash) {
        byte[] salt = new byte[chainID.length + ChainParam.BLOCK_RESPONSE_CHANNEL.length + 10];
        System.arraycopy(chainID, 0, salt, 0, chainID.length);
        System.arraycopy(ChainParam.BLOCK_RESPONSE_CHANNEL, 0, salt, chainID.length,
                ChainParam.BLOCK_RESPONSE_CHANNEL.length);
        System.arraycopy(blockHash, 0, salt, chainID.length + ChainParam.BLOCK_RESPONSE_CHANNEL.length, 10);

        return salt;
    }

    /**
     * make tx salt
     * @param chainID chain ID
     * @return tx tip salt
     */
    public static byte[] makeTxTipSalt(byte[] chainID) {
        byte[] salt = new byte[chainID.length + ChainParam.TX_TIP_CHANNEL.length];

        System.arraycopy(chainID, 0, salt, 0, chainID.length);
        System.arraycopy(ChainParam.TX_TIP_CHANNEL, 0, salt, chainID.length,
                ChainParam.TX_TIP_CHANNEL.length);

        return salt;
    }

    /**
     * make tx demand salt
     * @param chainID chain ID
     * @return tx demand salt
     */
    public static byte[] makeTxDemandSalt(byte[] chainID) {
        long time = System.currentTimeMillis() / 1000 / ChainParam.DEFAULT_BLOCK_TIME;
        byte[] timeBytes = ByteUtil.longToBytes(time);

        byte[] salt = new byte[chainID.length + ChainParam.TX_DEMAND_CHANNEL.length + timeBytes.length];
        System.arraycopy(chainID, 0, salt, 0, chainID.length);
        System.arraycopy(ChainParam.TX_DEMAND_CHANNEL, 0, salt, chainID.length,
                ChainParam.TX_DEMAND_CHANNEL.length);
        System.arraycopy(timeBytes, 0, salt, chainID.length + ChainParam.TX_DEMAND_CHANNEL.length, timeBytes.length);
        return salt;
    }
}
