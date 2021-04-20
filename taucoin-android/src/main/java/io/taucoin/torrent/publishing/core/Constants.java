package io.taucoin.torrent.publishing.core;

import java.math.BigInteger;

/**
 * core模块用到的所有常量定义类
 */
public class Constants {
    // 1 COIN
    public static final BigInteger COIN = new BigInteger("100", 10);
    // 默认社区链总共的coin值 10000000 COIN
    public static final BigInteger TOTAL_COIN = new BigInteger("10000000", 10).multiply(COIN);
    // 给朋友空投币的数量 10 COIN
    public static final BigInteger AIRDROP_COIN = new BigInteger("10", 10).multiply(COIN);
    // 最小值交易费 0.01 COIN
    public static final BigInteger MIN_FEE = new BigInteger("1", 10);

    // 默认社区链平均出块时间，单位:s
    public static final int BLOCK_IN_AVG = 300;
    // 社区简介长度限制
    public static final int LENGTH_LIMIT = 200;
    // APP分享URL
    public static final String APP_SHARE_URL = "https://taucoin.io/androidwallet.html";

    // Chain link中bs默认数
    public static final int CHAIN_LINK_BS_LIMIT = 10;

    public static final int ONLINE_HOURS = 12;

    // APP在前台运行时重新put immutable数据覆盖为10s, 单位：秒
    public static final int FOREGROUND_PUT_IMMUTABLE_DATA_TIME = 10;
    // 1、重新put immutable数据时间间隔10s
    // 2、APP在前台运行时的gossip时间片10s
    public static final long FOREGROUND_GOSSIP_INTERVAL = 10 * 1000;
    // APP在后台运行时的gossip时间片30s
    public static final long BACKGROUND_GOSSIP_INTERVAL = 30 * 1000;
}
