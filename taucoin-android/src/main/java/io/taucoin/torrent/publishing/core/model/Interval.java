package io.taucoin.torrent.publishing.core.model;

/**
 * 时间间隔枚举类
 */
public enum Interval {
    // 单位s
    GOSSIP_FORE_METERED_MIN(10),          // APP在前台时计费网络时gossip的最小时间间隔
    GOSSIP_FORE_METERED_MAX(15),          // APP在前台时计费网络时gossip的最大时间间隔
    GOSSIP_FORE_WIFI_MIN(5),              // APP在前台时非计费网络时gossip的最大时间间隔
    GOSSIP_FORE_WIFI_MAX(10),             // APP在前台时非计费网络时gossip的最大时间间隔

    // 单位s
    GOSSIP_BACK_METERED_MIN(40),          // APP在后台时计费网络时gossip的最小时间间隔
    GOSSIP_BACK_METERED_MAX(60),          // APP在后台时计费网络时gossip的最大时间间隔
    GOSSIP_BACK_WIFI_MIN(20),             // APP在后台时非计费网络时gossip的最大时间间隔
    GOSSIP_BACK_WIFI_MAX(40),             // APP在后台时非计费网络时gossip的最大时间间隔

    // 单位ms
    MAIN_LOOP_MIN(50),                    // 链端主循环最小时间间隔
    MAIN_LOOP_MAX(1000),                  // 链端主循环最大时间间隔

    // 单位ms
    DHT_OP_MIN(100),                      // DHT put/get操作的最小时间间隔
    DHT_OP_MAX(10000),                    // DHT put/get操作的最大时间间隔

    // Worker中失败异常重试频率，单位ms
    INTERVAL_RETRY(1000);

    private int interval;
    Interval(int interval) {
        this.interval = interval;
    }

    public int getInterval() {
        return interval;
    }
}
