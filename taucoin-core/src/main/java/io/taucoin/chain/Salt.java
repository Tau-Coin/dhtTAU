package io.taucoin.chain;

import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteUtil;

public class Salt {

    /**
     * make demand salt
     * @param chainID chain ID
     * @return demand salt
     */
    public static byte[] makeDemandSalt(byte[] chainID) {
        long time = System.currentTimeMillis() / 1000 / ChainParam.DEFAULT_BLOCK_TIME;
        byte[] timeBytes = ByteUtil.longToBytes(time);

        byte[] salt = new byte[chainID.length + ChainParam.DEMAND_CHANNEL.length + timeBytes.length];
        System.arraycopy(chainID, 0, salt, 0, chainID.length);
        System.arraycopy(ChainParam.DEMAND_CHANNEL, 0, salt, chainID.length,
                ChainParam.DEMAND_CHANNEL.length);
        System.arraycopy(timeBytes, 0, salt, chainID.length + ChainParam.DEMAND_CHANNEL.length, timeBytes.length);
        return salt;
    }

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
}
