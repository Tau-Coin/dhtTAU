package io.taucoin.torrent.publishing.core.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.PowerManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.chain.ChainManager;
import io.taucoin.controller.TauController;
import io.taucoin.core.AccountState;
import io.taucoin.genesis.GenesisConfig;
import io.taucoin.repository.AppRepository;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.leveldb.AndroidLeveldbFactory;
import io.taucoin.torrent.publishing.core.utils.AppUtil;
import io.taucoin.torrent.publishing.core.utils.ChainLinkUtil;
import io.taucoin.torrent.publishing.core.utils.DeviceUtils;
import io.taucoin.torrent.publishing.core.utils.FrequencyUtil;
import io.taucoin.torrent.publishing.core.utils.NetworkSetting;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.receiver.ConnectionReceiver;
import io.taucoin.torrent.publishing.service.SystemServiceManager;
import io.taucoin.torrent.publishing.receiver.PowerReceiver;
import io.taucoin.torrent.publishing.service.TauService;
import io.taucoin.torrent.publishing.ui.setting.TrafficTipsActivity;
import io.taucoin.types.BlockContainer;
import io.taucoin.types.Transaction;
import io.taucoin.util.ByteUtil;

/**
 * 区块链业务Daemon
 */
public class TauDaemon {
    private static final String TAG = TauDaemon.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(TAG);
    private static final int REOPEN_NETWORKS_THRESHOLD = 15; // 单位s

    private Context appContext;
    private SettingsRepository settingsRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private PowerReceiver powerReceiver = new PowerReceiver();
    private ConnectionReceiver connectionReceiver = new ConnectionReceiver();
    private TauController tauController;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    private SystemServiceManager systemServiceManager;
    private ExecutorService exec = Executors.newSingleThreadExecutor();
    private TauListenHandler tauListenHandler;
    private MsgListenHandler msgListenHandler;
    private TauInfoProvider tauInfoProvider;
    private Disposable restartSessionTimer; // 重启Sessions定时任务
    private volatile boolean isRunning = false;
    private volatile boolean trafficTips = true; // 剩余流量用完提示
    private volatile String seed;
    private long noRemainingDataTimes = 0; // 触发无剩余流量的次数
    private int noNodesCount = 0; // 无节点计数, nodes查询频率为1s

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
        systemServiceManager = SystemServiceManager.getInstance();
        tauListenHandler = new TauListenHandler(appContext, this);
        msgListenHandler = new MsgListenHandler(appContext);
        tauInfoProvider = TauInfoProvider.getInstance(this);

        AndroidLeveldbFactory androidLeveldbFactory = new AndroidLeveldbFactory();
        String repoPath = appContext.getApplicationInfo().dataDir;
        String deviceID = DeviceUtils.getCustomDeviceID(appContext);
        logger.info("TauController deviceID::{}, repoPath::{}", deviceID, repoPath);
        AppRepository appRepository = RepositoryHelper.getAppRepository(appContext);
        tauController = new TauController(repoPath, androidLeveldbFactory,
                appRepository, deviceID.getBytes());
        tauController.registerListener(daemonListener);
        tauController.registerMsgListener(msgListenHandler);
        initLocalParam();
    }

    /**
     * 初始化本地参数
     */
    private void initLocalParam() {
        switchPowerReceiver();
        switchConnectionReceiver();
        settingsRepo.setUPnpMapped(false);
        settingsRepo.setNATPMPMapped(false);
        // 初始化主循环频率
        FrequencyUtil.clearMainLoopIntervalList();
    }

    /**
     * 更新用户Seed
     * @param seed Seed
     */
    public void updateSeed(String seed) {
        if (StringUtil.isEmpty(seed) || StringUtil.isEquals(seed, this.seed)) {
            return;
        }
        // 更新用户登录的设备信息
        String deviceID = DeviceUtils.getCustomDeviceID(appContext);
        msgListenHandler.onNewDeviceID(deviceID.getBytes());
        logger.debug("updateUserDeviceInfo deviceID::{}", deviceID);

        this.seed = seed;
        logger.debug("updateSeed ::{}", seed);
        byte[] bytesSeed = ByteUtil.toByte(seed);
        tauController.updateKey(bytesSeed);
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
    public Flowable<Boolean> observeDaemonRunning() {
        return Flowable.create((emitter) -> {
            if (emitter.isCancelled())
                return;

            TauDaemonListener listener = new TauDaemonListener() {
                @Override
                public void onTauStarted(boolean success, String errMsg) {
                    if (!emitter.isCancelled() && success)
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
        public void onTauStarted(boolean success, String errMsg) {
            if (success) {
                logger.debug("Tau start successfully");
                isRunning = true;
                handleSettingsChanged(appContext.getString(R.string.pref_key_foreground_running));
                resetReadOnly();
            } else {
                logger.error("Tau failed to start::{}", errMsg);
            }
        }

        @Override
        public void onClearChainAllState(byte[] chainID) {
            tauListenHandler.handleClearChainAllState(chainID);
        }

        @Override
        public void onNewBlock(byte[] chainID, BlockContainer blockContainer) {
            tauListenHandler.handleNewBlock(chainID, blockContainer);
        }

        @Override
        public void onRollBack(byte[] chainID, BlockContainer blockContainer) {
            tauListenHandler.handleRollBack(chainID, blockContainer);
        }

        @Override
        public void onSyncBlock(byte[] chainID, BlockContainer blockContainer) {
            tauListenHandler.handleSyncBlock(chainID, blockContainer);
        }

        @Override
        public void onNATPMPMapped(int index, int externalPort) {
            logger.info("Nat-PMP mapped::{}", true);
            settingsRepo.setNATPMPMapped(true);
        }

        @Override
        public void onNATPMPUnmapped(int index) {
            logger.info("Nat-PMP mapped::{}", false);
            settingsRepo.setNATPMPMapped(false);
        }

        @Override
        public void onUPNPMapped(int index, int externalPort) {
            logger.info("UPnP mapped::{}", true);
            settingsRepo.setUPnpMapped(true);
        }

        @Override
        public void onUPNPUnmapped(int index) {
            logger.info("UPnP mapped::{}", false);
            settingsRepo.setUPnpMapped(false);
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
        disposables.add(tauInfoProvider.observeTrafficStatistics()
                .subscribeOn(Schedulers.newThread())
                .subscribe());
        disposables.add(tauInfoProvider.observeSessionStats()
                .subscribeOn(Schedulers.newThread())
                .subscribe(nodes -> {
                    if (nodes > 0) {
                        noNodesCount = 0;
                    } else {
                        noNodesCount += 1;
                        if (noNodesCount > REOPEN_NETWORKS_THRESHOLD) {
                            logger.trace("No nodes more than {}s, restartSessions...",
                                    REOPEN_NETWORKS_THRESHOLD);
                            restartSessions();
                            noNodesCount = 0;
                        }
                    }
                }));

        rescheduleTAUBySettings();
        tauController.start();
    }

    /**
     * Only calls from TauService
     */
    public void doStop() {
        if (!isRunning)
            return;
        isRunning = false;
        disposables.clear();
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
        settingsRepo.setInternetType(systemServiceManager.getInternetType());
        NetworkSetting.clearSpeedList();
        NetworkSetting.setMeteredNetwork(systemServiceManager.isNetworkMetered());
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
        if (key.equals(appContext.getString(R.string.pref_key_internet_state))) {
            logger.info("SettingsChanged, internet state::{}", settingsRepo.internetState());
        } else if (key.equals(appContext.getString(R.string.pref_key_internet_type))) {
            logger.info("SettingsChanged, internet type::{}", settingsRepo.getInternetType());
            rescheduleTAUBySettings(true);
        } else if (key.equals(appContext.getString(R.string.pref_key_charging_state))) {
            logger.info("SettingsChanged, charging state::{}", settingsRepo.chargingState());
        } else if (key.equals(appContext.getString(R.string.pref_key_is_metered_network))) {
            logger.info("clearSpeedList, isMeteredNetwork::{}", NetworkSetting.isMeteredNetwork());
        } else if (key.equals(appContext.getString(R.string.pref_key_current_speed_list))) {
            if (restartSessionTimer != null && !restartSessionTimer.isDisposed()
                    && NetworkSetting.getCurrentSpeed() > 0) {
                restartSessionTimer.dispose();
                disposables.remove(restartSessionTimer);
                logger.info("restartSessionTimer dispose");
            }
        } else if (key.equals(appContext.getString(R.string.pref_key_foreground_running))) {
            boolean isForeground = settingsRepo.getBooleanValue(key);
            logger.info("foreground running::{}", isForeground);
            restartSessionTimer();
        } else if (key.equals(appContext.getString(R.string.pref_key_nat_pmp_mapped))) {
            logger.info("SettingsChanged, Nat-PMP mapped::{}", settingsRepo.isNATPMPMapped());
        } else if (key.equals(appContext.getString(R.string.pref_key_upnp_mapped))) {
            logger.info("SettingsChanged, UPnP mapped::{}", settingsRepo.isUPnpMapped());
        }
    }

    /**
     * 定时任务：APP从后台到前台触发, 网速采样时间后，网速依然为0，重启Sessions
     */
    private void restartSessionTimer() {
        boolean isForeground = settingsRepo.getBooleanValue(appContext.getString(R.string.pref_key_foreground_running));
        if (!isForeground) {
            return;
        }
        if (restartSessionTimer != null && !restartSessionTimer.isDisposed()) {
            return;
        }
        logger.info("restartSessionTimer start");
        restartSessionTimer = Observable.timer(NetworkSetting.CURRENT_SPEED_SAMPLE, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(time -> {
                    // 当前有网络连接, 并且还有剩余可用流量，网速为0，重新启动Session
                    boolean isHaveAvailableData = NetworkSetting.isHaveAvailableData();
                    boolean isRestart = settingsRepo.internetState() && isHaveAvailableData
                            && NetworkSetting.getCurrentSpeed() == 0;
                    logger.info("restartSessionTimer isRestart::{}", isRestart);
                    if (isRestart) {
                        NetworkSetting.clearSpeedList();
                        rescheduleTAUBySettings(true);
                    }

                });
        disposables.add(restartSessionTimer);
    }

    /**
     * 根据当前设置重新调度DHT
     */
    void rescheduleTAUBySettings() {
        rescheduleTAUBySettings(false);
    }

    /**
     * 根据当前设置重新调度DHT
     * @param isRestart 是否重启Session
     */
    private synchronized void rescheduleTAUBySettings(boolean isRestart) {
        if (!isRunning) {
            return;
        }
        try {
            // 判断有无网络连接
            if (settingsRepo.internetState()) {
                if (isRestart) {
                    reopenNetworks();
                    resetReadOnly();
                } else {
                    if (NetworkSetting.isHaveAvailableData()) {
                        // 重置无可用流量提示对话框的参数
                        trafficTips = true;
                        noRemainingDataTimes = 0;
                        resetReadOnly(false);
                    } else {
                        resetReadOnly(true);
                        showNoRemainingDataTipsDialog();
                    }
                }

            }
        } catch (Exception e) {
            logger.error("rescheduleDHTBySettings errors", e);
        }
    }

    /**
     * 重新打开网络
     */
    void reopenNetworks() {
        if (!isRunning) {
            return;
        }
        tauController.reopenNetworks();
        logger.debug("reopenNetworks...");
    }

    /**
     * 重新启动Sessions
     */
    void restartSessions() {
        if (!isRunning) {
            return;
        }
        tauController.restartSessions();
        logger.debug("restartSessions...");
    }

    /**
     * 显示没有剩余流量提示对话框
     * 必须同时满足需要提示、触发次数大于等于网速采样数、APP在前台、目前没有打开的流量提示Activity
     */
    private void showNoRemainingDataTipsDialog() {
        if (trafficTips) {
            if (noRemainingDataTimes < NetworkSetting.CURRENT_SPEED_SAMPLE) {
                noRemainingDataTimes += 1;
                return;
            }
        }
        if (trafficTips && AppUtil.isOnForeground(appContext) &&
                !AppUtil.isForeground(appContext, TrafficTipsActivity.class)) {
            Intent intent = new Intent(appContext, TrafficTipsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext.startActivity(intent);
        }
    }

    /**
     * 处理用户流量提示选择
     * @param updateDailyDataLimit 是否更新每日流量限制
     */
    public void handleUserSelected(boolean updateDailyDataLimit) {
        trafficTips = updateDailyDataLimit;
        resetReadOnly(!updateDailyDataLimit);
    }

    /**
     * 计费网络下设置DHTEngine为read only模式
     */
    private void resetReadOnly() {
        resetReadOnly(false);
    }

    /**
     * 计费网络下设置DHTEngine为read only模式
     *  或者没有剩余流量时，强迫为read only模式
     */
    private void resetReadOnly(boolean isForced) {
        if (!isRunning) {
            return;
        }
        boolean isReadOnly = NetworkSetting.isMeteredNetwork() || isForced;
        tauController.getDHTEngine().setReadOnly(isReadOnly);
    }

    /**
     * 重置唤醒锁
     */
    public void resetWakeLock(boolean enable) {
        // 启动/禁止设备启动广播接收器
        if (enable) {
            Utils.enableBootReceiver(appContext, true);
        }
        // 启动CPU WakeLock
        keepCPUWakeLock(enable);
        // 启动Wifi WakeLock
        keepWifiWakeLock(enable);
    }

    /**
     * 保持CPU唤醒锁定
     */
    @SuppressLint("WakelockTimeout")
    private void keepCPUWakeLock(boolean enable) {
        logger.info("keepCPUWakeLock::{}", enable);
        if (enable) {
            if (wakeLock == null) {
                PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
                // 保持CPU 运转，屏幕和键盘灯有可能是关闭的。
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
    }

    /**
     * 保持Wifi唤醒锁定
     */
    private void keepWifiWakeLock(boolean enable) {
        logger.info("keepWifiWakeLock::{}", enable);
        if (enable) {
            if (wifiLock == null) {

                WifiManager wifiManager = (WifiManager) appContext.getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE);
                // 扫描，自动的尝试去连接一个曾经配置过的点，保持最佳性能
                wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, TAG);
            }
            if (!wifiLock.isHeld()){
                wifiLock.acquire();
            }
        } else {
            if (wifiLock == null){
                return;
            }
            if (wifiLock.isHeld()){
                wifiLock.release();
            }
        }
    }

    /**
     * 提交交易到链端交易池
     * @param tx 签过名的交易数据
     */
    public void submitTransaction(Transaction tx){
        if (isRunning) {
            getChainManager().sendTransaction(tx);
            logger.info("submitTransaction txID::{}, txType::{}",
                    ByteUtil.toHexString(tx.getTxID()), tx.getTxType());
        }
    }

    /**
     * 添加或创建社区
     * @param cf GenesisConfig
     */
    public void createNewCommunity(GenesisConfig cf) {
        if (isRunning) {
            boolean isSuccess = getChainManager().createNewCommunity(cf);
            logger.info("createNewCommunity CommunityName::{}, chainID::{}, isSuccess::{}",
                    cf.getCommunityName(),
                    Utils.toUTF8String(cf.getChainID()),
                    isSuccess);
        }
    }

    private ChainManager getChainManager() {
        return tauController.getChainManager();
    }

    /**
     * 获取用户Nonce值
     * @return long
     */
    public long getUserPower(String chainID, String publicKey) {
        try {
            AccountState accountState = tauController.getChainManager().getStateDB().getAccount(
                    chainID.getBytes(), ByteUtil.toByte(publicKey));
            if (accountState != null) {
                return accountState.getNonce().longValue();
            }
        } catch (Exception e) {
            logger.error("getUserPower error", e);
        }
        return 0L;
    }

    /**
     * 获取用户余额
     * @return long
     */
    public long getUserBalance(String chainID, String publicKey) {
        try {
            AccountState accountState = tauController.getChainManager().getStateDB().getAccount(
                    chainID.getBytes(), ByteUtil.toByte(publicKey));
            if (accountState != null) {
                return accountState.getBalance().longValue();
            }
        } catch (Exception e) {
            logger.error("getUserPower error", e);
        }
        return 0L;
    }

    /**
     * 跟随链/社区
     * @param chainLink
     */
    public void followCommunity(String chainLink) {
        if (isRunning) {
            ChainLinkUtil.ChainLink decode = ChainLinkUtil.decode(chainLink);
            if (decode.isValid()) {
                boolean isSuccess = tauController.followChain(decode.getBytesDn(),
                        decode.getBytesBootstraps());
                logger.info("followCommunity chainLink::{}, isSuccess::{}", chainLink, isSuccess);
            }
        }
    }

    /**
     * 取消跟随链/社区
     * @param chainID
     */
    public void unfollowCommunity(String chainID) {
        if (isRunning) {
            boolean isSuccess = tauController.unfollowChain(chainID.getBytes());
            logger.info("unfollowChain chainID::{}, isSuccess::{}", chainID, isSuccess);
        }
    }

    /**
     *
     * @param memo
     * @return
     */
    public byte[] putForumNote(String memo) {
        if (isRunning) { }
        return new byte[20];
    }

    /**
     * 统计Sessions的nodes数
     */
    long getSessionNodes() {
        if (!isRunning) {
            return 0;
        }
        return tauController.getDHTEngine().getSessionNodes();
    }
}