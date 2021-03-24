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

    private final AppRepository appRepository;

    // message db path
    private static final String MSG_PATH = "msg";

    public CommunicationManager(byte[] deviceID, MsgListener listener, KeyValueDataBaseFactory dbFactory,
                                AppRepository appRepository) {
        this.listener = listener;
        this.appRepository = appRepository;

        communication = new Communication(deviceID, this.listener, appRepository);
    }

    public void start() {
        try {
            communication.start();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void stop() {
        communication.stop();
    }

}
