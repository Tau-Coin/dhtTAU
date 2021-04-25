package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;

/**
 * 网络流量设置相关工具类
 */
public class FrequencyUtil {
    private static final Logger logger = LoggerFactory.getLogger("FrequencyUtil");
    private static final long internal_sample = 100;            // 主循环采样大小，单位s

    private static SettingsRepository settingsRepo;
    static {
        Context context = MainApplication.getInstance();
        settingsRepo = RepositoryHelper.getSettingsRepository(context);
    }

    /**
     * 获取当前主循环时间间隔
     */
    public static int getMainLoopInterval() {
        Context context = MainApplication.getInstance();
        List<Integer> list = settingsRepo.getListData(context.getString(R.string.pref_key_main_loop_interval_list),
                Integer.class);
        int totalSpeed = 0;
        int listSize = list.size();
        for (int i = listSize - 1; i >= 0; i--) {
            totalSpeed += list.get(i);
        }
        if (list.size() == 0) {
            return 0;
        }
        return totalSpeed / list.size();
    }

    /**
     * 获取当前主循环频率
     */
    public static double getMainLoopFrequency() {
        long interval = getMainLoopInterval();
        logger.trace("getMainLoopFrequency:: interval::{}", interval);
        if (interval > 0) {
            return  1.0 * 1000 / interval;
        }
        return 0;
    }

    /**
     * 更新主循环时间间隔
     */
    public static void updateMainLoopInterval(int interval) {
        Context context = MainApplication.getInstance();
        List<Integer> list = settingsRepo.getListData(context.getString(R.string.pref_key_main_loop_interval_list),
                Integer.class);
        if (list.size() >= internal_sample) {
            list.remove(0);
        }
        list.add(interval);
        logger.trace("updateMainLoopInterval:: list.size::{}, interval::{}", list.size(), interval);
        settingsRepo.setListData(context.getString(R.string.pref_key_main_loop_interval_list), list);
    }

    /**
     * 清除主循环时间间隔采样值列表
     */
    public static void clearMainLoopIntervalList() {
        Context context = MainApplication.getInstance();
        settingsRepo.setListData(context.getString(R.string.pref_key_main_loop_interval_list),
                new ArrayList<>());
    }
}