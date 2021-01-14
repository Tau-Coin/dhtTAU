package io.taucoin.torrent.publishing.core.model;

import android.content.Context;
import android.os.Build;

import io.taucoin.dht.SessionStats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

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
            while (!emitter.isCancelled()) {
                BigInteger nodes = BigInteger.ZERO;
                List<Long> sessionNodes = daemon.getSessionNodes();
                if (sessionNodes != null) {
                    for (Long node : sessionNodes) {
                        nodes = nodes.add(BigInteger.valueOf(node));
                    }
                    emitter.onNext(nodes.longValue());
                }
                if (!emitter.isCancelled()) {
                    Thread.sleep(STATISTICS_PERIOD);
                }
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
                        NetworkSetting.updateSpeedSample(statistics);
                        TrafficUtil.saveTrafficTotal(statistics);
                        daemon.rescheduleDHTBySettings();
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
