package io.taucoin.torrent.publishing.ui.chat;

import android.app.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

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
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgStatus;
import io.taucoin.torrent.publishing.core.model.data.Result;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgLog;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.MsgSplitUtil;
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
                ChatMsg[] messages = new ChatMsg[contents.size()];
                int contentSize = contents.size();
                for (int i = 0; i < contentSize; i++) {
                    String contentStr;
                    byte[] content;
                    int nonce = contentSize - 1 - i;
                    content = contents.get(nonce);
                    long millisTime = DateUtil.getMillisTime();
                    long timestamp = millisTime / 1000;
                    Message message;
                    if (type == MessageType.TEXT.ordinal()) {
                        contentStr = MsgSplitUtil.textMsgToString(content);
                        message = Message.CreateTextMessage(
                                BigInteger.valueOf(timestamp),
                                BigInteger.valueOf(nonce), content);
                    } else {
                        contentStr = ByteUtil.toHexString(content);
                        message = Message.CreatePictureMessage(
                                BigInteger.valueOf(timestamp),
                                BigInteger.valueOf(nonce),
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
                    messages[i] = chatMsg;

                    if (i == contentSize - 1) {
                        ChatMsgLog chatMsgLog = new ChatMsgLog(chatMsg.hash,
                                ChatMsgStatus.UNSENT.getStatus(), millisTime);
                        chatRepo.addChatMsgLog(chatMsgLog);
                    }
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
}