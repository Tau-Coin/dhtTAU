package io.taucoin.torrent.publishing.core.model;

/**
 * 时间间隔枚举类
 */
public enum Interval {
    // 单位ms
    MAIN_LOOP_MIN(50),                     // 链端主循环最小时间间隔
    MAIN_LOOP_MAX(10000),                  // 链端主循环最大时间间隔
    MAIN_LOOP_NO_AVERAGE_SPEED(20000),     // 链端主循环最大时间间隔, average target speed = 0

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
