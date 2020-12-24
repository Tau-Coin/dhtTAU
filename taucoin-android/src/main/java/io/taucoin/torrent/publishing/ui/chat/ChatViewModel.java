package io.taucoin.torrent.publishing.ui.chat;

import android.app.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import io.taucoin.db.DBException;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.model.Frequency;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.MsgBlock;
import io.taucoin.torrent.publishing.core.model.data.Result;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgType;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.MultimediaUtil;
import io.taucoin.torrent.publishing.service.LibJpegManager;
import io.taucoin.torrent.publishing.service.WorkerManager;
import io.taucoin.torrent.publishing.ui.constant.Page;
import io.taucoin.types.HashList;
import io.taucoin.types.MessageType;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.HashUtil;

/**
 * 聊天相关的ViewModel
 */
public class ChatViewModel extends AndroidViewModel {

    private static final Logger logger = LoggerFactory.getLogger("ChatViewModel");
    private static final int BYTE_LIMIT = 900;
    private static final int HORIZONTAL_SIZE = 40;
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
                byte[] content;
                if (type == MessageType.PICTURE.ordinal()) {
                    throw new Exception("Unknown message type");
                } else if(type == MessageType.TEXT.ordinal()) {
                    content = handleMsgText(msg);
                } else {
                    throw new Exception("Unknown message type");
                }
                logger.debug("sendMessageTask content::{}", ByteUtil.toHexString(content));
                // 组织Message的结构，并发送到DHT和数据入库
                long timestamp = DateUtil.getTime();
                String senderPk = MainApplication.getInstance().getPublicKey();
                String contentStr = new String(content, StandardCharsets.UTF_8);
                ChatMsg chatMsg = new ChatMsg(senderPk, friendPk, contentStr, type, timestamp);
                chatMsg.status = ChatMsgType.UNSENT.ordinal();
                chatRepo.addChatMsg(chatMsg);
                WorkerManager.startPublishNewMsgWorker();
            } catch (Exception e) {
                logger.error("sendMessageTask error", e);
                result.setFailMsg(e.getMessage());
            }
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> chatResult.postValue(result));
        disposables.add(disposable);
    }

    /**
     * 处理消息文本成DAG形式
     */
    private byte[] handleMsgText(String msg) {
        byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
        int msgSize = msgBytes.length;
        if (msgSize > BYTE_LIMIT) {
            msgSize = BYTE_LIMIT;
        }
        byte[] bytes = new byte[msgSize];
        System.arraycopy(msgBytes, 0, bytes, 0, bytes.length);
        logger.debug("msgBytes::{}, bytes::{}", msgBytes.length, bytes.length);
        return bytes;
    }

    private ContentItem handleMsgTextToDAG(String msg) throws DBException {
        List<byte[]> hashList = new ArrayList<>();
        List<byte[]> contentList = new ArrayList<>();
        byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
        int msgSize = msgBytes.length;
        int statPos = 0;
        // 拆分消息数据
        do {
            int endPos = statPos + BYTE_LIMIT;
            if (endPos >= msgSize) {
                endPos = msgSize;
            }
            byte[] bytes = new byte[endPos - statPos];
            logger.debug("msgBytes::{}, bytes::{}", msgBytes.length, bytes.length);
            System.arraycopy(msgBytes, statPos, bytes, 0, bytes.length);
            byte[] fragmentHash = HashUtil.bencodeHash(bytes);
            daemon.saveMsg(fragmentHash, bytes);
            logger.debug("fragmentHash::{}, hashSize::{}, fragmentContent::{}", fragmentHash,
                    fragmentHash.length, new String(bytes,
                            StandardCharsets.UTF_8));
            hashList.add(fragmentHash);
            contentList.add(bytes);
            statPos = endPos;
        } while (statPos < msgSize);
        // 组织消息数据结构
        MsgBlock msgBlock = new MsgBlock();
        byte[] contentLink = null;
        List<byte[]> msgBlockList = new ArrayList<>();
        for (int i = hashList.size() - 1; i >= 0; i--) {
            if (msgBlockList.size() == 0) {
                msgBlock.setHorizontalHash(hashList.get(i));
            } else {
                msgBlock.setVerticalHash(msgBlockList.get(msgBlockList.size() - 1));
                msgBlock.setHorizontalHash(hashList.get(i));
            }
            byte[] msgBlockEncoded = msgBlock.getEncoded();
            byte[] msgBlockHash = HashUtil.bencodeHash(msgBlockEncoded);
            msgBlockList.add(msgBlockHash);
            contentList.add(msgBlockEncoded);
            contentLink = msgBlockHash;
            daemon.saveMsg(msgBlockHash, msgBlockEncoded);
            logger.debug("msgBlockHash::{}, hashSize::{}", msgBlockHash, msgBlockHash.length);
        }
        return ContentItem.newInstance(contentList, contentLink);
    }

    /**
     * 处理消息图片成DAG形式
     */
    private ContentItem handleMsgPicToDAG(String originalPath) throws Exception {
        String compressPath = LibJpegManager.getCompressFilePath();
        String progressivePath = LibJpegManager.getProgressiveFilePath();
        long startTime = System.currentTimeMillis();
        // 压缩图片
        MultimediaUtil.compressImage(originalPath, compressPath);
        long endTime = System.currentTimeMillis();
        logger.debug("compressImage:: times::{}ms", endTime - startTime);
        LibJpegManager.jpegScans(compressPath, progressivePath);
        return saveImageToLevelDB(progressivePath);
    }

    private ContentItem saveImageToLevelDB(String progressivePath) throws Exception{
        List<byte[]> contentList = new ArrayList<>();
        List<byte[]> allList = new ArrayList<>();
        logger.debug("saveImageToLevelDB start");
        long startTime = System.currentTimeMillis();
        File file = new File(progressivePath);
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[BYTE_LIMIT];
        List<byte[]> list = new ArrayList<>();
        List<byte[]> hashList = new ArrayList<>();
        int num;
        byte[] msg;
        while (true) {
            num = fis.read(buffer);
            if (num != -1) {
                msg = new byte[num];
                System.arraycopy(buffer, 0, msg, 0, num);
                byte[] msgHash = HashUtil.bencodeHash(msg);
                list.add(msgHash);
                contentList.add(msg);
                allList.add(msgHash);
                daemon.saveMsg(msgHash, msg);
            }
            if (list.size() == HORIZONTAL_SIZE || num == -1) {
                HashList msgHashList = new HashList(list);
                byte[] listEncoded =  msgHashList.getEncoded();
                byte[] listHash = HashUtil.bencodeHash(listEncoded);
                hashList.add(listHash);
                contentList.add(listEncoded);
                allList.add(listHash);
                daemon.saveMsg(listHash, listEncoded);
                list.clear();
                if (num == -1) {
                    break;
                }
            }
        }
        MsgBlock latterBlock = new MsgBlock();
        int hashListSize = hashList.size();
        byte[] contentLink = null;
        for (int i = hashListSize - 1; i >= 0 ; i--) {
            byte[] listHash = hashList.get(i);
            boolean isSaveMsg = false;
            if (!latterBlock.isHaveVerticalHash() && i != hashListSize - 1) {
                latterBlock.setVerticalHash(listHash);
                if (i == 0) {
                    isSaveMsg = true;
                }
            } else {
                latterBlock.setHorizontalHash(listHash);
                isSaveMsg = true;
            }
            if (isSaveMsg) {
                byte[] blockEncoded = latterBlock.getEncoded();
                byte[] blockHash = HashUtil.bencodeHash(blockEncoded);
                daemon.saveMsg(blockHash, blockEncoded);
                contentLink = blockHash;
                allList.add(blockHash);
                contentList.add(blockEncoded);

                latterBlock = new MsgBlock();
                latterBlock.setVerticalHash(blockHash);
            }
        }
        long endTime = System.currentTimeMillis();
        logger.debug("saveImageToLevelDB end times::{}ms", endTime - startTime);
        fis.close();
        for (int i = 0; i < allList.size(); i++) {
            logger.debug("sendMessageTask data.size::{}, contentLink::{}",
                    i, ByteUtil.toHexString(allList.get(i)));
        }
        return ContentItem.newInstance(contentList, contentLink);
    }

    static class ContentItem {
        private List<byte[]> contentList;
        private byte[] contentLink;

        ContentItem(List<byte[]> contentList, byte[] contentLink) {
            this.contentList = contentList;
            this.contentLink = contentLink;
        }

        byte[] getContentLink() {
            return contentLink;
        }

        public List<byte[]> getList() {
            return contentList;
        }

        static ContentItem newInstance(List<byte[]> contentList,
                                       byte[] contentLink) {
            return new ContentItem(contentList, contentLink);
        }
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
                Frequency.FREQUENCY_PUBLISH_WRITING.getFrequency(), TimeUnit.SECONDS)
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
        daemon.writingToFriend(friendPk);
    }
}