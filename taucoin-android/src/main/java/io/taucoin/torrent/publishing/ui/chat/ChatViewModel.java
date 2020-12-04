package io.taucoin.torrent.publishing.ui.chat;

import android.app.Application;
import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.db.DBException;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.MsgBlock;
import io.taucoin.torrent.publishing.core.model.data.Result;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Chat;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.torrent.publishing.core.utils.MultimediaUtil;
import io.taucoin.torrent.publishing.service.LibJpegManager;
import io.taucoin.torrent.publishing.ui.constant.Page;
import io.taucoin.types.HashList;
import io.taucoin.types.Message;
import io.taucoin.types.MessageType;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.HashUtil;

/**
 * 聊天相关的ViewModel
 */
public class ChatViewModel extends AndroidViewModel {

    private static final Logger logger = LoggerFactory.getLogger("ChatViewModel");
    private static final int BYTE_LIMIT = 1000;
    private static final int HORIZONTAL_SIZE = 40;
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
    LiveData<PagedList<Chat>> observerChat(String friendPK) {
        sourceFactory.setFriendPk(friendPK);
        return new LivePagedListBuilder<>(sourceFactory, Page.getPageListConfig())
                .setInitialLoadKey(Page.PAGE_SIZE)
                .build();
    }

    /**
     * 给朋友发信息
     * @param friendPK 朋友公钥
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

    private void sendMessageTask(String friendPK, String msg, int type) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            Result result = new Result();
            try {
                List<byte[]> data;
                if (type == MessageType.PICTURE.ordinal()) {
                    data = handleMsgPicToDAG(msg);
                } else if(type == MessageType.TEXT.ordinal()) {
                    data = handleMsgTextToDAG(msg);
                } else {
                    throw new Exception("Unknown message type");
                }
                // 组织Message的结构，并发送到DHT和数据入库
                byte[] contentLink = data.get(data.size() - 1);
                Message message = daemon.sendMessage(ByteUtil.toByte(friendPK), contentLink, data);
                String hash = ByteUtil.toHexString(message.getHash());
                Chat chat = chatRepo.queryChatByHash(hash);
                if (null == chat) {
                    String senderPk = MainApplication.getInstance().getPublicKey();
                    String contentLinkStr = ByteUtil.toHexString(contentLink);
                    long timestamp = ByteUtil.byteArrayToLong(message.getTimestamp());
                    chat = new Chat(hash, senderPk, friendPK, contentLinkStr, type, timestamp);
                    chatRepo.addChat(chat);
                }
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
    private List<byte[]> handleMsgTextToDAG(String msg) throws DBException {
        List<byte[]> hashList = new ArrayList<>();
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
            statPos = endPos;
        } while (statPos < msgSize);
        // 组织消息数据结构
        MsgBlock msgBlock = new MsgBlock();
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
            daemon.saveMsg(msgBlockHash, msgBlockEncoded);
            logger.debug("msgBlockHash::{}, hashSize::{}", msgBlockHash, msgBlockHash.length);
        }
        hashList.addAll(msgBlockList);
        return hashList;
    }

    /**
     * 处理消息图片成DAG形式
     */
    private List<byte[]> handleMsgPicToDAG(String originalPath) throws Exception {
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

    private List<byte[]> saveImageToLevelDB(String progressivePath) throws Exception{
        List<byte[]> allHashList = new ArrayList<>();
        logger.debug("saveImageToLevelDB start");
        long startTime = System.currentTimeMillis();
        File file = new File(progressivePath);
        FileInputStream fis = new FileInputStream(file);
        int horizontal_size = 40;
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
                daemon.saveMsg(msgHash, msg);
            }
            if (list.size() == horizontal_size || num == -1) {
                HashList msgHashList = new HashList(list);
                byte[] listEncoded =  msgHashList.getEncoded();
                byte[] listHash = HashUtil.bencodeHash(listEncoded);
                hashList.add(listHash);
                daemon.saveMsg(listHash, listEncoded);
                allHashList.addAll(list);
                list.clear();
                if (num == -1) {
                    break;
                }
            }
        }
        MsgBlock latterBlock = new MsgBlock();
        int hashListSize = hashList.size();
        allHashList.addAll(hashList);
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
                byte[] blockHash = HashUtil.sha1hash(blockEncoded);
                daemon.saveMsg(blockHash, blockEncoded);
                allHashList.add(blockHash);

                latterBlock = new MsgBlock();
                latterBlock.setVerticalHash(blockHash);
            }
        }
        long endTime = System.currentTimeMillis();
        logger.debug("saveImageToLevelDB end times::{}ms", endTime - startTime);
        fis.close();
        return allHashList;
    }
}