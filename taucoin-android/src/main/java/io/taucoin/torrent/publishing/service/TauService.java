package io.taucoin.torrent.publishing.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.Nullable;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.TauDaemonListener;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.receiver.NotificationReceiver;
import io.taucoin.torrent.publishing.ui.TauNotifier;

/**
 * TAUService: 链端业务服务
 * 包含初始化、启动、停止等
 */
public class TauService extends Service {

    private static final String TAG = TauService.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(TAG);
    public static final String ACTION_SHUTDOWN = "io.taucoin.torrent.publishing.service.ACTION_SHUTDOWN";

    // 是不是已经在运行
    private AtomicBoolean isAlreadyRunning = new AtomicBoolean(false);
    private TauDaemon daemon;
    private SettingsRepository settingsRepo;
    private UserRepository userRepo;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        settingsRepo = RepositoryHelper.getSettingsRepository(getApplicationContext());
        userRepo = RepositoryHelper.getUserRepository(getApplicationContext());
        daemon = TauDaemon.getInstance(getApplicationContext());
        TauNotifier.makeForegroundNotify(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = null;
        if (intent != null){
            action = intent.getAction();
        }

        // 处理关闭动作
        if (action != null && (StringUtil.isEquals(action, ACTION_SHUTDOWN)
                || StringUtil.isEquals(action, NotificationReceiver.NOTIFY_ACTION_SHUTDOWN_APP))) {
            shutdown();
            return START_NOT_STICKY;
        }

        // 是不是已经在运行
        if (isAlreadyRunning.compareAndSet(false, true)){
            subscribeCurrentUser();
        }
        return START_STICKY;
    }

    /**
     * 订阅当前用户
     */
    private void subscribeCurrentUser() {
        final AtomicBoolean isAlreadyInit = new AtomicBoolean(false);
        disposables.add(userRepo.observeCurrentUser()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(user -> {
//                    if(null == user){
//                        return;
//                    }
//                    // 更新设置用户seed
//                    daemon.updateSeed(user.seed);
//                    logger.info("Update user seed");
//                    if(isAlreadyInit.compareAndSet(false, true)){
//                        initAndStart();
//                    }
                }));
    }

    /**
     * 初始化并启动TauDaemon
     */
    private void initAndStart() {
        logger.info("initAndStart {}", TAG);

        TauNotifier.makeForegroundNotify(this);

        disposables.add(settingsRepo.observeSettingsChanged()
                .subscribe(this::handleSettingsChanged));

        daemon.enableServerMode(settingsRepo.serverMode());

        daemon.doStart();
        daemon.registerListener(daemonListener);
    }

    /**
     * 停止服务：动作需要在Tau链组件全部停止之后调用
     */
    private void stopService() {
        logger.info("stopService");
        disposables.clear();
        daemon.unregisterListener(daemonListener);
        daemon.keepCPUWakeLock(false);

        isAlreadyRunning.set(false);
        stopForeground(true);
        stopSelf();
    }

    /**
     * TauDaemon事件监听
     */
    private final TauDaemonListener daemonListener = new TauDaemonListener() {
        @Override
        public void onTauStopped() {
            stopService();
        }
    };

    /**
     * 关闭APP
     */
    private void shutdown() {
        logger.info("shutdown");
        disposables.add(Completable.fromRunnable(() -> daemon.doStop())
                .subscribeOn(Schedulers.computation())
                .subscribe());
    }

    /**
     * 处理设置的改变
     * @param key 改变的key
     */
    private void handleSettingsChanged(String key) {
        if (key.equals(getString(R.string.pref_key_server_mode))){
            daemon.enableServerMode(settingsRepo.serverMode());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        logger.info("Stop {}", TAG);
    }
}