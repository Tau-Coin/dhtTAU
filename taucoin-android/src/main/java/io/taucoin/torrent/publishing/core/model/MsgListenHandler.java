package io.taucoin.torrent.publishing.core.model;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

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
import io.taucoin.torrent.publishing.core.utils.MsgSplitUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.types.Message;
import io.taucoin.util.ByteUtil;

/**
 * MsgListener处理程序
 */
class MsgListenHandler extends MsgListener{
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
    @Override
    public void onNewMessage(byte[] friendPk, Message message) {
        Disposable disposable = Flowable.create(emitter -> {
            try {
                // 朋友默认为发送者
                String senderPk = ByteUtil.toHexString(message.getSender());
                String receiverPk = ByteUtil.toHexString(message.getReceiver());
                String hash = ByteUtil.toHexString(message.getHash());
                String logicMsgHash = ByteUtil.toHexString(message.getLogicMsgHash());

                long sentTime = message.getTimestamp().longValue();
                long receivedTime = DateUtil.getTime();

                ChatMsg chatMsg = chatRepo.queryChatMsg(senderPk, hash);
                logger.debug("TAU messaging onNewMessage senderPk::{}, receiverPk::{}, hash::{}, " +
                                "SentTime::{}, ReceivedTime::{}, DelayTime::{}s, exist::{}",
                        senderPk, receiverPk, hash,
                        DateUtil.formatTime(sentTime, DateUtil.pattern6),
                        DateUtil.formatTime(receivedTime, DateUtil.pattern6),
                        receivedTime - sentTime, chatMsg != null);
                // 上报的Message有可能重复, 如果本地已存在不处理
                if (null == chatMsg) {
                    // 处理ChatName，如果为空，取显朋友显示名
                    String communityName = UsersUtil.getDefaultName(senderPk);
                    Community community = communityRepo.getChatByFriendPk(senderPk);
                    if (null == community) {
                        community = new Community(senderPk, communityName);
                        community.type = 1;
                        community.publicKey = receiverPk;
                        communityRepo.addCommunity(community);
                    }

                    User user = userRepo.getCurrentUser();
                    // 原始数据解密
                    byte[] cryptoKey;
                    // 判断消息的发送者是否是自己
                    if (StringUtil.isEquals(senderPk, user.publicKey)) {
                        cryptoKey = Utils.keyExchange(receiverPk, user.seed);
                    } else {
                        cryptoKey = Utils.keyExchange(senderPk, user.seed);
                    }
                    message.decrypt(cryptoKey);

                    String content = MsgSplitUtil.textBytesToString(message.getRawContent());
                    ChatMsg msg = new ChatMsg(hash, senderPk, receiverPk, content, message.getType().ordinal(),
                            sentTime, message.getNonce().longValue(), logicMsgHash);
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
    @Override
    public void onReadMessageRoot(byte[] friendPk, byte[] root, BigInteger timestamp) {
        Disposable disposable = Flowable.create(emitter -> {
            try {
                String hash = ByteUtil.toHexString(root);
                String friendPkStr = ByteUtil.toHexString(friendPk);
                logger.trace("onReadMessageRoot friendPk::{}，MessageRoot::{}",
                        friendPkStr, hash);
                String userPk = MainApplication.getInstance().getPublicKey();
                ChatMsgLog msgLog = chatRepo.queryChatMsgLog(hash, userPk, friendPkStr,
                        ChatMsgStatus.RECEIVED_CONFIRMATION.getStatus());
                logger.trace("onReadMessageRoot friendPk::{}, msgRoot::{}, exist::{}",
                        friendPkStr, hash, msgLog != null);
                if (null == msgLog) {
                    msgLog = new ChatMsgLog(hash, userPk, friendPkStr,
                            ChatMsgStatus.RECEIVED_CONFIRMATION.getStatus(), timestamp.longValue());
                   chatRepo.addChatMsgLog(msgLog);
                }
            } catch (SQLiteConstraintException ignore) {
            } catch (Exception e) {
                logger.error("onReadMessageRoot error", e);
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
     * @param timestamp 和朋友通信时间
     */
    @Override
    public void onDiscoveryFriend(byte[] friendPk, BigInteger timestamp) {
        Disposable disposable = Flowable.create(emitter -> {
            logger.debug("onDiscoveryFriend friendPk::{}",
                    ByteUtil.toHexString(friendPk));
            try {
                String userPk = MainApplication.getInstance().getPublicKey();
                String friendPkStr = ByteUtil.toHexString(friendPk);
                Friend friend = friendRepo.queryFriend(userPk, friendPkStr);
                long lastCommTime = timestamp.longValue();
                if (friend != null) {
                    boolean isUpdate = false;
                    if (friend.state != 2) {
                        friend.state = 2;
                        isUpdate = true;
                    }
                    // 当前时间大于上次更新时间
                    if (lastCommTime > friend.lastCommTime) {
                        friend.lastCommTime = lastCommTime;
                        isUpdate = true;
                    }
                    if (isUpdate) {
                        friendRepo.updateFriend(friend);
                    }
                    logger.info("onDiscoveryFriend friendPk::{}, lastCommTime::{}",
                            ByteUtil.toHexString(friendPk),
                            DateUtil.formatTime(lastCommTime, DateUtil.pattern6));
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
    @Override
    public void onMessageStatus(byte[] friend, byte[] root, MsgStatus msgStatus) {
        Disposable disposable = Flowable.create(emitter -> {
            try {
                String hash = ByteUtil.toHexString(root);
                logger.trace("onMessageStatus root::{}, msgStatus::{}",
                        hash, msgStatus.name());
                if (msgStatus.ordinal() == MsgStatus.PUT_SUCCESS.ordinal()) {
                    logger.trace("onMessageStatus put success hash::{}",
                            hash);
                }
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
     * 新device ID通知
     * @param deviceID device id
     */
    @Override
    public void onNewDeviceID(byte[] deviceID) {
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
     * 发现的新朋友通知，多台设备同Key之间同步朋友
     * @param friendPk 发现的新朋友
     */
    @Override
    public void onNewFriendFromMultiDevice(byte[] friendPk) {
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

                    String communityName = UsersUtil.getDefaultName(friendPkStr);
                    Community community = communityRepo.getChatByFriendPk(friendPkStr);
                    if (null == community) {
                        community = new Community(friendPkStr, communityName);
                        community.type = 1;
                        community.publicKey = userPk;
                        communityRepo.addCommunity(community);
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
