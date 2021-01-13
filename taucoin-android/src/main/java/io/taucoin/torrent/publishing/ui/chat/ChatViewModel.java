package io.taucoin.torrent.publishing.ui.chat;

import android.app.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.model.Frequency;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.Result;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgLog;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.MsgSplitUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.service.WorkerManager;
import io.taucoin.torrent.publishing.ui.constant.Page;
import io.taucoin.types.Message;
import io.taucoin.types.MessageType;
import io.taucoin.util.ByteUtil;

/**
 * 聊天相关的ViewModel
 */
public class ChatViewModel extends AndroidViewModel {

    private static final Logger logger = LoggerFactory.getLogger("ChatViewModel");
    private ChatRepository chatRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MutableLiveData<Result> chatResult = new MutableLiveData<>();
    private TauDaemon daemon;
    private Disposable observeDaemonRunning;
    private Disposable writingToFriendTimer;
    private ChatSourceFactory sourceFactory;
    public ChatViewModel(@NonNull Application application) {
        super(application);
        chatRepo = RepositoryHelper.getChatRepository(getApplication());
        daemon = TauDaemon.getInstance(application);
        sourceFactory = new ChatSourceFactory(chatRepo);
    }

    public MutableLiveData<Result> getChatResult() {
        return chatResult;
    }

    public void observeNeedStartDaemon () {
        disposables.add(daemon.observeNeedStartDaemon()
                .subscribeOn(Schedulers.io())
                .filter((needStart) -> needStart)
                .subscribe((needStart) -> daemon.start()));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
        if (observeDaemonRunning != null && !observeDaemonRunning.isDisposed()) {
            observeDaemonRunning.dispose();
        }
        resumeGossipTimeInternal();
    }

    /**
     * 观察和朋友聊天
     * @param friendPK 朋友公钥
     * @return LiveData
     */
    LiveData<PagedList<ChatMsg>> observerChat(String friendPK) {
        sourceFactory.setFriendPk(friendPK);
        return new LivePagedListBuilder<>(sourceFactory, Page.getPageListConfig())
                .setInitialLoadKey(Page.PAGE_SIZE)
                .build();
    }

    /**
     * 给朋友发信息任务
     * @param friendPK 朋友公钥
     * @param msg 消息
     * @param type 消息类型
     */
    void sendMessage(String friendPK, String msg, int type) {
        if (observeDaemonRunning != null && !observeDaemonRunning.isDisposed()) {
            return;
        }
        observeDaemonRunning = daemon.observeDaemonRunning()
                .subscribeOn(Schedulers.io())
                .subscribe((isRunning) -> {
                    if (isRunning) {
                        sendMessageTask(friendPK, msg, type);
                        if (observeDaemonRunning != null) {
                            observeDaemonRunning.dispose();
                        }
                    }
                });
    }

    /**
     * 给朋友发信息任务
     * @param friendPk 朋友公钥
     * @param msg 消息
     * @param type 消息类型
     */
    private void sendMessageTask(String friendPk, String msg, int type) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            Result result = new Result();
            try {
                List<byte[]> contents;
                if (type == MessageType.PICTURE.ordinal()) {
                    String progressivePath = MsgSplitUtil.compressAndScansPic(msg);
                    contents = MsgSplitUtil.splitPicMsg(progressivePath);
                } else if(type == MessageType.TEXT.ordinal()) {
                    contents = MsgSplitUtil.splitTextMsg(msg);
                } else {
                    throw new Exception("Unknown message type");
                }
                byte[] friendPkBytes = ByteUtil.toByte(friendPk);
                byte[] friendLatestMsgHash = daemon.getFriendLatestRoot(friendPkBytes);
                byte[] previousMsgDAGRoot = null;
                byte[] skipMessageRoot = null;
                ChatMsg latestBuiltAndUnsentMsg = chatRepo.getLatestBuiltAndUnsentMsg();
                if (latestBuiltAndUnsentMsg != null) {
                    previousMsgDAGRoot = ByteUtil.toByte(latestBuiltAndUnsentMsg.hash);
                    skipMessageRoot = previousMsgDAGRoot;
                } else {
                    // 如果当前message的nonce为0，最新的msgRoot就是message的skipMessageRoot
                    // 否则最新msgRoot对应的skipMessageRoot，就是message的skipMessageRoot
                    previousMsgDAGRoot = daemon.getMyLatestMsgRoot(friendPkBytes);
                    skipMessageRoot = previousMsgDAGRoot;
                }
                ChatMsg[] messages = new ChatMsg[contents.size()];
                int contentSize = contents.size();
                for (int i = 0; i < contentSize; i++) {
                    String contentStr;
                    byte[] content;
                    int nonce = contentSize - 1 - i;
                    content = contents.get(nonce);
                    long timestamp = DateUtil.getTime();
                    Message message;
                    if (type == MessageType.TEXT.ordinal()) {
                        contentStr = MsgSplitUtil.textMsgToString(content);
                        message = Message.CreateTextMessage(
                                BigInteger.valueOf(timestamp),
                                BigInteger.valueOf(nonce),
                                previousMsgDAGRoot,
                                friendLatestMsgHash,
                                skipMessageRoot, content);
                    } else {
                        contentStr = ByteUtil.toHexString(content);
                        message = Message.CreatePictureMessage(
                                BigInteger.valueOf(timestamp),
                                BigInteger.valueOf(nonce),
                                previousMsgDAGRoot,
                                friendLatestMsgHash,
                                skipMessageRoot,
                                content);
                    }
                    String hash = ByteUtil.toHexString(message.getHash());
                    logger.debug("sendMessageTask hash::{}, contentType::{}, nonce::{}, contentSize::{}",
                            hash, type, i, content.length);
                    logger.trace("sendMessageTask hash::{}, content::{}", hash, contentStr);
                    // 组织Message的结构，并发送到DHT和数据入库
                    String senderPk = MainApplication.getInstance().getPublicKey();
                    ChatMsg chatMsg = new ChatMsg(hash, senderPk, friendPk, contentStr, type,
                            timestamp, nonce);
                    if (previousMsgDAGRoot != null) {
                        chatMsg.previousMsgHash = ByteUtil.toHexString(previousMsgDAGRoot);
                        logger.trace("sendMessageTask hash::{}, previousMsgHash::{}",
                                hash, chatMsg.previousMsgHash);
                    }
                    if (skipMessageRoot != null) {
                        chatMsg.skipMsgHash = ByteUtil.toHexString(skipMessageRoot);
                        logger.trace("sendMessageTask hash::{}, skipMsgHash::{}",
                                hash,chatMsg.skipMsgHash);
                    }
                    if (friendLatestMsgHash != null) {
                        chatMsg.friendLatestMsgHash = ByteUtil.toHexString(friendLatestMsgHash);
                        logger.trace("sendMessageTask hash::{}, friendLatestMsgHash::{}",
                                hash, chatMsg.friendLatestMsgHash);
                    }
                    messages[i] = chatMsg;
                    previousMsgDAGRoot = message.getHash();
                }
                // 批量添加到数据库
                chatRepo.addChatMessages(messages);
                WorkerManager.startPublishNewMsgWorker();
            } catch (Exception e) {
                logger.error("sendMessageTask error", e);
                result.setFailMsg(e.getMessage());
            }
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> chatResult.postValue(result));
        disposables.add(disposable);
    }

    Observable<List<ChatMsgLog>> observerMsgLogs(String hash) {
        return chatRepo.observerMsgLogs(hash);
    }

    /**
     * 更新Gossip时间间隔
     * @
     */
    private void updateGossipTimeInternal() {
        daemon.setGossipTimeInterval(Frequency.GOSSIP_FREQUENCY_HEIGHT.getFrequency());
    }

    /**
     * 更新Gossip时间间隔
     */
    private void resumeGossipTimeInternal() {
        if (writingToFriendTimer != null && !writingToFriendTimer.isDisposed()) {
            writingToFriendTimer.dispose();
        }
        daemon.updateGossipTimeInterval();
    }

    /**
     * 用户在聊天页面，触发定时通知朋友更新
     * @param friendPK
     */
    void writingToFriendTimer(String friendPK) {
        if (writingToFriendTimer != null && !writingToFriendTimer.isDisposed()) {
            logger.debug("writingToFriend friendPK::{} Timer is running", friendPK);
            return;
        }
        updateGossipTimeInternal();
        writingToFriend(friendPK);
        writingToFriendTimer = Observable.interval(
                Frequency.GOSSIP_FREQUENCY_HEIGHT.getFrequency(), TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    writingToFriend(friendPK);
                });
    }

    /**
     * 通知朋友正在输入状态
     * @param friendPk
     */
    private void writingToFriend(String friendPk) {
        logger.debug("writingToFriend friendPk::{}", friendPk);
        try {
            daemon.writingToFriend(friendPk);
        } catch (Exception e) {
            logger.error("writingToFriend error", e);
        }
    }
}