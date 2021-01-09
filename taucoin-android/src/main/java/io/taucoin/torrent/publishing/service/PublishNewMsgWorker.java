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
        try {
            byte[] friendPkBytes = ByteUtil.toByte(msg.friendPk);
            byte[] content;
            byte[] previousMsgDAGRoot = daemon.getMyLatestMsgRoot(friendPkBytes);
            byte[] friendLatestMessageRoot = daemon.getFriendLatestRoot(friendPkBytes);
            byte[] skipMessageRoot = null;
            logger.debug("previousMsgDAGRoot::{}", ByteUtil.toHexString(previousMsgDAGRoot));
            // 如果当前message的nonce为0，最新的msgRoot就是message的skipMessageRoot
            // 否则最新msgRoot对应的skipMessageRoot，就是message的skipMessageRoot
            if (msg.nonce == 0) {
                skipMessageRoot = daemon.getMyLatestMsgRoot(friendPkBytes);
            } else {
                byte[] latestMsgRoot = daemon.getMyLatestMsgRoot(friendPkBytes);
                if (latestMsgRoot != null) {
                    byte[] latestMsgEncoded =  daemon.getMsg(latestMsgRoot);
                    if (latestMsgEncoded != null) {
                        Message message = new Message(latestMsgEncoded);
                        skipMessageRoot = message.getSkipMessageRoot();
                    }
                }
            }
            BigInteger nonce = BigInteger.valueOf(msg.nonce);
            Message message;
            List<byte[]> data = new ArrayList<>();
            if (msg.contextType == MessageType.PICTURE.ordinal()) {
//                    message = Message.createPictureMessage(timestamp,
//                            previousMsgDAGRoot, friendLatestMessageRoot, contentLink);
//                    parseMsgPicData(contentLink, data);
                return;
            } else {
                content = msg.context.getBytes(StandardCharsets.UTF_8);
                message = Message.CreateTextMessage(BigInteger.valueOf(msg.timestamp), nonce,
                        previousMsgDAGRoot, friendLatestMessageRoot, skipMessageRoot, content);
            }
            boolean isSendSuccess = daemon.sendMessage(friendPkBytes, message, data);
            String hash = ByteUtil.toHexString(message.getHash());
            logger.debug("NewMsgDAGRoot::{}, data.size::{}, isSendSuccess::{}", hash,
                    data.size(), isSendSuccess);
            if (isSendSuccess) {
                msg.hash = hash;
                msg.nonce = nonce.longValue();
                chatRepo.updateChatMsg(msg);
                WorkerManager.startReceivedConfirmationWorker();
            } else {
                // 发送消息，进入Communication Queue失败，重试发送
                Thread.sleep(Frequency.FREQUENCY_RETRY.getFrequency());
                publishMessage(msg);
            }
        } catch (InterruptedException ignore) {

        } catch (Exception e) {
            logger.warn("publishMessage error:: {}", e.getMessage());
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        logger.debug("Worker onStopped");
    }
}