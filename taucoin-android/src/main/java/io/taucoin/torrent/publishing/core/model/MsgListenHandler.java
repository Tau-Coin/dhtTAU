package io.taucoin.torrent.publishing.core.model;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.listener.MsgStatus;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgStatus;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgLog;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Device;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Friend;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.CommunityRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.DeviceRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.FriendRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.types.Message;
import io.taucoin.util.ByteUtil;

/**
 * MsgListener处理程序
 */
class MsgListenHandler {
    private static final Logger logger = LoggerFactory.getLogger("MsgListenHandler");
    private CompositeDisposable disposables = new CompositeDisposable();
    private ChatRepository chatRepo;
    private FriendRepository friendRepo;
    private CommunityRepository communityRepo;
    private DeviceRepository deviceRepo;
    private UserRepository userRepo;

    MsgListenHandler(Context appContext) {
        chatRepo = RepositoryHelper.getChatRepository(appContext);
        friendRepo = RepositoryHelper.getFriendsRepository(appContext);
        communityRepo = RepositoryHelper.getCommunityRepository(appContext);
        deviceRepo = RepositoryHelper.getDeviceRepository(appContext);
        userRepo = RepositoryHelper.getUserRepository(appContext);
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
        Disposable disposable = Flowable.create(emitter -> {
            try {
                String userPk = MainApplication.getInstance().getPublicKey();
                String hash = ByteUtil.toHexString(message.getHash());
                String previousHash = ByteUtil.toHexString(message.getPreviousHash());
                long sentTime = message.getTimestamp().longValue();
                long receivedTime = DateUtil.getTime();
                String sentTimeStr = DateUtil.formatTime(sentTime, DateUtil.pattern6);
                String receivedTimeStr = DateUtil.formatTime(receivedTime, DateUtil.pattern6);
                long delayTime = receivedTime - sentTime;
                logger.debug("TAU messaging onNewMessage friendPk::{}, hash::{}, SentTime::{}, ReceivedTime::{}," +
                                " DelayTime::{}s",
                        ByteUtil.toHexString(friendPk), hash,
                        sentTimeStr,
                        receivedTimeStr,
                        delayTime);

                String friendPkStr = ByteUtil.toHexString(friendPk);
                ChatMsg chatMsg = chatRepo.queryChatMsg(userPk, hash);
                logger.debug("onNewMessage friendPk::{}, hash::{},  timestamp::{}, exist::{}",
                        friendPkStr, hash, DateUtil.formatTime(sentTime, DateUtil.pattern6),
                        null != chatMsg);
                // 上报的Message有可能重复, 如果本地已存在不处理
                if (null == chatMsg) {
                    // 处理ChatName，如果为空，取显朋友显示名
                    String communityName = UsersUtil.getDefaultName(friendPkStr);
                    Community community = communityRepo.getChatByFriendPk(friendPkStr);
                    if (null == community) {
                        community = new Community(friendPkStr, communityName);
                        community.type = 1;
                        community.publicKey = userPk;
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
                    ChatMsg msg = new ChatMsg(hash, friendPkStr, userPk, message.getType().ordinal(),
                            sentTime, message.getNonce().longValue(), previousHash);
                    msg.unsent = 1;
                    chatRepo.addChatMsg(msg);
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
     * 消息已被接收
     * @param friendPk byte[] 朋友公钥
     * @param root 消息root
     */
    void onReceivedMessageRoot(byte[] friendPk, byte[] root) {
        String friendPkStr = ByteUtil.toHexString(friendPk);
        logger.trace("onReceivedMessageRoot friendPk::{}，MessageRoot::{}",
                friendPkStr, root);
        Disposable disposable = Flowable.create(emitter -> {
            try {
                String hash = ByteUtil.toHexString(root);
                String userPk = MainApplication.getInstance().getPublicKey();
                ChatMsgLog msgLog = new ChatMsgLog(hash, userPk, friendPkStr,
                        ChatMsgStatus.RECEIVED_CONFIRMATION.getStatus(), DateUtil.getMillisTime());
                long result = chatRepo.addChatMsgLog(msgLog);
                logger.trace("updateReceivedConfirmationState friendPk::{}, msgRoot::{}, result::{}",
                        friendPkStr, hash, result);
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
                    boolean isUpdate = false;
                    if (friend.state != 2) {
                        friend.state = 2;
                        isUpdate = true;
                        logger.info("onDiscoveryFriend successfully, friendPk::{}, timestamp::{}",
                                ByteUtil.toHexString(friendPk),
                                DateUtil.formatTime(DateUtil.getTime(), DateUtil.pattern6));
                    }
                    long currentTime = DateUtil.getTime();
                    // 当前时间大于上次更新时间30s再更新
                    if (currentTime > friend.lastSeenTime) {
                        friend.lastSeenTime = currentTime;
                        isUpdate = true;
                    }
                    if (isUpdate) {
                        friendRepo.updateFriend(friend);
                    }
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

    /**
     * 上报的消息状态变化
     * @param friend
     * @param root
     * @param msgStatus
     */
    void onMessageStatus(byte[] friend, byte[] root, MsgStatus msgStatus) {
        Disposable disposable = Flowable.create(emitter -> {
            try {
                String hash = ByteUtil.toHexString(root);
                logger.trace("onMessageStatus root::{}, msgStatus::{}",
                        hash, msgStatus.name());
                int status = msgStatus.ordinal();
                String userPk = MainApplication.getInstance().getPublicKey();
                String friendPk = ByteUtil.toHexString(friend);
                ChatMsgLog msgLog = new ChatMsgLog(hash, userPk, friendPk, status,
                        DateUtil.getMillisTime());
                chatRepo.addChatMsgLog(msgLog);
            } catch (Exception e) {
                logger.error("onMessageStatus error", e);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     * 清除消息处理，防止多Seed数据错乱
     */
    void onCleared() {
        if (disposables != null) {
            disposables.clear();
        }
    }

    /**
     * 新device ID通知
     * @param deviceID device id
     */
    void onNewDeviceID(byte[] deviceID) {
        Disposable disposable = Flowable.create(emitter -> {
            try {
                String userPk = MainApplication.getInstance().getPublicKey();
                String deviceIDStr = new String(deviceID);
                logger.debug("onNewDeviceID userPk::{}, deviceID::{}",
                        userPk, deviceIDStr);
                Device device = new Device(userPk, deviceIDStr, DateUtil.getTime());
                deviceRepo.addDevice(device);
            } catch (Exception e) {
                logger.error("onNewDeviceID error", e);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     * 发现的新朋友通知
     * @param friendPk 发现的新朋友
     */
    void onNewFriend(byte[] friendPk) {
        Disposable disposable = Flowable.create(emitter -> {
            try {
                String userPk = MainApplication.getInstance().getPublicKey();
                String friendPkStr = ByteUtil.toHexString(friendPk);
                logger.debug("onNewFriend userPk::{}, friendPk::{}",
                        userPk, friendPkStr);
                User user = userRepo.getUserByPublicKey(friendPkStr);
                if (null == user) {
                    user = new User(friendPkStr);
                    userRepo.addUser(user);
                }
                if (StringUtil.isNotEquals(userPk, friendPkStr)) {
                    Friend friend = friendRepo.queryFriend(userPk, friendPkStr);
                    if (friend != null) {
                        if (friend.state == 0) {
                            friend.state = 1;
                            friendRepo.updateFriend(friend);
                        }
                    } else {
                        friend = new Friend(userPk, friendPkStr, 1);
                        friendRepo.addFriend(friend);
                    }
                }
            } catch (Exception e) {
                logger.error("onNewFriend error", e);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .subscribe();
        disposables.add(disposable);
    }
}
