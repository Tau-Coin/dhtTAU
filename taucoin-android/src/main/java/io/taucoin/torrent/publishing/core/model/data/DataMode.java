package io.taucoin.torrent.publishing.core.model.data;

/**
 * Data Cost中的选择的数据模式
 */
public enum DataMode {
    FOREGROUND(1),          // 前台模式：高速模式
    BACKGROUND(3);          // 后台模式：低速模式

    private int mode;
    DataMode(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return mode;
    }
}
