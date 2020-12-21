package io.taucoin.torrent.publishing.core.model;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgType;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Friend;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.CommunityRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.FriendRepository;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.service.ThreadPoolFactory;
import io.taucoin.types.Message;
import io.taucoin.util.ByteUtil;

/**
 * MsgListener处理程序
 */
class MsgListenHandler {
    private static final Logger logger = LoggerFactory.getLogger("MsgListenHandler");
    private static final Logger msgLogger = LoggerFactory.getLogger("TAU messaging");
    private CompositeDisposable disposables = new CompositeDisposable();
    private ChatRepository chatRepo;
    private CommunityRepository communityRepo;
    private FriendRepository friendRepo;

    MsgListenHandler(Context appContext){
        chatRepo = RepositoryHelper.getChatRepository(appContext);
        communityRepo = RepositoryHelper.getCommunityRepository(appContext);
        friendRepo = RepositoryHelper.getFriendsRepository(appContext);
    }
    /**
     * 处理新的消息
     * 0、如果没和朋友建立Chat, 创建Chat
     * 1、更新朋友状态
     * 2、保存Chat的聊天信息
     * @param friendPk byte[] 朋友公钥
     * @param message Message
     */
    void onNewMessage(byte[] friendPk, Message message) {
        // 单个线程池处理，防止数据重复上报
        ThreadPoolFactory.getInstance().executeRequest(() -> {
            try {
                String hashStr = ByteUtil.toHexString(message.getHash());
                String friendPkStr = ByteUtil.toHexString(friendPk);
                String content = new String(message.getContent(), StandardCharsets.UTF_8);
                long sentTime = message.getTimestamp().longValue();
                long receivedTime = DateUtil.getTime();
                String sentTimeStr = DateUtil.formatTime(sentTime, DateUtil.pattern6);
                String receivedTimeStr = DateUtil.formatTime(receivedTime, DateUtil.pattern6);
                long delayTime = receivedTime - sentTime;
                int type = message.getType().ordinal();
                String userPk = MainApplication.getInstance().getPublicKey();
                ChatMsg chatMsg = chatRepo.queryChatMsg(userPk, hashStr);
                // 上报的Message有可能重复, 如果本地已存在不处理
                msgLogger.debug("onNewMessage friendPk::{}, hash::{}, SentTime::{}, ReceivedTime::{}," +
                                " DelayTime::{}s content::{}, exist::{}",
                        friendPkStr,
                        hashStr,
                        sentTimeStr,
                        receivedTimeStr,
                        delayTime,
                        content, null != chatMsg);
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
                        BigInteger currentCommTime = BigInteger.valueOf(sentTime);
                        BigInteger lastCommTime = BigInteger.valueOf(friend.lastCommTime);
                        if (currentCommTime.compareTo(lastCommTime) > 0) {
                            friend.lastCommTime = sentTime;
                        }
                        friendRepo.updateFriend(friend);
                    }
                    ChatMsg msg = new ChatMsg(hashStr, friendPkStr, userPk, content, type, sentTime);
                    chatRepo.addChatMsg(msg);
                }
            } catch (Exception e) {
                logger.error("onNewMessage error", e);
            }
        });
    }

    /**
     * 消息已被接收
     * @param friendPk byte[] 朋友公钥
     * @param root 消息root
     */
    void onReceivedMessageRoot(byte[] friendPk, byte[] root) {
        logger.debug("onReceivedMessageRoot friendPk::{}，MessageRoot::{}",
                ByteUtil.toHexString(friendPk), root);
        Disposable disposable = Flowable.create(emitter -> {
            try {
                String friendPkStr = ByteUtil.toHexString(friendPk);
                String hash = ByteUtil.toHexString(root);
                ChatMsg msg = chatRepo.queryChatMsg(friendPkStr, hash);
                if (msg != null) {
                    msg.status = ChatMsgType.RECEIVED.ordinal();
                    chatRepo.updateChatMsg(msg);
                }
            } catch (Exception e) {
                logger.error("onNewMessage error", e);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     * 销毁处理程序
     */
    void destroy() {
        disposables.clear();
    }

    /**
     * 发现朋友
     * @param friendPk 朋友公钥
     */
    void onDiscoveryFriend(byte[] friendPk) {
        logger.debug("onDiscoveryFriend friendPk::{}",
                ByteUtil.toHexString(friendPk));
        Disposable disposable = Flowable.create(emitter -> {
            try {
                String userPk = MainApplication.getInstance().getPublicKey();
                String friendPkStr = ByteUtil.toHexString(friendPk);
                Friend friend = friendRepo.queryFriend(userPk, friendPkStr);
                if (friend != null) {
                    friend.state = 2;
                    long currentTime = DateUtil.getTime();
                    // 当前时间大于上次更新时间30s再更新
                    if (currentTime - friend.lastSeenTime > 30) {
                        friend.lastSeenTime = DateUtil.getTime();
                    }
                    friendRepo.updateFriend(friend);
                }
            } catch (Exception e) {
                logger.error("onDiscoveryFriend error", e);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .subscribe();
        disposables.add(disposable);
    }
}
