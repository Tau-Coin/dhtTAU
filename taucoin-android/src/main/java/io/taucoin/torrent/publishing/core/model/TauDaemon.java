package io.taucoin.torrent.publishing.core.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.config.ChainConfig;
import io.taucoin.controller.TauController;
import io.taucoin.torrent.SessionSettings;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.leveldb.AndroidLeveldbFactory;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.receiver.ConnectionReceiver;
import io.taucoin.torrent.publishing.service.SystemServiceManager;
import io.taucoin.torrent.publishing.receiver.PowerReceiver;
import io.taucoin.torrent.publishing.service.Scheduler;
import io.taucoin.torrent.publishing.service.TauService;
import io.taucoin.types.Block;
import io.taucoin.types.Transaction;
import io.taucoin.util.ByteUtil;

/**
 * 区块链业务Daemon
 */
public class TauDaemon {
    private static final String TAG = TauDaemon.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(TAG);

    private Context appContext;
    private SettingsRepository settingsRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private PowerReceiver powerReceiver = new PowerReceiver();
    private ConnectionReceiver connectionReceiver = new ConnectionReceiver();
    private TauController tauController;
    private SessionSettings sessionSettings;
    private PowerManager.WakeLock wakeLock;
    private SystemServiceManager systemServiceManager;
    private ExecutorService exec = Executors.newSingleThreadExecutor();
    private TauListenHandler tauListenHandler;
    private boolean isRunning = false;

    private static volatile TauDaemon instance;

    public static TauDaemon getInstance(@NonNull Context appContext) {
        if (instance == null) {
            synchronized (TauDaemon.class) {
                if (instance == null)
                    instance = new TauDaemon(appContext);
            }
        }
        return instance;
    }

    /**
     * TauDaemon构造函数
     */
    private TauDaemon(@NonNull Context appContext) {
        this.appContext = appContext;
        settingsRepo = RepositoryHelper.getSettingsRepository(appContext);
        systemServiceManager = SystemServiceManager.getInstance(appContext);
        tauListenHandler = new TauListenHandler(appContext, this);

        AndroidLeveldbFactory androidLeveldbFactory = new AndroidLeveldbFactory();
        String repoPath = appContext.getApplicationInfo().dataDir;
        logger.info("TauController repoPath::{}", repoPath);
        tauController = new TauController(repoPath, androidLeveldbFactory);
        tauController.registerListener(daemonListener);
        sessionSettings = new SessionSettings.Builder()
                .setDHTMaxItems(SessionSettings.TauDHTMaxItems)
                .build();

        switchPowerReceiver();
        switchConnectionReceiver();
    }

    /**
     * 更新用户Seed
     * @param seed Seed
     */
    public void updateSeed(String seed) {
        byte[] byteSeed = ByteUtil.toByte(seed);
        tauController.updateKey(byteSeed);
    }

    /**
     * Daemon启动
     */
    public void start() {
        if (isRunning){
            return;
        }
        Intent intent = new Intent(appContext, TauService.class);
        Utils.startServiceBackground(appContext, intent);
    }

    /**
     * 观察是否需要启动Daemon
     * @return Flowable
     */
    public Flowable<Boolean> observeNeedStartDaemon() {
        return Flowable.create((emitter) -> {
            if (emitter.isCancelled()){
                return;
            }
            Runnable emitLoop = () -> {
                while (!Thread.interrupted()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (emitter.isCancelled() || isRunning){
                        return;
                    }
                    emitter.onNext(true);
                }
            };

            Disposable d = observeDaemonRunning()
                    .subscribeOn(Schedulers.io())
                    .subscribe((isRunning) -> {
                        if (emitter.isCancelled())
                            return;

                        if (!isRunning) {
                            emitter.onNext(true);
                            exec.submit(emitLoop);
                        }
                    });
            if (!emitter.isCancelled()) {
                emitter.onNext(!isRunning);
                emitter.setDisposable(d);
            }

        }, BackpressureStrategy.LATEST);
    }

    /**
     * 观察Daemon是否是在运行
     */
    private Flowable<Boolean> observeDaemonRunning() {
        return Flowable.create((emitter) -> {
            if (emitter.isCancelled())
                return;

            TauDaemonListener listener = new TauDaemonListener() {
                @Override
                public void onTauStarted() {
                    if (!emitter.isCancelled())
                        emitter.onNext(true);
                }

                @Override
                public void onTauStopped() {
                    if (!emitter.isCancelled())
                        emitter.onNext(false);
                }
            };

            if (!emitter.isCancelled()) {
                emitter.onNext(isRunning);
                registerListener(listener);
                emitter.setDisposable(Disposables.fromAction(() -> unregisterListener(listener)));
            }

        }, BackpressureStrategy.LATEST);
    }

    /**
     * 链端事件监听逻辑处理
     */
    private final TauDaemonListener daemonListener = new TauDaemonListener() {
        @Override
        public void onTauStarted() {
            isRunning = true;
            rescheduleTauBySettings();
        }

        @Override
        public void onNewBlock(Block block) {
            tauListenHandler.saveCommunity(block);
            tauListenHandler.handleBlockData(block, false, false);
        }

        @Override
        public void onRollBack(Block block) {
            tauListenHandler.handleBlockData(block, true, false);
        }

        @Override
        public void onSyncBlock(Block block) {
            tauListenHandler.handleBlockData(block, false, true);
        }
    };

    /**
     * Only calls from TauService
     */
    public void doStart() {
        logger.info("doStart");
        if (isRunning)
            return;
        disposables.add(settingsRepo.observeSettingsChanged()
                .subscribe(this::handleSettingsChanged));
        tauController.start(sessionSettings);
    }

    /**
     * Only calls from TauService
     */
    public void doStop() {
        if (!isRunning)
            return;
        isRunning = false;
        disposables.clear();
        tauListenHandler.destroy();
        tauController.stop();
    }

    /**
     * 强制停止
     */
    public void forceStop() {
        Intent i = new Intent(appContext, TauService.class);
        i.setAction(TauService.ACTION_SHUTDOWN);
        Utils.startServiceBackground(appContext, i);
    }

    /**
     * 事件监听注册进TauController
     * @param listener TauDaemonListener
     */
    public void registerListener(TauDaemonListener listener) {
        tauController.registerListener(listener);
    }

    /**
     * 从TauController取消事件监听
     * @param listener TauDaemonListener
     */
    public void unregisterListener(TauDaemonListener listener) {
        tauController.unregisterListener(listener);
    }

    /**
     * 电源充电状态切换广播接受器
     */
    private void switchPowerReceiver() {
        settingsRepo.chargingState(systemServiceManager.isPlugged());
        try {
            appContext.unregisterReceiver(powerReceiver);
        } catch (IllegalArgumentException ignore) {
            /* Ignore non-registered receiver */
        }
        appContext.registerReceiver(powerReceiver, PowerReceiver.getCustomFilter());
    }

    /**
     * 网络连接切换广播接受器
     */
    private void switchConnectionReceiver() {
        settingsRepo.internetState(systemServiceManager.isHaveNetwork());
        try {
            appContext.unregisterReceiver(connectionReceiver);
        } catch (IllegalArgumentException ignore) {
            /* Ignore non-registered receiver */
        }
        appContext.registerReceiver(connectionReceiver, ConnectionReceiver.getFilter());
    }

    /**
     * 处理设置的改变
     * @param key 存储key
     */
    private void handleSettingsChanged(String key) {
        logger.info("handleSettingsChanged::{}", key);
        if (key.equals(appContext.getString(R.string.pref_key_wifi_only))) {
            rescheduleTauBySettings();
        }else if (key.equals(appContext.getString(R.string.pref_key_telecom_data_end_time))) {
            rescheduleTauBySettings();
        } else if (key.equals(appContext.getString(R.string.pref_key_internet_state))) {
            logger.info("SettingsChanged, internet state::{}", settingsRepo.internetState());
            enableServerMode(settingsRepo.serverMode());
            if(settingsRepo.wifiOnly()){
                rescheduleTauBySettings();
            }
        } else if (key.equals(appContext.getString(R.string.pref_key_charging_state))) {
            logger.info("SettingsChanged, charging state::{}", settingsRepo.chargingState());
            enableServerMode(settingsRepo.serverMode());
        }
    }

    /**
     * 根据当前设置重新调度Tau
     */
    private synchronized void rescheduleTauBySettings() {
        if (!isRunning){
            return;
        }
        logger.info("rescheduleTauBySettings Wifi Only::{}", settingsRepo.wifiOnly());
        if(settingsRepo.wifiOnly()){
            settingsRepo.telecomDataEndTime(0);
            Scheduler.cancelSwitchWifiOnlyAlarm(appContext);
            if(systemServiceManager.isMobileConnected()){
                if(isRunning){
                    // TODO：电信网络暂停链端业务
                    // tauController.pause();
                }

                logger.info("rescheduleTauBySettings, network type::MobileConnected");
            }else if(systemServiceManager.isWifiConnected()){
                if(isRunning){
                    // TODO：wifi网络恢复链端业务
                    // tauController.resume();
                }
                logger.info("rescheduleTauBySettings, network type::WifiConnected");
            }
        }else{
            long endTime = settingsRepo.telecomDataEndTime();
            logger.info("rescheduleTauBySettings, end time::{}", endTime);
            if(endTime <= 0){
                return;
            }
            long currentTime = System.currentTimeMillis();
            logger.info("rescheduleTauBySettings, current time::{}", currentTime);
            if(!settingsRepo.wifiOnly() && endTime > currentTime){
                Scheduler.setSwitchWifiOnlyAlarm(appContext, endTime);
                if(isRunning){
                    // TODO：恢复链端业务
                    // tauController.resume();
                }
            }else{
                settingsRepo.wifiOnly(true);
            }
        }
    }

    /**
     * 设置是否启动服务器模式
     */
    public void enableServerMode(boolean enable) {
        Utils.enableBootReceiver(appContext, enable);
        logger.info("EnableServerMode, enable::{}", enable);
        if (enable) {
            keepCPUWakeLock(true);
            Scheduler.cancelWakeUpAppAlarm(appContext);
        } else {
            // 设备在充电状态并网络可用的状态下，启动WakeLock
            logger.info("EnableServerMode, chargingState::{}, internetState::{}",
                    settingsRepo.chargingState(), settingsRepo.internetState());
            if(settingsRepo.chargingState() && settingsRepo.internetState()){
                keepCPUWakeLock(true);
                Scheduler.cancelWakeUpAppAlarm(appContext);
            }else{
                keepCPUWakeLock(false);
                Scheduler.setWakeUpAppAlarm(appContext);
            }
        }
    }

    /**
     * 保持CPU唤醒锁定
     */
    @SuppressLint("WakelockTimeout")
    public void keepCPUWakeLock(boolean enable) {
        if (enable) {
            if (wakeLock == null) {
                Context context = MainApplication.getInstance();
                PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            }
            if (!wakeLock.isHeld()){
                wakeLock.acquire();
            }
        } else {
            if (wakeLock == null){
                return;
            }
            if (wakeLock.isHeld()){
                wakeLock.release();
            }
        }
        settingsRepo.wakeLock(enable);
    }

    /**
     * 提交交易到链端交易池
     * @param tx 签过名的交易数据
     */
    public void submitTransaction(Transaction tx){

    }

    /**
     * 添加或创建社区
     * @param chainConfig 链的配置
     */
    public void createCommunity(ChainConfig chainConfig) {

    }

    /**
     * 获取用户Nonce值
     * @return long
     */
    public long getUserPower(String chainID, String publicKey) {
        return 0;
    }

    /**
     * 获取用户余额
     * @return long
     */
    public long getUserBalance(String chainID, String publicKey) {
        return 0;
    }
}