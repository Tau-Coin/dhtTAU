package io.taucoin.torrent.publishing.core.model;

/**
 * 时间间隔枚举类
 */
public enum Interval {
    // 单位ms
    FORE_MAIN_LOOP_MIN(50),                     // 前台链端主循环最小时间间隔
    FORE_MAIN_LOOP_MAX(5000),                   // 前台链端主循环最大时间间隔

    // 单位ms
    BACK_MAIN_LOOP_MIN(50),                     // 后台链端主循环最小时间间隔
    BACK_MAIN_LOOP_MAX(15000),                  // 后台链端主循环最大时间间隔

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
