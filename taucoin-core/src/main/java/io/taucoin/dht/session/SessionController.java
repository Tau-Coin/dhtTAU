package io.taucoin.dht.session;

import io.taucoin.dht.metrics.Counter;
import io.taucoin.dht.util.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

/**
 * SessionController is the factory of session. It's responsible for creating
 * destroying all tau sessions.
 */
public class SessionController {

    private static final Logger logger = LoggerFactory.getLogger("SessionController");

    private static final int LISTEN_PORT = 6881;

    private static final long INTERVAL = 100; // milliseconds.

    public static final int MIN_SESSIONS = 0;
    public static final int MAX_SESSIONS = 64;

    // This list stores all the tau sessions.
    private List<TauSession> sessionsList = Collections.synchronizedList(
            new ArrayList<TauSession>());

    // Map from tau session to worker.
    private Map<TauSession, Worker> sessionToWorkerMap = Collections.synchronizedMap(
            new HashMap<TauSession, Worker>());

    // Producer input queue
    private BlockingQueue inputQueue;

    // counter for dht immutable and mutable item request
    private Counter counter;

    // Lock to ensure the operation atomicity
    // between 'sessionsList' and 'sessionToWokerMap'
    private final Object lock = new Object();

    /**
     * SessionController constructor.
     *
     * @param inputQueue producer input queue
     * @param counter metrics counter
     */
    public SessionController(BlockingQueue inputQueue, Counter counter) {
        this.inputQueue = inputQueue;
        this.counter = counter;
    }

    /**
     * Start all sessions and workers.
     *
     * @param quota sessions quota
     */
    public boolean start(int quota) {

        boolean ret = true;

        if (sessionsList.size() != 0 || sessionToWorkerMap.size() != 0) {
            logger.error("stop all sessions before starting");
            return false;
        }

        synchronized (lock) {
            // create tau sessions and workers.
            for (int i = 0; i < quota; i++) {
                SessionSettings.Builder builder = new SessionSettings.Builder();
                TauSession s = new TauSession(builder.build());
                Worker w = new Worker(i, s, inputQueue, counter);

                sessionToWorkerMap.put(s, w);
                sessionsList.add(s);
            }
        }

        // start all workers and sessions.
        Iterator<Map.Entry<TauSession, Worker>> it
                = sessionToWorkerMap.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<TauSession, Worker> entry = it.next();
            boolean ok = entry.getValue().start();
            if (!ok) {
                logger.error("starting worker failed");
                ret = false;
                stop();
                break;
            }

            try {
                Thread.sleep(INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Utils.printStacktraceToLogger(logger, e);
                ret = false;
                break;
            }
        }

        return ret;
    }

    /**
     * Stop all sessions and workers.
     */
    public void stop() {
        // stop all workers and sessions.
        Iterator<Map.Entry<TauSession, Worker>> it
                = sessionToWorkerMap.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<TauSession, Worker> entry = it.next();
            entry.getValue().stop();
        }

        synchronized (lock) {
            sessionToWorkerMap.clear();
            sessionsList.clear();
        }
    }

    /**
     * Restart sessions and workers.
     *
     * @param quota sessions quota
     */
    public boolean restart(int quota) {
        stop();
        return start(quota);
    }

    /**
     * Increase one session and one worker.
     */
    public boolean increase() {

        if (sessionsList.size() >= MAX_SESSIONS) {
            return false;
        }

        SessionSettings.Builder builder = new SessionSettings.Builder();
        TauSession s = new TauSession(builder.build());
        Worker w = new Worker(sessionsList.size(), s, inputQueue, counter);

        if (w.start()) {
            synchronized (lock) {
                sessionToWorkerMap.put(s, w);
                sessionsList.add(s);
            }
            return true;
        }

        return false;
    }

    /**
     * Decrease one session and one worker.
     */
    public boolean decrease() {

        if (sessionsList.size() <= MIN_SESSIONS) {
            return false;
        }

        // get the last session
        TauSession s = sessionsList.get(sessionsList.size() - 1);
        Worker w = sessionToWorkerMap.get(s);

        w.stop();
        synchronized (lock) {
            sessionsList.remove(sessionsList.size() - 1);
            sessionToWorkerMap.remove(s);
        }

        return true;
    }

    public int sessions() {
        return sessionsList.size();
    }

    private static final class NetworkInterfacePolicy {

        public static String networkInterfaces(int index) {
            int port = LISTEN_PORT + index;
            return "0.0.0.0:" + port;
        }
    }
}
