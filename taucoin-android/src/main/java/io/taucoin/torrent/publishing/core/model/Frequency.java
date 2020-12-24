package io.taucoin.torrent.publishing.core.model;

/**
 * 频率枚举类
 */
public enum Frequency {
    // Gossip频率，单位s
    GOSSIP_FREQUENCY_HEIGHT(1),
    GOSSIP_FREQUENCY_MEDIUM_HEIGHT(5),
    GOSSIP_FREQUENCY_DEFAULT(10),
    GOSSIP_FREQUENCY_MEDIUM_LOW(30),
    GOSSIP_FREQUENCY_LOW(60),

    // Worker中失败异常重试频率，单位ms
    FREQUENCY_RETRY(1000),
    // 聊天界面，发送写时间频率，单位s
    FREQUENCY_PUBLISH_WRITING(60);

    private long frequency;
    Frequency(long frequency) {
        this.frequency = frequency;
    }

    public long getFrequency() {
        return frequency;
    }
}
