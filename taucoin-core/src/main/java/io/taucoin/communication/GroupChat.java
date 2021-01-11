package io.taucoin.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.taucoin.db.MessageDB;
import io.taucoin.listener.MsgListener;

public class GroupChat {
    private static final Logger logger = LoggerFactory.getLogger("GroupChat");

    private final MsgListener msgListener;

    // message db
    private final MessageDB messageDB;

    // Communication thread.
    private Thread groupChatThread;

    public GroupChat(MessageDB messageDB, MsgListener msgListener) {
        this.messageDB = messageDB;
        this.msgListener = msgListener;
    }

    private boolean init() {
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

//        groupChatThread = new Thread(this::mainLoop);
//        groupChatThread.start();

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
