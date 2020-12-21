package io.taucoin.torrent.publishing.core.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.frostwire.jlibtorrent.Ed25519;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
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
import io.taucoin.communication.CommunicationManager;
import io.taucoin.controller.TauController;
import io.taucoin.core.AccountState;
import io.taucoin.db.DBException;
import io.taucoin.genesis.GenesisConfig;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.leveldb.AndroidLeveldbFactory;
import io.taucoin.torrent.publishing.core.utils.ChainLinkUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.NetworkSetting;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.receiver.ConnectionReceiver;
import io.taucoin.torrent.publishing.service.WorkerManager;
import io.taucoin.torrent.publishing.service.SystemServiceManager;
import io.taucoin.torrent.publishing.receiver.PowerReceiver;
import io.taucoin.torrent.publishing.service.Scheduler;
import io.taucoin.torrent.publishing.service.TauService;
import io.taucoin.types.BlockContainer;
import io.taucoin.types.Message;
import io.taucoin.types.Transaction;
import io.taucoin.util.ByteUtil;

/**
 * 区块链业务Daemon
 */
public class TauDaemon {
    private static final String TAG = TauDaemon.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(TAG);
    private static final Logger msgLogger = LoggerFactory.getLogger("TAU messaging");
    private static final int initDHTOPInterval = 30 * 1000;
    private static final int maxSessions = 8;
    private static final int sessionStartedTime = 10 * 1000;

    private Context appContext;
    private SettingsRepository settingsRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private PowerReceiver powerReceiver = new PowerReceiver();
    private ConnectionReceiver connectionReceiver = new ConnectionReceiver();
    private TauController tauController;
    private PowerManager.WakeLock wakeLock;
    private SystemServiceManager systemServiceManager;
    private ExecutorService exec = Executors.newSingleThreadExecutor();
    private TauListenHandler tauListenHandler;
    private MsgListenHandler msgListenHandler;
    private TauInfoProvider tauInfoProvider;
    private Disposable sessionStartedDispose; // session启动任务
    private volatile boolean isRunning = false;
    private volatile String seed;

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
        logger.info("TauController repoPath::{}", repoPath);
        tauController = new TauController(repoPath, androidLeveldbFactory);
        tauController.registerListener(daemonListener);
        tauController.registerMsgListener(msgListener);
//        tauController.getDHTEngine().regulateDHTOPInterval(initDHTOPInterval);

        switchPowerReceiver();
        switchConnectionReceiver();
    }

    /**
     * 更新用户Seed
     * @param seed Seed
     */
    public void updateSeed(String seed) {
        if (StringUtil.isEmpty(seed) || StringUtil.isEquals(seed, this.seed)) {
            return;
        }
        this.seed = seed;
        logger.debug("updateSeed ::{}", seed);
        byte[] bytesSeed = ByteUtil.toByte(seed);
        if (isRunning) {
            tauController.updateKey(bytesSeed);
        } else {
            tauController.updateKey(Ed25519.createKeypair(bytesSeed));
        }
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
    private final MsgListener msgListener = new MsgListener() {

        @Override
        public void onNewMessage(byte[] friend, Message message) {
            msgListenHandler.onNewMessage(friend, message);
        }

        @Override
        public void onReadMessageRoot(byte[] friend, byte[] root) {
            msgListenHandler.onReceivedMessageRoot(friend, root);
        }

        @Override
        public void onDiscoveryFriend(byte[] friend) {
            msgListenHandler.onDiscoveryFriend(friend);
        }
    };

    /**
     * 链端事件监听逻辑处理
     */
    private final TauDaemonListener daemonListener = new TauDaemonListener() {
        @Override
        public void onTauStarted(boolean success, String errMsg) {
            if (success) {
                logger.debug("Tau start successfully");
                isRunning = true;
                WorkerManager.startAllWorker();
                handleSettingsChanged(appContext.getString(R.string.pref_key_foreground_running));
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
        rescheduleDHTBySettings();
        resetDHTSessions(1);
        tauController.start(NetworkSetting.getDHTSessions());
        subscribeSessionStarted();
    }

    /**
     * Only calls from TauService
     */
    public void doStop() {
        if (!isRunning)
            return;
        isRunning = false;
        WorkerManager.cancelAllWork();
        disposables.clear();
        if (sessionStartedDispose != null
                && !sessionStartedDispose.isDisposed()) {
            sessionStartedDispose.dispose();
        }
        tauListenHandler.destroy();
        msgListenHandler.destroy();
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
            rescheduleDHTBySettings(true);
            enableServerMode(settingsRepo.serverMode());
        } else if (key.equals(appContext.getString(R.string.pref_key_charging_state))) {
            logger.info("SettingsChanged, charging state::{}", settingsRepo.chargingState());
            enableServerMode(settingsRepo.serverMode());
        } else if (key.equals(appContext.getString(R.string.pref_key_is_metered_network))) {
            logger.info("clearSpeedList, isMeteredNetwork::{}", NetworkSetting.isMeteredNetwork());
        } else if (key.equals(appContext.getString(R.string.pref_key_foreground_running))) {
            logger.info("foreground running::{}", settingsRepo.getBooleanValue(key));
            updateGossipTimeInterval();
        }
    }

    /**
     * 更新Gossip的时间间隔
     * 1、如果APP在前台，非计费网络是5s, 计费网络是10s
     * 2、如果APP在后台，非计费网络是30s, 计费网络是60s
     * 3、如果用户在聊天页面1s, 退出恢复1、2处理
     */
    public void updateGossipTimeInterval() {
        if (!isRunning) {
            return;
        }
        boolean foregroundRunning = settingsRepo.getBooleanValue(
                appContext.getString(R.string.pref_key_foreground_running));
        boolean isMeteredNetwork = NetworkSetting.isMeteredNetwork();
        long gossipFrequency;
        if (foregroundRunning) {
            // APP在前台，非计费网络是5s, 计费网络是10s
            gossipFrequency = isMeteredNetwork ?
                    Frequency.GOSSIP_FREQUENCY_DEFAULT.getFrequency() :
                    Frequency.GOSSIP_FREQUENCY_MEDIUM_HEIGHT.getFrequency();
        } else {
            // APP在后台，非计费网络是30s, 计费网络是60s
            gossipFrequency = isMeteredNetwork ?
                    Frequency.GOSSIP_FREQUENCY_LOW.getFrequency() :
                    Frequency.GOSSIP_FREQUENCY_MEDIUM_LOW.getFrequency();
        }
        setGossipTimeInterval(gossipFrequency);
    }

    /**
     * 根据当前设置重新调度DHT
     */
    void rescheduleDHTBySettings() {
        rescheduleDHTBySettings(false);
    }

    private synchronized void rescheduleDHTBySettings(boolean isRestart) {
        if (!isRunning) {
            return;
        }
        try {
            // 判断有无网络连接
            if (settingsRepo.internetState()) {
                if (isRestart) {
                    resetDHTSessions(1);
                    tauController.restartSessions(NetworkSetting.getDHTSessions());
                    logger.info("rescheduleDHTBySettings restartSessions::{}",
                            NetworkSetting.getDHTSessions());
                } else {
//                    int regulateValue = NetworkSetting.calculateRegulateValue();
//                    if (regulateValue > 0) {
//                        tauController.getDHTEngine().increaseDHTOPInterval();
//                    } else if (regulateValue < 0) {
//                        if ((sessionStartedDispose != null && !sessionStartedDispose.isDisposed())) {
//                            return;
//                        }
//                        int dhtSessions = NetworkSetting.getDHTSessions();
//                        if (dhtSessions < maxSessions) {
//                            tauController.getDHTEngine().decreaseDHTOPInterval();
//                            tauController.getDHTEngine().increaseSession();
//                            resetDHTSessions(dhtSessions + 1);
//                        }
//                    }
                    logger.info("rescheduleDHTBySettings DHTSessions::{}, DHTOPInterval::{}",
                            NetworkSetting.getDHTSessions(), tauController.getDHTEngine().getDHTOPInterval());
                }
            }
        } catch (Exception e) {
            logger.error("rescheduleDHTBySettings errors", e);
        }
    }

    /**
     * 重置DHT Sessions个数
     */
    private void resetDHTSessions(int dhtSessions) {
        dhtSessions = 8;
        NetworkSetting.updateDHTSessions(dhtSessions);
        if (dhtSessions != maxSessions) {
            subscribeSessionStarted();
        }
    }

    /**
     * 订阅Session启动稳定，sessionStartedTime稳定时间
     */
    private void subscribeSessionStarted() {
        sessionStartedDispose = Observable.timer(sessionStartedTime, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe();
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

    private CommunicationManager getCommunicationManager() {
        return tauController.getCommunicationManager();
    }

    /**
     * 保存消息进levelDB
     * @param hash
     * @param msg
     * @throws DBException
     */
    public void saveMsg(byte[] hash, byte[] msg) throws DBException{
        getCommunicationManager().getMessageDB().putMessage(hash, msg);
    }

    /**
     * 获取消息从levelDB
     * @param hash
     * @return msg
     * @throws DBException
     */
    public byte[] getMsg(byte[] hash) throws DBException {
        return  getCommunicationManager().getMessageDB().getMessageByHash(hash);
    }

    /**
     * 请求消息数据
     * @param hash
     */
    public void requestMessageData(byte[] hash) {
        getCommunicationManager().requestMessageData(hash);
    }

    /**
     * 添加朋友
     * @param friendPk 朋友公钥
     */
    public void addNewFriend(byte[] friendPk) {
        if (!isRunning) {
            return;
        }
        getCommunicationManager().addNewFriend(friendPk);
        logger.info("addNewFriend friendPk::{}", ByteUtil.toHexString(friendPk));
    }

    /**
     * 发送消息
     * @param friendPK
     * @param message
     * @param data
     * @return
     */
    public boolean sendMessage(byte[] friendPK, Message message, List<byte[]> data) {
        boolean isPublishSuccess = getCommunicationManager().publishNewMessage(friendPK, message, data);
        logger.debug("sendMessage isPublishSuccess{}", isPublishSuccess);
        String content = new String(message.getContent(), StandardCharsets.UTF_8);
        String hash = ByteUtil.toHexString(message.getHash());
        msgLogger.debug("sendMessage friendPk::{}, hash::{}, timestamp::{}, content::{}",
                ByteUtil.toHexString(friendPK), hash,
                DateUtil.formatTime(DateUtil.getTime(), DateUtil.pattern6),
                content);
        return isPublishSuccess;
    }

    /**
     * 获取用户信息root
     * @param userPk
     * @return root hash
     */
    public byte[] getMyLatestMsgRoot(byte[] userPk) {
        return getCommunicationManager().getMyLatestMsgRoot(userPk);
    }

    /**
     * 获取朋友最新信息root
     * @param friendPK
     * @return root hash
     */
    public byte[] getFriendLatestRoot(byte[] friendPK) {
        return getCommunicationManager().getFriendLatestRoot(friendPK);
    }

    /**
     * 获取朋友已接收的最新信息root
     * @param friendPK
     * @return root hash
     */
    public byte[] getFriendConfirmationRoot(byte[] friendPK) {
        return getCommunicationManager().getFriendConfirmationRoot(friendPK);
    }

    /**
     * 根据APP前后台状态调整gossip的时间间隔
     * @param timeInterval 时间间隔
     */
    public void setGossipTimeInterval(long timeInterval) {
        if (!isRunning) {
            return;
        }
        getCommunicationManager().setGossipTimeInterval(timeInterval);
        logger.debug("setGossipTimeInterval::{}", timeInterval);
    }

    /**
     * 发布Immutable数据
     * @param data Immutable Data
     */
    public void publishImmutableData(byte[] data) {
        getCommunicationManager().publishImmutableData(data);
    }

    /**
     * 发布最后一条消息
     * @param friendPk
     */
    public void publishLastMessage(byte[] friendPk) {
        if (!isRunning) {
            return;
        }
        getCommunicationManager().publishLastMessage(friendPk);
        logger.debug("publishLastMessage friendPk::{} successfully", friendPk);
    }
}
