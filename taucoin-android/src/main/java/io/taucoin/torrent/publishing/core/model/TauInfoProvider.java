package io.taucoin.torrent.publishing.core.model;

import android.content.Context;

import com.frostwire.jlibtorrent.SessionStats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposables;

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
            final AtomicReference<SessionStats> stats = new AtomicReference<>();
            TauDaemonListener listener = new TauDaemonListener() {
                @Override
                public void onSessionStats(@NonNull SessionStats newStats) {
                    SessionStats oldStats = stats.get();
                    if (!newStats.equals(oldStats)) {
                        stats.set(newStats);
                        if (!emitter.isCancelled())
                            emitter.onNext(newStats);
                    }
                }
            };
            if (!emitter.isCancelled()) {
                daemon.registerListener(listener);
                emitter.setDisposable(Disposables.fromAction(() ->
                        daemon.registerListener(listener)));
            }
        }, BackpressureStrategy.LATEST);
    }
}
