package io.taucoin.torrent.publishing.core.model;

/**
 * 频率枚举类
 */
public enum Frequency {
    // Gossip频率
    GOSSIP_FREQUENCY_HEIGHT(1000),
    GOSSIP_FREQUENCY_MEDIUM_HEIGHT(5000),
    GOSSIP_FREQUENCY_DEFAULT(10000),
    GOSSIP_FREQUENCY_MEDIUM_LOW(30000),
    GOSSIP_FREQUENCY_LOW(60000),

    // Worker中失败异常重试频率
    FREQUENCY_RETRY(1000),
    // 聊天界面，发送上一次消息的时间频率
    FREQUENCY_PUBLISH_MESSAGE(3000);

    private long frequency;
    Frequency(long frequency) {
        this.frequency = frequency;
    }

    public long getFrequency() {
        return frequency;
    }
}
