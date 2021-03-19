package io.taucoin.torrent.publishing.service;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import io.taucoin.torrent.publishing.core.model.Interval;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgStatus;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgLog;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.MsgSplitUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
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
    private UserRepository userRepo;
    public PublishNewMsgWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        logger.debug("constructor");
        daemon = TauDaemon.getInstance(context);
        chatRepo = RepositoryHelper.getChatRepository(context);
        userRepo = RepositoryHelper.getUserRepository(context);
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
    private void publishMessage(@NonNull ChatMsg msg) throws Exception {
        if (isStopped()) {
            return;
        }
        BigInteger nonce = BigInteger.valueOf(msg.nonce);
        BigInteger timestamp = BigInteger.valueOf(msg.timestamp);
        byte[] senderPk = ByteUtil.toByte(msg.senderPk);
        byte[] friendPk = ByteUtil.toByte(msg.friendPk);
        byte[] logicMsgHash = ByteUtil.toByte(msg.logicMsgHash);
        byte[] content = MsgSplitUtil.textStringToBytes(msg.content);
        Message message;
        if (msg.contentType == MessageType.PICTURE.ordinal()) {
            message = Message.createPictureMessage(timestamp, senderPk, friendPk, logicMsgHash,
                    nonce, content);
        } else {
            message = Message.createTextMessage(timestamp, senderPk, friendPk, logicMsgHash,
                    nonce, content);
        }
        User user = userRepo.getUserByPublicKey(msg.senderPk);
        byte[] key = Utils.keyExchange(msg.friendPk, user.seed);
        message.encrypt(key);
        String hash = ByteUtil.toHexString(message.getHash());
        boolean isSendSuccess = daemon.sendMessage(friendPk, message);
        logger.debug("newMsgHash::{}, friendPk::{} nonce::{}, logicMsgHash::{}, isSendSuccess::{}",
                hash, msg.friendPk, msg.nonce, msg.logicMsgHash, isSendSuccess);
        if (isSendSuccess) {
            msg.unsent = 1;
            chatRepo.updateMsgSendStatus(msg);
            ChatMsgLog log = new ChatMsgLog(msg.hash, msg.senderPk, msg.friendPk,
                    ChatMsgStatus.SENT.getStatus(), DateUtil.getMillisTime());
            chatRepo.addChatMsgLog(log);
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        logger.debug("Worker onStopped");
    }
}