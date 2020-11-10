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
     * make tip salt
     * @param chainID chain ID
     * @return tip salt
     */
    public static byte[] makeTipSalt(byte[] chainID) {
        byte[] salt = new byte[chainID.length + ChainParam.TIP_CHANNEL.length];

        System.arraycopy(chainID, 0, salt, 0, chainID.length);
        System.arraycopy(ChainParam.TIP_CHANNEL, 0, salt, chainID.length,
                ChainParam.TIP_CHANNEL.length);

        return salt;
    }

    /**
     * make personal info salt
     * @param chainID chain ID
     * @return personal info salt
     */
    public static byte[] makePersonalInfoSalt(byte[] chainID) {
        byte[] salt = new byte[chainID.length + ChainParam.PERSONAL_INFO_CHANNEL.length];

        System.arraycopy(chainID, 0, salt, 0, chainID.length);
        System.arraycopy(ChainParam.PERSONAL_INFO_CHANNEL, 0, salt, chainID.length,
                ChainParam.PERSONAL_INFO_CHANNEL.length);

        return salt;
    }

}
