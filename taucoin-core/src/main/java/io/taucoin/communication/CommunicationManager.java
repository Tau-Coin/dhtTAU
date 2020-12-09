package io.taucoin.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import io.taucoin.db.KeyValueDataBaseFactory;
import io.taucoin.db.MessageDB;
import io.taucoin.db.MessageDBImpl;
import io.taucoin.listener.MsgListener;
import io.taucoin.types.Message;
import io.taucoin.util.Repo;

public class CommunicationManager {
    private static final Logger logger = LoggerFactory.getLogger("CommunicationManager");

    private final Communication communication;

    private MsgListener listener;

    private final MessageDB messageDB;

    // message db path
    private static final String MSG_PATH = "msg";

    public CommunicationManager(MsgListener listener, KeyValueDataBaseFactory dbFactory) {
        this.listener = listener;
        this.messageDB = new MessageDBImpl(dbFactory.newDatabase());

        communication = new Communication(this.messageDB, this.listener);
    }

    public void openMessageDB() throws Exception {
        try {
            this.messageDB.open(Repo.getRepoPath() + File.separator + MSG_PATH);
        } catch (Exception e) {
            throw e;
        }
    }

    public void start() {

        // Open the db for repo and block
        try {
            openMessageDB();
            communication.start();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void stop() {
        communication.stop();

        this.messageDB.close();
    }


    public MessageDB getMessageDB() {
        return messageDB;
    }

    /**
     * 向朋友发布新消息
     * @param friend 朋友公钥
     * @param message 新消息
     * @param data 新消息的其它相关数据，比如可能有多级文字或者图片结构，这些数据会一起发布到dht
     * @return true:接受该消息， false:拒绝该消息
     */
    public boolean publishNewMessage(byte[] friend, Message message, List<byte[]> data) {
        return this.communication.publishNewMessage(friend, message, data);
    }

    /**
     * 请求message相关的数据，包括message/message content/各级图片/文本等，拿到数据后会放到message db
     * @param hash data hash
     */
    public void requestMessageData(byte[] hash) {
        try {
            this.communication.requestImmutableData(hash);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 添加新朋友
     * @param pubKey public key
     */
    public void addNewFriend(byte[] pubKey) {
        try {
            this.communication.addNewFriend(pubKey);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 删除朋友
     * @param pubKey public key
     */
    public void delFriend(byte[] pubKey) {
        try {
            this.communication.delFriend(pubKey);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 获取所有的朋友
     * @return friend list
     */
    public List<byte[]> getAllFriends() {
        return this.communication.getAllFriends();
    }

    /**
     * 获取队列容量
     * @return 容量
     */
    public int getQueueCapability() {
        return this.communication.getQueueCapability();
    }

    /**
     * 获取队列当前大小
     * @return 队列大小
     */
    public int getQueueSize() {
        return this.communication.getQueueSize();
    }

    /**
     * get my latest msg root
     * @param pubKey public key
     * @return root
     */
    public byte[] getMyLatestMsgRoot(byte[] pubKey) {
        return this.communication.getMyLatestMsgRoot(pubKey);
    }

    /**
     * 获取朋友最新的root
     * @param pubKey public key
     * @return root
     */
    public byte[] getFriendLatestRoot(byte[] pubKey) {
        return this.communication.getFriendLatestRoot(pubKey);
    }

}
