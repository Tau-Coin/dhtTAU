package io.taucoin.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import io.taucoin.db.KeyValueDataBaseFactory;
import io.taucoin.db.MessageDB;
import io.taucoin.db.MessageDBImpl;
import io.taucoin.listener.MsgListener;
import io.taucoin.repository.AppRepository;
import io.taucoin.types.Message;
import io.taucoin.util.Repo;

public class CommunicationManager {
    private static final Logger logger = LoggerFactory.getLogger("CommunicationManager");

    private final Communication communication;

    private MsgListener listener;

    private final MessageDB messageDB;
    private final AppRepository appRepository;

    // message db path
    private static final String MSG_PATH = "msg";

    public CommunicationManager(byte[] deviceID, MsgListener listener, KeyValueDataBaseFactory dbFactory,
                                AppRepository appRepository) {
        this.listener = listener;
        this.appRepository = appRepository;
        this.messageDB = new MessageDBImpl(dbFactory.newDatabase());

        communication = new Communication(deviceID, this.messageDB, this.listener);
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

    /**
     * 中止主循环睡眠
     */
    public void interruptSleep() {
        this.communication.interruptSleep();
    }


    public MessageDB getMessageDB() {
        return messageDB;
    }

    /**
     * 向朋友发布新消息
     * @param friend 朋友公钥
     * @param message 新消息
     * @return true:接受该消息， false:拒绝该消息
     */
    public boolean publishNewMessage(byte[] friend, Message message) {
        return this.communication.publishNewMessage(friend, message);
    }

    /**
     * 当留在该朋友聊天页面时，只访问该朋友
     * @param peer 要访问的朋友
     */
    public void startVisitFriend(byte[] peer) {
        this.communication.startVisitFriend(peer);
    }

    /**
     * 当离开朋友聊天页面时，取消对朋友的单独访问
     */
    public void stopVisitFriend() {
        this.communication.stopVisitFriend();
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
     * 获取当前间隔时间
     * @return 间隔时间
     */
    public int getIntervalTime() {
        return this.communication.getIntervalTime();
    }

    /**
     * 设置间隔时间
     */
    public void setIntervalTime(int minIntervalTime) {
        this.communication.setIntervalTime(minIntervalTime);
    }

}
