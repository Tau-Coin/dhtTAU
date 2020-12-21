package io.taucoin.torrent.publishing.service;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Friend;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.CommunityRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.FriendRepository;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.util.ByteUtil;

/**
 * Chat消息上报监听处理Worker
 * 仅上报触发
 */
public class MsgListenHandlerWorker extends Worker {

    private static final Logger logger = LoggerFactory.getLogger("MsgListenHandlerWorker");
    private ChatRepository chatRepo;
    private CommunityRepository communityRepo;
    private FriendRepository friendRepo;
    public MsgListenHandlerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        logger.debug("constructor");
        chatRepo = RepositoryHelper.getChatRepository(context);
        communityRepo = RepositoryHelper.getCommunityRepository(context);
        friendRepo = RepositoryHelper.getFriendsRepository(context);
        logger.debug("constructor isStopped::{}", isStopped());
    }

    @NonNull
    @Override
    public Result doWork() {
        logger.debug("doWork");
        try {
            Data data = getInputData();
            byte[] friendPk = data.getByteArray("friendPk");
            byte[] hash = data.getByteArray("hash");
            long timestamp = data.getLong("timestamp", 0);
            byte[] content = data.getByteArray("content");
            int type = data.getInt("type", 0);
            String friendPkStr = ByteUtil.toHexString(friendPk);
            String hashStr = ByteUtil.toHexString(hash);
            String userPk = MainApplication.getInstance().getPublicKey();
            logger.debug("onNewMessage friendPk::{}, hash::{}, timestamp::{}", friendPkStr, hashStr,
                    DateUtil.formatTime(timestamp, DateUtil.pattern6));
            ChatMsg chatMsg = chatRepo.queryChatMsg(userPk, hashStr);
            // 上报的Message有可能重复, 如果本地已存在不处理
            if (null == chatMsg) {
                // 处理ChatName，如果为空，取显朋友显示名
                String communityName = UsersUtil.getDefaultName(friendPkStr);
                Community community = communityRepo.getChatByFriendPk(friendPkStr);
                if (null == community) {
                    community = new Community(friendPkStr, communityName);
                    community.type = 1;
                    communityRepo.addCommunity(community);
                }
                // 更新朋友信息
                Friend friend = friendRepo.queryFriend(userPk, friendPkStr);
                if (friend != null) {
                    friend.state = 2;
                    friend.lastSeenTime = DateUtil.getTime();
                    BigInteger currentCommTime = BigInteger.valueOf(timestamp);
                    BigInteger lastCommTime = BigInteger.valueOf(friend.lastCommTime);
                    if (currentCommTime.compareTo(lastCommTime) > 0) {
                        friend.lastCommTime = timestamp;
                    }
                    friendRepo.updateFriend(friend);
                }
                String contentStr = new String(content);
                ChatMsg msg = new ChatMsg(hashStr, friendPkStr, userPk, contentStr, type, timestamp);
                chatRepo.addChatMsg(msg);
            }
        } catch (Exception e) {
            logger.error("onNewMessage error", e);
        }
        return Result.success();
    }

    @Override
    public void onStopped() {
        super.onStopped();
        logger.debug("Worker onStopped");
    }
}