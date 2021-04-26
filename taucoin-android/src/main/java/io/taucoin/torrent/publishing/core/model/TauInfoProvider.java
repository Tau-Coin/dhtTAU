package io.taucoin.torrent.publishing.core.model;

import android.content.Context;
import android.os.Build;

import io.taucoin.dht.SessionStats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposables;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.utils.EmulatorUtil;
import io.taucoin.torrent.publishing.core.utils.Formatter;
import io.taucoin.torrent.publishing.core.utils.NetworkSetting;
import io.taucoin.torrent.publishing.core.utils.NetworkStatistics;
import io.taucoin.torrent.publishing.core.utils.NetworkStatsUtil;
import io.taucoin.torrent.publishing.core.utils.Sampler;
import io.taucoin.torrent.publishing.core.utils.TrafficInfo;
import io.taucoin.torrent.publishing.core.utils.TrafficUtil;
import io.taucoin.torrent.publishing.service.SystemServiceManager;

/**
 * Provides runtime information about Tau, which isn't saved to the database.
 */
public class TauInfoProvider {
    private static final String TAG = TauInfoProvider.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(TAG);
    private static final int STATISTICS_PERIOD = 1000;

    private static volatile TauInfoProvider INSTANCE;
    private TauDaemon daemon;
    private Sampler sampler;

    public static TauInfoProvider getInstance(@NonNull Context appContext) {
        if (INSTANCE == null) {
            synchronized (TauInfoProvider.class) {
                if (INSTANCE == null)
                    INSTANCE = new TauInfoProvider(TauDaemon.getInstance(appContext));
            }
        }
        return INSTANCE;
    }

    public static TauInfoProvider getInstance(TauDaemon tauDaemon) {
        if (INSTANCE == null) {
            synchronized (TauInfoProvider.class) {
                if (INSTANCE == null)
                    INSTANCE = new TauInfoProvider(tauDaemon);
            }
        }
        return INSTANCE;
    }

    private TauInfoProvider(TauDaemon daemon) {
        this.daemon = daemon;
        this.sampler = Sampler.getInstance();
    }

    /**
     * 观察torrent SessionStats的工作流
     * @return Flowable
     */
    public Flowable<Long> observeSessionStats() {
        return makeSessionStatsFlowable();
    }

    /**
     * 创建torrent SessionStats的工作流
     * @return Flowable
     */
    private Flowable<Long> makeSessionStatsFlowable() {
        return Flowable.create((emitter) -> {
            try {
                while (!emitter.isCancelled()) {
                    long sessionNodes = daemon.getSessionNodes();
                    emitter.onNext(sessionNodes);
                    if (!emitter.isCancelled()) {
                        Thread.sleep(STATISTICS_PERIOD);
                    }
                }
            } catch (InterruptedException ignore) {
            } catch (Exception e) {
                logger.error("makeSessionStatsFlowable is error", e);
            }
        }, BackpressureStrategy.LATEST);
    }

    /**
     * 观察流量统计
     * @return
     */
    Flowable<SessionStats> observeTrafficStatistics() {
        return makeTrafficStatisticsFlowable();
    }

    /**
     * 创建流量统计流
     * @return
     */
    private Flowable<SessionStats> makeTrafficStatisticsFlowable() {
        return Flowable.create((emitter) -> {
            try {
                boolean isEmulator = EmulatorUtil.isEmulator();
                while (!emitter.isCancelled()){

                    logger.debug("ConnectionReceiver isHaveNetwork::{}, isNetworkMetered::{}",
                            SystemServiceManager.getInstance().isHaveNetwork(),
                            SystemServiceManager.getInstance().isNetworkMetered());

                    Context context = MainApplication.getInstance();
                    NetworkStatistics statistics = null;
                    if (!isEmulator && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                        statistics = NetworkStatsUtil.getSummaryTotal(context);
                    }
                    logger.debug("Network statistical methods:: {}", null == statistics ?
                            "TrafficInfo" : "NetworkStatsUtil");
                    if (null == statistics) {
                        statistics = TrafficInfo.getTrafficUsed(context);
                    }
                    if (statistics != null) {
                        // 更新网速采样数据
                        NetworkSetting.updateSpeedSample(statistics);
                        // 保存流量统计
                        TrafficUtil.saveTrafficTotal(statistics);
                        // 更新UI展示链端主循环时间间隔
                        NetworkSetting.calculateMainLoopInterval();
                        // 重新调度TAU工作通过设置
                        daemon.rescheduleTAUBySettings();
                        logger.debug("Network statistical result:: rxBytes::{}({}), txBytes::{}({})",
                                Formatter.formatFileSize(context, statistics.getRxBytes()),
                                statistics.getRxBytes(),
                                Formatter.formatFileSize(context, statistics.getTxBytes()),
                                statistics.getTxBytes());

                        Thread.sleep(STATISTICS_PERIOD);
                    } else {
                        logger.warn("Network statistical: Unable to get traffic data");
                        break;
                    }
                }
            } catch (InterruptedException ignore) {
            } catch (Exception e) {
                logger.error("makeTrafficStatisticsFlowable is error", e);
            }
        }, BackpressureStrategy.LATEST);
    }

    public Flowable<Sampler.Statistics> observeCPUAndMemStatistics() {
        return makeCPUAndMemStatisticsFlowable();
    }

    private Flowable<Sampler.Statistics> makeCPUAndMemStatisticsFlowable() {
        return Flowable.create((emitter) -> {
            try {
                Context context = MainApplication.getInstance();
                Sampler.Statistics statistics = new Sampler.Statistics();
                if (!emitter.isCancelled()) {
                    emitter.setDisposable(Disposables.fromAction(() -> {

                    }));
                }
                while (!emitter.isCancelled()){
                    long totalMemory = sampler.sampleMemory();
                    double cpuUsageRate = sampler.sampleCPU();

                    if (cpuUsageRate < 0) {
                        cpuUsageRate = 0;
                    } else if (cpuUsageRate > 100) {
                        cpuUsageRate = 100;
                    }
                    String cpuInfo = String.valueOf(cpuUsageRate);
                    int pointIndex = cpuInfo.indexOf(".");
                    int length = cpuInfo.length();
                    if(pointIndex > 0 && length - pointIndex > 3){
                        cpuInfo = cpuInfo.substring(0, pointIndex + 3);
                    }
                    cpuInfo += "%";
                    statistics.cpuUsageRate = cpuInfo;
                    statistics.totalMemory = totalMemory;

                    logger.debug("cpuUsageRate::{}, cpuUsageRate::{}, maxMemory::{}", cpuUsageRate,
                            statistics.cpuUsageRate,
                            Formatter.formatFileSize(context, statistics.totalMemory));
                    if (!emitter.isCancelled()) {
                        emitter.onNext(statistics);
                        Thread.sleep(STATISTICS_PERIOD);
                    }
                }
            } catch (Exception ignore) {
            }
        }, BackpressureStrategy.LATEST);
    }
}
