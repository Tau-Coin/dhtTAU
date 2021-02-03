package io.taucoin.torrent.publishing.service;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import io.taucoin.torrent.publishing.core.model.Interval;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
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
                    Thread.sleep(Interval.INTERVAL_RETRY.getInterval());
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
        Message message;
        byte[] friendPkBytes = ByteUtil.toByte(msg.friendPk);
        BigInteger nonce = BigInteger.valueOf(msg.nonce);
        BigInteger timestamp = BigInteger.valueOf(msg.timestamp);
        byte[] content;
        if (msg.contentType == MessageType.PICTURE.ordinal()) {
            content = ByteUtil.toByte(msg.content);
            message = Message.createPictureMessage(timestamp, nonce, content);
        } else {
            content = msg.content.getBytes(StandardCharsets.UTF_8);
            message = Message.createTextMessage(BigInteger.valueOf(msg.timestamp), nonce, content);
        }
        boolean isSendSuccess = daemon.sendMessage(friendPkBytes, message);
        logger.debug("NewMsgHash::{}, nonce::{}, isSendSuccess::{}", msg.hash, msg.nonce, isSendSuccess);
        if (isSendSuccess) {
            msg.content = null;
            msg.unsent = 1;
            chatRepo.updateChatMsg(msg);
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        logger.debug("Worker onStopped");
    }
}