package io.taucoin.torrent.publishing.core.model;

import android.content.Context;

import io.taucoin.torrent.SessionStats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposables;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.utils.Formatter;
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
    private static final int CPU_MEM_STATISTICS_PERIOD = 3 * 1000;
    private static final int TRAFFIC_STATISTICS_PERIOD = 1 * 1000;

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
    public Flowable<SessionStats> observeSessionStats() {
        return makeSessionStatsFlowable();
    }

    /**
     * 创建torrent SessionStats的工作流
     * @return Flowable
     */
    private Flowable<SessionStats> makeSessionStatsFlowable() {
        return Flowable.create((emitter) -> {
            TauDaemonListener listener = new TauDaemonListener() {
                @Override
                public void onSessionStats(@NonNull SessionStats newStats) {
                    if (!emitter.isCancelled())
                        emitter.onNext(newStats);
                }
            };
            if (!emitter.isCancelled()) {
                daemon.registerListener(listener);
                emitter.setDisposable(Disposables.fromAction(() ->
                        daemon.unregisterListener(listener)));
            }
        }, BackpressureStrategy.LATEST);
    }

    Flowable<SessionStats> observeTrafficStatistics() {
        return makeTrafficStatisticsFlowable();
    }

    private Flowable<SessionStats> makeTrafficStatisticsFlowable() {
        return Flowable.create((emitter) -> {
            try {
                while (!emitter.isCancelled()){
                    Context context = MainApplication.getInstance();
                    long summaryTotal = NetworkStatsUtil.getSummaryTotal(context);
                    if (summaryTotal != -1) {
//                        TrafficUtil.saveTrafficSummaryTotal(summaryTotal);
                        logger.debug("saveTrafficSummaryTotal::{}",
                                Formatter.formatFileSize(context, summaryTotal));
                    } else {
                        long traffic = TrafficInfo.getTrafficUsed(context);
                        traffic = traffic >= 0 ? traffic : 0;
//                        TrafficUtil.saveTrafficTotal(traffic);
//                        logger.debug("saveTrafficTotal::{}, traffic::{}",
//                                Formatter.formatFileSize(context, TrafficUtil.getTrafficTotal()),
//                                Formatter.formatFileSize(context, traffic));
                    }
                    Thread.sleep(TRAFFIC_STATISTICS_PERIOD);
                }
            } catch (Exception ignore) {}
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
                        Thread.sleep(CPU_MEM_STATISTICS_PERIOD);
                    }
                }
            } catch (Exception ignore) {
            }
        }, BackpressureStrategy.LATEST);
    }
}