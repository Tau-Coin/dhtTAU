package io.taucoin.Communication;

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

    private Communication communication;

    private MsgListener listener;

    private MessageDB messageDB;

    // message db path
    private static final String MSG_PATH = "msg";

    public CommunicationManager(MsgListener listener, KeyValueDataBaseFactory dbFactory) {
        this.listener = listener;
        this.messageDB = new MessageDBImpl(dbFactory.newDatabase());

        communication = new Communication(this.messageDB, this.listener);
    }

    public void openChainDB() throws Exception {
        try {
            this.messageDB.open(Repo.getRepoPath() + File.separator + MSG_PATH);
        } catch (Exception e) {
            throw e;
        }
    }

    public void start() {

        // Open the db for repo and block
        try {
            openChainDB();
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
     */
    public void publishNewMessage(byte[] friend, Message message, List<byte[]> data) {
        this.communication.publishNewMessage(friend, message, data);
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

}
