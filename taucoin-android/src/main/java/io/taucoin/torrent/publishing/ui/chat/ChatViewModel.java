package io.taucoin.torrent.publishing.ui.chat;

import android.app.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgAndUser;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgStatus;
import io.taucoin.torrent.publishing.core.model.data.DataChanged;
import io.taucoin.torrent.publishing.core.model.data.Result;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgLog;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.HashUtil;
import io.taucoin.torrent.publishing.core.utils.MsgSplitUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.ui.constant.Page;
import io.taucoin.types.Message;
import io.taucoin.types.MessageType;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.CryptoUtil;

/**
 * 聊天相关的ViewModel
 */
public class ChatViewModel extends AndroidViewModel {

    private static final Logger logger = LoggerFactory.getLogger("ChatViewModel");
    private ChatRepository chatRepo;
    private UserRepository userRepo;
    private SettingsRepository settingsRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MutableLiveData<Result> chatResult = new MutableLiveData<>();
    private MutableLiveData<List<ChatMsgAndUser>> chatMessages = new MutableLiveData<>();
    private TauDaemon daemon;
    public ChatViewModel(@NonNull Application application) {
        super(application);
        chatRepo = RepositoryHelper.getChatRepository(getApplication());
        userRepo = RepositoryHelper.getUserRepository(getApplication());
        settingsRepo = RepositoryHelper.getSettingsRepository(getApplication());
        daemon = TauDaemon.getInstance(application);
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
    }

    /**
     * 观察查询的聊天信息
     */
    LiveData<List<ChatMsgAndUser>> observerChatMessages() {
        return chatMessages;
    }

    /**
     * 观察社区的消息的变化
     */
    Observable<DataChanged> observeDataSetChanged() {
        return chatRepo.observeDataSetChanged();
    }

    /**
     * 异步给朋友发信息任务
     * @param friendPk 朋友公钥
     * @param msg 消息
     * @param type 消息类型
     */
    void sendMessage(String friendPk, String msg, int type) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            Result result = syncSendMessageTask(friendPk, msg, type);
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> chatResult.postValue(result));
        disposables.add(disposable);
    }

    /**
     * 批量测试入口
     * 异步给朋友发信息任务
     * @param friendPk 朋友公钥
     */
    void sendBatchDebugMessage(String friendPk, int time, int msgSize) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            InputStream inputStream = null;
            try {
                inputStream = getApplication().getAssets().open("HarryPotter1-8.txt");
                byte[] bytes = new byte[msgSize];
                StringBuilder msg = new StringBuilder();
                for (int i = 0; i < time; i++) {
                    if (emitter.isCancelled()) {
                        break;
                    }
                    logger.debug("sendBatchDebugMessage available::{}", inputStream.available());
                    if (inputStream.available() < bytes.length) {
                        inputStream.reset();
                        logger.debug("sendBatchDebugMessage reset");
                    }
                    inputStream.read(bytes);
                    logger.debug("sendBatchDebugMessage read");
                    msg.append(i + 1);
                    msg.append("、");
                    msg.append(new String(bytes, StandardCharsets.UTF_8));
                    long startTime = System.currentTimeMillis();

                    syncSendMessageTask(friendPk, msg.toString(), MessageType.TEXT.ordinal());
                    long endTime = System.currentTimeMillis();
                    logger.debug("sendBatchDebugMessage no::{}, time::{}", i, endTime - startTime);
                    msg.setLength(0);
                }
                inputStream.close();
            } catch (Exception e) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ignore) {
                    }
                }
            }
            emitter.onNext(true);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        disposables.add(disposable);
    }

    private char getMsgPosition() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        if (testChatPos >= chars.length()) {
            testChatPos = 0;
        }
        char randomChar = chars.charAt(testChatPos);
        testChatPos += 1;
        return randomChar;
    }

    /**
     * 批量测试入口
     * 异步给朋友发数字信息任务
     * @param friendPk 朋友公钥
     */
    private int testChatPos = 0;
    void sendBatchDebugDigitMessage(String friendPk, int time) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            try {
                char randomChar = getMsgPosition();
                for (int i = 0; i < time; i++) {
                    if (emitter.isCancelled()) {
                        break;
                    }
                    String msg = randomChar + FmtMicrometer.fmtTestData( i + 1);
                    syncSendMessageTask(friendPk, msg, MessageType.TEXT.ordinal());
                }
            } catch (Exception ignore) {
            }
            emitter.onNext(true);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     * 同步给朋友发信息任务
     * @param friendPkStr 朋友公钥
     * @param msg 消息
     * @param type 消息类型
     */
    public Result syncSendMessageTask(String friendPkStr, String msg, int type) {
        Result result = new Result();
        AppDatabase.getInstance(getApplication()).runInTransaction(() -> {
            try {
                List<byte[]> contents;
                String logicMsgHashStr;
                if (type == MessageType.PICTURE.ordinal()) {
                    String progressivePath = MsgSplitUtil.compressAndScansPic(msg);
                    logicMsgHashStr = HashUtil.makeFileSha1HashWithTimeStamp(progressivePath);
                    contents = MsgSplitUtil.splitPicMsg(progressivePath);
                } else if(type == MessageType.TEXT.ordinal()) {
                    logicMsgHashStr = HashUtil.makeSha1HashWithTimeStamp(msg);
                    contents = MsgSplitUtil.splitTextMsg(msg);
                } else {
                    throw new Exception("Unknown message type");
                }
                byte[] logicMsgHash = ByteUtil.toByte(logicMsgHashStr);
                User user = userRepo.getCurrentUser();
                String senderPkStr = user.publicKey;
                byte[] senderPk = ByteUtil.toByte(senderPkStr);
                byte[] friendPk = ByteUtil.toByte(friendPkStr);
                ChatMsg[] messages = new ChatMsg[contents.size()];
                ChatMsgLog[] chatMsgLogs = new ChatMsgLog[contents.size()];
                int contentSize = contents.size();
                byte[] key = Utils.keyExchange(friendPkStr, user.seed);
                for (int nonce = 0; nonce < contentSize; nonce++) {
                    byte[] content = contents.get(nonce);
                    long millisTime = DateUtil.getMillisTime();
                    long timestamp = millisTime / 1000;
                    Message message;
                    if (type == MessageType.TEXT.ordinal()) {
                        message = Message.createTextMessage(BigInteger.valueOf(timestamp), senderPk,
                                friendPk, logicMsgHash, BigInteger.valueOf(nonce), content);
                    } else {
                        message = Message.createPictureMessage(BigInteger.valueOf(timestamp), senderPk,
                                friendPk, logicMsgHash, BigInteger.valueOf(nonce), content);
                    }
                    message.encrypt(key);
                    String hash = ByteUtil.toHexString(message.getHash());
                    byte[] encryptedContent = message.getEncryptedContent();
                    logger.debug("sendMessageTask newMsgHash::{}, contentType::{}, " +
                                    "nonce::{}, rawLength::{}, encryptedLength::{}, " +
                                    "logicMsgHash::{}, millisTime::{}",
                            hash, type, nonce, content.length,
                            null == encryptedContent ? 0 : encryptedContent.length,
                            logicMsgHashStr, DateUtil.format(millisTime, DateUtil.pattern9));

                    // 组织Message的结构，并发送到DHT和数据入库
                    messages[nonce] = new ChatMsg(hash, senderPkStr, friendPkStr, encryptedContent, type,
                            timestamp, nonce, logicMsgHashStr);

                    // 更新消息日志信息
                    chatMsgLogs[nonce] = new ChatMsgLog(hash,
                            ChatMsgStatus.BUILT.getStatus(), millisTime);

                }
                // 批量添加到数据库
                chatRepo.addChatMsgLogs(chatMsgLogs);
                chatRepo.addChatMessages(messages);
            } catch (Exception e) {
                logger.error("sendMessageTask error", e);
                result.setFailMsg(e.getMessage());
            }
        });
        chatRepo.submitDataSetChangedDirect(friendPkStr);
        return result;
    }

    /**
     * 观察消息日志信息
     */
    Observable<List<ChatMsgLog>> observerMsgLogs(String hash) {
        return chatRepo.observerMsgLogs(hash);
    }

    /**
     * 当留在该朋友聊天页面时，只访问该朋友
     * @param friendPk 要访问的朋友
     */
    public void startVisitFriend(String friendPk) {
        if (StringUtil.isEmpty(friendPk)) {
            return;
        }
        settingsRepo.setChattingFriend(friendPk);
    }

    /**
     * 当离开朋友聊天页面时，取消对朋友的单独访问
     */
    public void stopVisitFriend() {
        settingsRepo.setChattingFriend(null);
    }

    public void loadMessagesData(String friendPk, int pos) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<List<ChatMsgAndUser>>) emitter -> {
            List<ChatMsgAndUser> messages = new ArrayList<>();
            try {
                long startTime = System.currentTimeMillis();
                int pageSize = pos == 0 ? Page.PAGE_SIZE * 2 : Page.PAGE_SIZE;
                messages = chatRepo.getMessages(friendPk, pos, pageSize);
                long getMessagesTime = System.currentTimeMillis();
                logger.trace("loadMessagesData pos::{}, pageSize::{}, messages.size::{}",
                        pos, pageSize, messages.size());
                logger.trace("loadMessagesData getMessagesTime::{}", getMessagesTime - startTime);
                byte[] friendCryptoKey = Utils.keyExchange(friendPk, MainApplication.getInstance().getSeed());
                for (ChatMsgAndUser msg : messages) {
                    byte[] encryptedContent = msg.content;
                    try {
                        msg.rawContent = CryptoUtil.decrypt(encryptedContent, friendCryptoKey);
                        msg.content = null;
                    } catch (Exception e) {
                        logger.error("loadMessagesData decrypt error::", e);
                    }
                }
                long decryptTime = System.currentTimeMillis();
                logger.trace("loadMessagesData decryptTime Time::{}", decryptTime - getMessagesTime);
                Collections.reverse(messages);
                long endTime = System.currentTimeMillis();
                logger.trace("loadMessagesData reverseTime Time::{}", endTime - decryptTime);
            } catch (Exception e) {
                logger.error("loadMessagesData error::", e);
            }
            emitter.onNext(messages);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(messages -> {
                chatMessages.postValue(messages);
            });
        disposables.add(disposable);
    }
}