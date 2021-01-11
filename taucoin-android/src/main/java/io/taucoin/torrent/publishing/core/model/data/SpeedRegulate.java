package io.taucoin.torrent.publishing.core.model.data;

/**
 * 网速调节值
 */
public enum SpeedRegulate {
    SPEED_UP(-1), // 需要提速
    SPEED_DOWN(1), // 需要降速
    SPEED_UNCHANGED(10), // 网速不变
    NO_REMAINING_DATA(2);  // 没有剩余流量

    private int value;
    SpeedRegulate(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
