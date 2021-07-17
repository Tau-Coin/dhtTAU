package io.taucoin.torrent.publishing.ui.constant;

/**
 * UI端：相关常量配置
 */
public class Constants {
    // 昵称长度限制 单位：byte
    public static final int NICKNAME_LENGTH = 24;

    // 统计老数据多久清理一次，单位：秒
    public static final int STATISTICS_CLEANING_PERIOD = 10 * 60;

    // 统计显示周期，单位：秒
    public static final int STATISTICS_DISPLAY_PERIOD = 60;

    // 保存的字体大小缩放比例
    public static final String PREF_KEY_FONT_SCALE_SIZE = "pref_key_font_scale_size";
}