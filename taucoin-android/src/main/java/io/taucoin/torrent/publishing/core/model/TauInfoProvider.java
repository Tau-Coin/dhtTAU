package io.taucoin.torrent.publishing.core.model;

import android.content.Context;
import android.text.format.Formatter;

import io.taucoin.torrent.SessionStats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposables;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.utils.NetworkStatsUtil;
import io.taucoin.torrent.publishing.core.utils.TrafficInfo;
import io.taucoin.torrent.publishing.core.utils.TrafficUtil;

/**
 * Provides runtime information about Tau, which isn't saved to the database.
 */
public class TauInfoProvider {
    private static final String TAG = TauInfoProvider.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(TAG);

    private static volatile TauInfoProvider INSTANCE;
    private TauDaemon daemon;

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
                        daemon.registerListener(listener)));
            }
        }, BackpressureStrategy.LATEST);
    }

    Flowable<SessionStats> observeResourceStatistics() {
        return makeResourceStatisticsFlowable();
    }

    private Flowable<SessionStats> makeResourceStatisticsFlowable() {
        return Flowable.create((emitter) -> {
            do {
                Context context = MainApplication.getInstance();
                long summaryTotal = NetworkStatsUtil.getSummaryTotal(context);
                if (summaryTotal != -1) {
                    TrafficUtil.saveTrafficSummaryTotal(summaryTotal);
                    logger.debug("saveTrafficSummaryTotal::{}",
                            Formatter.formatFileSize(context, summaryTotal));
                } else {
                    long traffic = TrafficInfo.getTrafficUsed(context);
                    traffic = traffic >= 0 ? traffic : 0;
                    TrafficUtil.saveTrafficTotal(traffic);
                    logger.debug("saveTrafficTotal::{}, traffic::{}",
                            Formatter.formatFileSize(context, TrafficUtil.getTrafficTotal()),
                            Formatter.formatFileSize(context, traffic));
                }
                Thread.sleep(1000);
            } while (!emitter.isCancelled());
        }, BackpressureStrategy.LATEST);
    }
}