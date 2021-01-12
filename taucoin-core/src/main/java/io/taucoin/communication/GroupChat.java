package io.taucoin.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.taucoin.chain.Salt;
import io.taucoin.db.DBException;
import io.taucoin.db.MessageDB;
import io.taucoin.listener.MsgListener;
import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteArrayWrapper;

public class GroupChat {
    private static final Logger logger = LoggerFactory.getLogger("GroupChat");

    private final MsgListener msgListener;

    // message db
    private final MessageDB messageDB;

    // Communication thread.
    private Thread groupChatThread;

    // 主循环间隔最小时间
    private final int MIN_LOOP_INTERVAL_TIME = 50; // 50 ms

    // 主循环间隔时间
    private int loopIntervalTime = MIN_LOOP_INTERVAL_TIME;

    // 当前follow的group id集合
    private final Set<ByteArrayWrapper> groupIDs = Collections.synchronizedSet(new HashSet<>());

    // gossip channel salt
    private final Map<ByteArrayWrapper, byte[]> gossipSalts = Collections.synchronizedMap(new HashMap<>());

    public GroupChat(MessageDB messageDB, MsgListener msgListener) {
        this.messageDB = messageDB;
        this.msgListener = msgListener;
    }

    private boolean init() {
        return true;
    }

    /**
     * 主循环
     */
    private void mainLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {

                try {
                    Thread.sleep(this.loopIntervalTime);
                } catch (InterruptedException e) {
                    logger.info(e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
            } /*catch (DBException e) {
                this.msgListener.onMsgError("Data Base Exception!");
                logger.error(e.getMessage(), e);

                try {
                    Thread.sleep(this.MIN_LOOP_INTERVAL_TIME);
                } catch (InterruptedException ex) {
                    logger.info(ex.getMessage(), ex);
                    Thread.currentThread().interrupt();
                }
            }*/ catch (Exception e) {
                logger.error(e.getMessage(), e);

                try {
                    Thread.sleep(this.MIN_LOOP_INTERVAL_TIME);
                } catch (InterruptedException ex) {
                    logger.info(ex.getMessage(), ex);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * make gossip salt
     * @param groupID group ID
     * @return gossip salt
     */
    public static byte[] makeGossipSalt(byte[] groupID) {
        byte[] salt = new byte[ChainParam.GROUP_CHAT_GOSSIP_CHANNEL_PREFIX.length + groupID.length];

        System.arraycopy(ChainParam.GROUP_CHAT_GOSSIP_CHANNEL_PREFIX, 0, salt, 0,
                ChainParam.GROUP_CHAT_GOSSIP_CHANNEL_PREFIX.length);
        System.arraycopy(groupID, 0, salt, ChainParam.GROUP_CHAT_GOSSIP_CHANNEL_PREFIX.length, groupID.length);

        return salt;
    }

    /**
     * follow a new group
     * @param groupID group ID
     * @return true if succeed, false otherwise
     */
    public boolean followGroup(byte[] groupID, List<byte[]> peerList) throws DBException {
        return true;
    }

    /**
     * start chatting group
     * @param groupID group ID
     * @return true if succeed, false otherwise
     */
    public boolean startGroup(byte[] groupID) throws DBException {
        ByteArrayWrapper wGroupID = new ByteArrayWrapper(groupID);

        if (this.groupIDs.contains(wGroupID)) {
            logger.info("Chain:{} is followed.", wGroupID.toString());
            return true;
        }

        this.gossipSalts.put(wGroupID, makeGossipSalt(groupID));

        return true;
    }

    /**
     * Start thread
     *
     * @return boolean successful or not.
     */
    public boolean start() {

        if (!init()) {
            return false;
        }

        groupChatThread = new Thread(this::mainLoop);
        groupChatThread.start();

        return true;
    }

    /**
     * Stop thread
     */
    public void stop() {
        if (null != groupChatThread) {
            groupChatThread.interrupt();
        }
    }
}
