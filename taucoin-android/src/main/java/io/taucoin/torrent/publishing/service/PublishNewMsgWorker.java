package io.taucoin.torrent.publishing.service;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import io.taucoin.torrent.publishing.core.model.Frequency;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.types.Message;
import io.taucoin.types.MessageType;
import io.taucoin.util.ByteUtil;

/**
 * 发布新消息Worker
 * APP初次启动和发送新消息时触发此工作
 */
public class PublishNewMsgWorker extends Worker {

    private static final Logger logger = LoggerFactory.getLogger("PublishNewMsgWorker");
    private TauDaemon daemon;
    private ChatRepository chatRepo;
    public PublishNewMsgWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        logger.debug("constructor");
        daemon = TauDaemon.getInstance(context);
        chatRepo = RepositoryHelper.getChatRepository(context);
        logger.debug("constructor isStopped::{}", isStopped());
    }

    @NonNull
    @Override
    public Result doWork() {
        while (!isStopped()) {
            try {
                List<ChatMsg> list = chatRepo.getUnsentMessages();
                if (list != null && list.size() > 0) {
                    logger.debug("publishMessage list.size::{}", list.size());
                    for (ChatMsg msg : list) {
                        if (isStopped()) {
                            break;
                        }
                        publishMessage(msg);
                    }
                } else {
                    break;
                }
            } catch (Exception e) {
                logger.warn("publishMessage error ::{}", e.getMessage());
                try {
                    Thread.sleep(Frequency.FREQUENCY_RETRY.getFrequency());
                    logger.debug("publishMessage retry");
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }
        return Result.success();
    }

    /**
     * 发送消息
     */
    private void publishMessage(@NonNull ChatMsg msg) {
        if (isStopped()) {
            return;
        }
        List<byte[]> data = new ArrayList<>();
        Message message;
        byte[] friendPkBytes = ByteUtil.toByte(msg.friendPk);
        BigInteger nonce = BigInteger.valueOf(msg.nonce);
        BigInteger timestamp = BigInteger.valueOf(msg.timestamp);
        byte[] previousMsgDAGRoot = null;
        if (StringUtil.isNotEmpty(msg.previousMsgHash)) {
            previousMsgDAGRoot = ByteUtil.toByte(msg.previousMsgHash);
        }
        byte[] friendLatestMessageRoot = null;
        if (StringUtil.isNotEmpty(msg.friendLatestMsgHash)) {
            friendLatestMessageRoot = ByteUtil.toByte(msg.friendLatestMsgHash);
        }
        byte[] skipMessageRoot = null;
        if (StringUtil.isNotEmpty(msg.skipMsgHash)) {
            skipMessageRoot = ByteUtil.toByte(msg.skipMsgHash);
        }
        byte[] content;
        if (msg.contentType == MessageType.PICTURE.ordinal()) {
            content = ByteUtil.toByte(msg.content);
            message = Message.CreatePictureMessage(timestamp, nonce,
                    previousMsgDAGRoot, friendLatestMessageRoot, skipMessageRoot, content);
        } else {
            content = msg.content.getBytes(StandardCharsets.UTF_8);
            message = Message.CreateTextMessage(BigInteger.valueOf(msg.timestamp), nonce,
                    previousMsgDAGRoot, friendLatestMessageRoot, skipMessageRoot, content);
        }
        boolean isSendSuccess = daemon.sendMessage(friendPkBytes, message, data);
        logger.debug("NewMsgDAGRoot::{}, data.size::{}, isSendSuccess::{}", msg.hash,
                data.size(), isSendSuccess);
        if (isSendSuccess) {
            if (msg.contentType == MessageType.PICTURE.ordinal()) {
                msg.content = null;
            }
            msg.unsent = 1;
            chatRepo.updateChatMsg(msg);
            WorkerManager.startReceivedConfirmationWorker();
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        logger.debug("Worker onStopped");
    }
}