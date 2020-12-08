package io.taucoin.dht.session;

import io.taucoin.dht.metrics.Counter;
import io.taucoin.dht.util.Utils;

import com.frostwire.jlibtorrent.Sha1Hash;
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

    public static final int DEFUALT_INTERFACES = 1;

    public static final int MIN_SESSIONS = 1;
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

    // Parameters regulator for dht middleware.
    private Regulator regulator;

    // Cache map from sha1 hash to putting immutable or mutable item request.
    private Map<Sha1Hash, Object> putCache;

    // Lock to ensure the operation atomicity
    // between 'sessionsList' and 'sessionToWokerMap'
    private final Object lock = new Object();

    static final class SessionQuota {

        private int sessionsQuota;
        private int interfacesQuota;

        public SessionQuota() {
            this.sessionsQuota = 1;
            this.interfacesQuota = DEFUALT_INTERFACES;
        }

        public void set(int sessionsQuota, int interfacesQuota) {
            this.sessionsQuota = sessionsQuota;
            this.interfacesQuota = interfacesQuota;
        }

        public int getSessionsQuota() {
            return sessionsQuota;
        }

        public int getInterfacesQuota() {
            return interfacesQuota;
        }
    }

    private SessionQuota sessionQuota;

    /**
     * SessionController constructor.
     *
     * @param inputQueue producer input queue
     * @param counter metrics counter
     */
    public SessionController(BlockingQueue inputQueue, Counter counter,
            Map<Sha1Hash, Object> putCache) {
        this.inputQueue = inputQueue;
        this.counter = counter;
        this.regulator = new Regulator();
        this.putCache = putCache;
        this.sessionQuota = new SessionQuota();
    }

    /**
     * Start all sessions and workers.
     *
     * @param sessionsQuota sessions quota
     * @param interfacesQuota interfaces quota
     */
    public boolean start(int sessionsQuota, int interfacesQuota) {

        boolean ret = true;

        if (sessionsList.size() != 0 || sessionToWorkerMap.size() != 0) {
            logger.error("stop all sessions before starting");
            return false;
        }

        sessionQuota.set(sessionsQuota, interfacesQuota);

        synchronized (lock) {
            // create tau sessions and workers.
            for (int i = 0; i < sessionsQuota; i++) {
                SessionSettings.Builder builder = new SessionSettings.Builder()
                        .setNetworkInterfaces(NetworkInterfacePolicy
                                .networkInterfaces(i, sessionQuota.getInterfacesQuota()));
                TauSession s = new TauSession(builder.build());
                Worker w = new Worker(i, s, inputQueue, counter, regulator, putCache);

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
     * @param sessionsQuota sessions quota
     * @param interfacesQuota interfaces quota
     */
    public boolean restart(int sessionsQuota, int interfacesQuota) {
        stop();
        return start(sessionsQuota, interfacesQuota);
    }

    /**
     * Increase one session and one worker.
     */
    public boolean increase() {

        if (sessionsList.size() >= MAX_SESSIONS) {
            return false;
        }

        SessionSettings.Builder builder = new SessionSettings.Builder()
                .setNetworkInterfaces(NetworkInterfacePolicy
                        .networkInterfaces(sessionsList.size(),
                                sessionQuota.getInterfacesQuota()));

        TauSession s = new TauSession(builder.build());
        Worker w = new Worker(sessionsList.size(), s, inputQueue, counter, regulator,
                putCache);

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

    /**
     * Set read only mode for all sessions.
     *
     * @param value
     */
    public void setReadOnly(boolean value) {
        for (TauSession s : sessionsList) {
            s.setReadOnly(value);
        }
    }

    public List<SessionInfo> getSessionInfos() {
        List<SessionInfo> ret = new ArrayList<>();

        for (int i = 0; i < sessionsList.size(); i++) {
            SessionInfo si = new SessionInfo(i, sessionsList.get(i).nids(),
                    sessionsList.get(i).dhtNodes());
            ret.add(si);
        }

        return ret;
    }

    /**
     * Regulate time interval for dht putting and getting operation.
     *
     * @param interval time interval(milliseconds)
     */
    public void regulateDHTOPInterval(long interval) {
        if (interval < Regulator.DEFAULT_DHTOPInterval) {
            interval = Regulator.DEFAULT_DHTOPInterval;
        }
        regulator.setDHTOPInterval(interval);
    }

    /**
     * Get time interval for dht putting and getting operation.
     *
     * @return time interval(milliseconds)
     */
    public long getDHTOPInterval() {
        return regulator.getDHTOPInterval();
    }

    public void increaseDHTOPInterval() {
        regulator.increase();
    }

    public void decreaseDHTOPInterval() {
        regulator.decrease();
    }

    private static final class NetworkInterfacePolicy {

        public static String networkInterfaces(int index, int interfacesQuota) {
            StringBuilder sb = new StringBuilder();

            int startPort = LISTEN_PORT + index * interfacesQuota;
            for (int i = 0; i < interfacesQuota; i++) {
                if (i != (interfacesQuota - 1)) {
                    sb.append("0.0.0.0:" + (startPort + i) + ",");
                } else {
                    sb.append("0.0.0.0:" + (startPort + i));
                }
            }

            return sb.toString();
        }
    }
}
