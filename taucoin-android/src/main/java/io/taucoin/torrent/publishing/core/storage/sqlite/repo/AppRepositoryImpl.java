package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import io.taucoin.core.FriendInfo;
import io.taucoin.repository.AppRepository;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.FrequencyUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.types.Message;
import io.taucoin.types.MessageType;
import io.taucoin.util.ByteUtil;

/**
 * FriendRepository接口实现
 */
public class AppRepositoryImpl implements AppRepository {

    private Context appContext;
    private AppDatabase db;

    /**
     * FriendRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    public AppRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    /**
     * 获取自己所有朋友
     * @return 朋友集合
     */
    @Override
    public Set<byte[]> getAllFriends() {
        Set<byte[]> set = new HashSet<>();
        try {
            List<String> friendList = db.friendDao().queryAllFriends();
            for (String friend : friendList) {
                set.add(ByteUtil.toByte(friend));
            }
        } catch (Exception ignore) { }
        return set;
    }

    /**
     * 获取自己和朋友的最新消息列表
     * @param friendPk 朋友的公钥
     * @param num 查询的消息的条数
     * @return 消息列表
     */
    @Override
    public List<Message> getLatestMessageList(byte[] friendPk, int num) {
        List<Message> msgList = new ArrayList<>();
        try {
            String userPkStr = MainApplication.getInstance().getPublicKey();
            String friendPkStr = ByteUtil.toHexString(friendPk);
            List<ChatMsg> chatMsgList = db.chatDao().getMessageList(userPkStr, friendPkStr, 0, num);
            if (null == chatMsgList || chatMsgList.size() == 0) {
                return msgList;
            }
            for (int i = 0; i < chatMsgList.size(); i++) {
                ChatMsg msg = chatMsgList.get(i);
                BigInteger nonce = BigInteger.valueOf(msg.nonce);
                BigInteger timestamp = BigInteger.valueOf(msg.timestamp);
                byte[] senderPk = ByteUtil.toByte(msg.senderPk);
                byte[] receiverPk = ByteUtil.toByte(msg.receiverPk);
                byte[] logicMsgHash = ByteUtil.toByte(msg.logicMsgHash);
                byte[] content = msg.content;
                Message message;
                if (msg.contentType == MessageType.PICTURE.ordinal()) {
                    message = Message.createPictureMessage(timestamp, senderPk, receiverPk, logicMsgHash,
                            nonce, null);
                } else {
                    message = Message.createTextMessage(timestamp, senderPk, receiverPk, logicMsgHash,
                            nonce, null);
                }
                message.setEncryptedContent(content);
                msgList.add(message);
            }
        } catch (Exception ignore) { }
        return msgList;
    }

    /**
     * 获取和自己正在聊天的朋友
     * @return 朋友公钥
     */
    public byte[] getChattingFriend() {
        Context context = MainApplication.getInstance();
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(context);
        String friend = settingsRepo.getChattingFriend();
        if (StringUtil.isNotEmpty(friend)) {
            return ByteUtil.toByte(friend);
        }
        return null;
    }

    /**
     * 获取主循环的时间间隔
     * @return 时间间隔
     */
    public int getMainLoopInterval() {
        return FrequencyUtil.getMainLoopInterval();
    }

    /**
     * 获取朋友的昵称
     * 朋友的公钥
     * @return 昵称信息类
     */
    @Override
    public FriendInfo getFriendInfo(byte[] friendPk) {
        String friendPkStr = ByteUtil.toHexString(friendPk);
        User friend = db.userDao().getUserByPublicKey(friendPkStr);
        byte[] nickname = null;
        BigInteger timestamp = BigInteger.ZERO;
        if (friend != null && StringUtil.isNotEmpty(friend.nickname)) {
            nickname = Utils.textStringToBytes(friend.nickname);
            timestamp = BigInteger.valueOf(friend.updateTime);
        }
        return new FriendInfo(friendPk, nickname, timestamp);
    }

    /**
     * 获取活跃的朋友
     * @return 活跃的朋友列表
     */
    @Override
    public List<byte[]> getActiveFriends() {
        List<byte[]> activeFriends = new ArrayList<>();
        try {
            List<String> friends = db.friendDao().getActiveFriends();
            if (friends != null && friends.size() > 0) {
                for (String friend : friends) {
                    activeFriends.add(ByteUtil.toByte(friend));
                }
            }
        } catch (Exception ignore) { }
        return activeFriends;
    }
}
