package io.taucoin.torrent.publishing.service;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import io.taucoin.db.DBException;
import io.taucoin.torrent.publishing.core.model.Frequency;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgStatus;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgLog;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.types.Message;
import io.taucoin.util.ByteUtil;

/**
 * 消息朋友接收确认Worker
 */
public class ReceivedConfirmationWorker extends Worker {

    private static final Logger logger = LoggerFactory.getLogger("ReceivedConfirmationWorker");
    private static final int maxConfirmedQuantity = 5; // 最大已确认数量
    private TauDaemon daemon;
    private ChatRepository chatRepo;
    private int confirmedQuantity;
    public ReceivedConfirmationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        logger.debug("constructor");
        daemon = TauDaemon.getInstance(context);
        chatRepo = RepositoryHelper.getChatRepository(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        logger.debug("doWork");
        logger.debug("receivedConfirmation start");
        while (!isStopped()) {
            try {
                List<String> publicKeys = chatRepo.getUnConfirmationFriends();
                if (publicKeys != null && publicKeys.size() > 0) {
                    logger.debug("receivedConfirmation publicKeys.size::{}", publicKeys.size());
                    receivedConfirmation(publicKeys);
                    Thread.sleep(Frequency.FREQUENCY_RETRY.getFrequency());
                } else {
                    break;
                }
            } catch (Exception e) {
                logger.warn("receivedConfirmation error ::{}", e.getMessage());
                try {
                    Thread.sleep(Frequency.FREQUENCY_RETRY.getFrequency());
                    logger.debug("receivedConfirmation retry");
                } catch (InterruptedException ignore) {
                }
            }
        }
        return Result.success();
    }

    /**
     * 消息已接收确认
     */
    private void receivedConfirmation(@NonNull List<String> publicKeys) {
        for (String publicKey : publicKeys) {
            if (isStopped()) {
                break;
            }
            try {
                byte[] friendPk = ByteUtil.toByte(publicKey);
                byte[] friendConfirmationRoot = daemon.getFriendConfirmationRoot(friendPk);
                logger.debug("friendPk::{}, friendConfirmationRoot::{}",
                        publicKey,
                        ByteUtil.toHexString(friendConfirmationRoot));
                if (null == friendConfirmationRoot) {
                    continue;
                }
                confirmedQuantity = 0;
                updateReceivedConfirmationState(publicKey, friendConfirmationRoot);
            }catch (Exception e) {
                logger.warn("updateReceivedConfirmationState error:: {}", e.getMessage());
            }
        }
    }

    /**
     * 递归更新本地消息已接收状态
     */
    private void updateReceivedConfirmationState(String friendPk, byte[] msgRoot) throws DBException{
        String msgRootHash = ByteUtil.toHexString(msgRoot);
        ChatMsgLog msg = chatRepo.queryChatMsgLastLog(msgRootHash);
        if (null == msg) {
            return;
        }
        if (msg.status != ChatMsgStatus.RECEIVED_CONFIRMATION.getStatus()) {
            confirmedQuantity = 0;
            ChatMsgLog msgLog = new ChatMsgLog(msgRootHash,
                    ChatMsgStatus.RECEIVED_CONFIRMATION.getStatus(), DateUtil.getTime());
            logger.debug("updateReceivedConfirmationState friendPk::{}, msgRoot::{}",
                    friendPk, msgRootHash);
            chatRepo.addChatMsgLog(msgLog);
        } else {
            // 防止部分信息无法确认
            if (confirmedQuantity >= maxConfirmedQuantity) {
                return;
            }
            confirmedQuantity ++;
        }
        byte[] msgEncoded = daemon.getMsg(msgRoot);
        Message message = new Message(msgEncoded);
        byte[] previousMsgDAGRoot = message.getPreviousMsgDAGRoot();
        if (previousMsgDAGRoot != null) {
            updateReceivedConfirmationState(friendPk, previousMsgDAGRoot);
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        logger.debug("Worker onStopped");
    }
}