package io.taucoin.torrent.publishing.core.model;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposables;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Statistic;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.StatisticRepository;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.Formatter;
import io.taucoin.torrent.publishing.core.utils.NetworkSetting;
import io.taucoin.torrent.publishing.core.utils.Sampler;
import io.taucoin.torrent.publishing.core.utils.SessionStatistics;
import io.taucoin.torrent.publishing.core.utils.TrafficUtil;
import io.taucoin.torrent.publishing.service.SystemServiceManager;
import io.taucoin.torrent.publishing.ui.constant.Constants;

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
    private SettingsRepository settingsRepo;
    private StatisticRepository statisticRepo;

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
        settingsRepo = RepositoryHelper.getSettingsRepository(MainApplication.getInstance());
        statisticRepo = RepositoryHelper.getStatisticRepository(MainApplication.getInstance());
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
     * 观察APP统计
     * @return
     */
    Flowable<Void> observeAppStatistics() {
        return makeAppStatisticsFlowable();
    }

    /**
     * 创建流量统计流
     * @return
     */
    private Flowable<Void> makeAppStatisticsFlowable() {
        return Flowable.create((emitter) -> {
            try {
                Sampler.Statistics samplerStatistics = new Sampler.Statistics();
                if (!emitter.isCancelled()) {
                    emitter.setDisposable(Disposables.fromAction(() -> {

                    }));
                }
                long trafficSize;
                long oldTrafficTotal = 0;
                SessionStatistics sessionStatistics = new SessionStatistics();
                Statistic statistic = new Statistic();
                int seconds = 0;
                while (!emitter.isCancelled()) {
                    long startTimestamp = DateUtil.getMillisTime();
                    handlerTrafficStatistics(sessionStatistics);
                    long trafficTotal = sessionStatistics.getTotalDownload() + sessionStatistics.getTotalUpload();
                    trafficSize = trafficTotal - oldTrafficTotal;
                    trafficSize = Math.max(trafficSize, 0);
                    oldTrafficTotal = trafficTotal;

                    handlerCPUAndMemoryStatistics(samplerStatistics);

                    statistic.timestamp = startTimestamp / 1000;
                    statistic.dataSize = trafficSize;
                    statistic.memorySize = samplerStatistics.totalMemory;
                    statistic.cpuUsageRate = samplerStatistics.cpuUsage;
                    statistic.isMetered = NetworkSetting.isMeteredNetwork() ? 1 : 0;
                    statisticRepo.addStatistic(statistic);
                    if (seconds > Constants.STATISTICS_CLEANING_PERIOD) {
                        statisticRepo.deleteOldStatistics();
                        seconds = 0;
                    }
                    seconds ++;
                    // 处理代码执行时间
                    long endTimestamp = DateUtil.getMillisTime();
                    long consumeTime = endTimestamp - startTimestamp;
                    long sleepTime = STATISTICS_PERIOD - consumeTime;
                    if (sleepTime > 0) {
                        Thread.sleep(STATISTICS_PERIOD);
                    }
                }
            } catch (InterruptedException ignore) {
            } catch (Exception e) {
                logger.error("makeAppStatisticsFlowable is error", e);
            }
        }, BackpressureStrategy.LATEST);
    }

    /**
     * 统计流量使用信息
     */
    private void handlerTrafficStatistics(SessionStatistics statistics) {
        logger.debug("ConnectionReceiver isHaveNetwork::{}, isNetworkMetered::{}",
                SystemServiceManager.getInstance().isHaveNetwork(),
                SystemServiceManager.getInstance().isNetworkMetered());

        Context context = MainApplication.getInstance();
        daemon.getSessionStatistics(statistics);
        // 保存流量统计
        TrafficUtil.saveTrafficTotal(statistics);
        // 更新网速采样数据
        NetworkSetting.updateNetworkSpeed(statistics);
        // 更新UI展示链端主循环时间间隔
        NetworkSetting.calculateMainLoopInterval();
        // 重新调度TAU工作通过设置
        daemon.rescheduleTAUBySettings();
        logger.debug("Network statistical:: totalDownload::{}({}), totalUpload::{}({})" +
                        ", downloadRate::{}/s, uploadRate::{}/s",
                Formatter.formatFileSize(context, statistics.getTotalDownload()),
                statistics.getTotalDownload(),
                Formatter.formatFileSize(context, statistics.getTotalUpload()),
                statistics.getTotalUpload(),
                Formatter.formatFileSize(context, statistics.getDownloadRate()),
                Formatter.formatFileSize(context, statistics.getUploadRate()));
    }

    /**
     * 统计CPU和Memory使用信息
     */
    private void handlerCPUAndMemoryStatistics( Sampler.Statistics statistics) {
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
        statistics.cpuUsage = cpuUsageRate;
        statistics.totalMemory = totalMemory;

        settingsRepo.setCpuUsage(statistics.cpuUsageRate);
        settingsRepo.setMemoryUsage(statistics.totalMemory);
        Context context = MainApplication.getInstance();
        logger.debug("cpuUsageRate::{}, cpuUsageRate::{}, maxMemory::{}", cpuUsageRate,
                statistics.cpuUsageRate,
                Formatter.formatFileSize(context, statistics.totalMemory));
    }
}
