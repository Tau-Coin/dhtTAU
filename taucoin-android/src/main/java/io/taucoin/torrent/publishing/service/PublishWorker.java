package io.taucoin.torrent.publishing.service;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import io.taucoin.db.DBException;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.MsgBlock;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgType;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.types.HashList;
import io.taucoin.types.Message;
import io.taucoin.types.MessageType;
import io.taucoin.util.ByteUtil;

/**
 * 发布消息Worker
 */
public class PublishWorker extends Worker {

    private static final Logger logger = LoggerFactory.getLogger("PublishWorker");
    private TauDaemon daemon;
    private ChatRepository chatRepo;
    public PublishWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        logger.debug("constructor");
        daemon = TauDaemon.getInstance(context);
        chatRepo = RepositoryHelper.getChatRepository(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        logger.debug("doWork");
        while (true) {
            try {
                logger.debug("publishMessage start");
                List<ChatMsg> list = chatRepo.getUnsentMessages();
                if (list != null && list.size() > 0) {
                    logger.debug("publishMessage list.size::{}", list.size());
                    publishMessage(list);
                } else {
                    break;
                }
            }catch (Exception e) {
                logger.warn("publishMessage error ::{}", e.getMessage());
                try {
                    Thread.sleep(1000);
                    logger.debug("publishMessage retry");
                } catch (InterruptedException ignore) {
                }
            }
        }
        return Result.success();
    }

    /**
     * 发布消息
     * APP前端运行触发put immutable 数据，后台APP只会gossip, 不从UI主动触发put immutable 数据
     * APP前端运行触发put immutable 数据范围覆盖前10秒，
     * 当从gossip中捕捉到demand, put immutable 数据范围覆盖也是10秒
     */
    private void publishMessage(@NonNull List<ChatMsg> list) {
        for (ChatMsg msg : list) {
            try {
                byte[] friendPkBytes = ByteUtil.toByte(msg.friendPk);
                byte[] timestamp = ByteUtil.longToBytes(msg.timestamp);
                byte[] contentLink = ByteUtil.toByte(msg.contextLink);
                byte[] previousMsgDAGRoot = daemon.getMyLatestMsgRoot(friendPkBytes);
                byte[] friendLatestMessageRoot = daemon.getFriendLatestRoot(friendPkBytes);
                logger.debug("previousMsgDAGRoot::{}", ByteUtil.toHexString(previousMsgDAGRoot));
                Message message;
                List<byte[]> data = new ArrayList<>();
                if (msg.contextType == MessageType.PICTURE.ordinal()) {
                    message = Message.CreatePictureMessage(timestamp,
                            previousMsgDAGRoot, friendLatestMessageRoot, contentLink);
                    parseMsgPicData(contentLink, data);
                } else {
                    message = Message.CreateTextMessage(timestamp,
                            previousMsgDAGRoot, friendLatestMessageRoot, contentLink);
                    parseMsgTextData(contentLink, data);
                }
                boolean isSendSuccess = daemon.sendMessage(friendPkBytes, message, data);
                String hash = ByteUtil.toHexString(message.getHash());
                logger.debug("NewMsgDAGRoot::{}, data.size::{}, isSendSuccess::{}", hash,
                        data.size(), isSendSuccess);
                if (isSendSuccess) {
                    msg.hash = hash;
                    msg.status = ChatMsgType.QUEUED.ordinal();
                    chatRepo.updateChatMsg(msg);
                    PublishManager.startReceivedConfirmationWorker();
                } else {
                    Thread.sleep(2000);
                }
            } catch (DBException e) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignore) {
                }
            } catch (Exception e) {
                logger.warn("publishMessage error:: {}", e.getMessage());
            }
        }
    }

    /**
     * 解析图片信息原始数据
     * @return List<byte[]>
     */
    private void parseMsgPicData(byte[] contentLink, List<byte[]> data) throws DBException {
        byte[] encoded = daemon.getMsg(contentLink);
        data.add(encoded);
        MsgBlock msgBlock = new MsgBlock(encoded);
        if (msgBlock.isHaveHorizontalHash()) {
            HashList hashList = new HashList(msgBlock.getHorizontalHash());
            List<byte[]> fragments = hashList.getHashList();
            if (fragments != null) {
                for (byte[] hash : fragments) {
                    byte[] fragment = daemon.getMsg(hash);
                    data.add(fragment);
                }
            }
        }
        if (msgBlock.isHaveVerticalHash()) {
            parseMsgTextData(msgBlock.getVerticalHash(), data);
        }
    }

    /**
     * 解析文本信息原始数据
     */
    private void parseMsgTextData(byte[] contentLink, List<byte[]> data) throws DBException {
        byte[] encoded = daemon.getMsg(contentLink);
        data.add(encoded);
        MsgBlock msgBlock = new MsgBlock(encoded);
        if (msgBlock.isHaveHorizontalHash()) {
            byte[] fragment = daemon.getMsg(msgBlock.getHorizontalHash());
            data.add(fragment);
        }
        if (msgBlock.isHaveVerticalHash()) {
            parseMsgTextData(msgBlock.getVerticalHash(), data);
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        logger.debug("PublishWorker onStopped");
    }
}