package io.taucoin.dht;

import io.taucoin.dht.metrics.Counter;
import io.taucoin.dht.session.SessionController;
import io.taucoin.dht.session.SessionInfo;
import io.taucoin.listener.TauListener;

import com.hybhub.util.concurrent.ConcurrentSetBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.List;

import static io.taucoin.dht.DHT.*;
import static io.taucoin.dht.DHTReqResult.*;

/**
 * DHTEngine is the bridge between Tau blockchain and torrent dht network.
 * SessionController is responsible for starting and stopping sessions.
 */
public class DHTEngine {

    private static final Logger logger = LoggerFactory.getLogger("DHTEngine");

    // Dht blocking queue limit.
    public static final int DHTQueueCapability = 10000;

    private static volatile DHTEngine INSTANCE;

    // counter for dht immutable and mutable item request
    private Counter counter;

    // Session controller which manages multi sessions.
    private SessionController sessionController;

    private TauListener tauListener;

    // Queue of dht item requests(get and put immutable or mutable item).
    private BlockingQueue<Object> requestQueue
            = new ConcurrentSetBlockingQueue<>();

    /**
     * Get DHTEngine instance.
     *
     * @return DHTEngine instance.
     */
    public static DHTEngine getInstance() {
        if (INSTANCE == null) {
            synchronized (DHTEngine.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DHTEngine();
                }
            }
        }

        return INSTANCE;
    }

    // DHTEngine constructor
    private DHTEngine() {
        this.counter = new Counter();
        this.sessionController = new SessionController(requestQueue, counter);
    }

    /**
     * Set TauListener.
     *
     * @param tauListener TauListener
     */
    public void setTauListener(TauListener tauListener) {
        this.tauListener = tauListener;
    }

    /**
     * Start dht engine.
     *
     * @param quota sessions quota
     */
    public boolean start(int quota) {
        boolean ok = sessionController.start(quota);

        if (ok) {
            logger.info("dht sessions start successfully");
            tauListener.onDHTStarted(true, "");
        } else {
            logger.error("dht sessions start failed");
            tauListener.onDHTStarted(false, "listen failed");
        }

        return ok;
    }

    /**
     * Stop dht engine.
     */
    public void stop() {
        requestQueue.clear();
        sessionController.stop();
        tauListener.onDHTStopped();
    }

    /**
     * Restart dht engine.
     *
     * @param quota sessions quota
     */
    public boolean restart(int quota) {
        return sessionController.restart(quota);
    }

    /**
     * Increase dht session.
     *
     * @return boolean true indicates starting successfully, or else false.
     */
    public boolean increaseSession() {
        return sessionController.increase();
    }

    /**
     * Decrease dht session.
     *
     * @return boolean true indicates decreasing successfully, or else false.
     */
    public boolean decreaseSession() {
        return sessionController.decrease();
    }

    public List<SessionInfo> getSessionInfos() {
        return sessionController.getSessionInfos();
    }

    /**
     * Put immutable item asynchronously.
     *
     * @param item immutable item.
     * @return boolean true indicates this item is put into queue,
     *     or else false.
     */
    public DHTReqResult distribute(ImmutableItem item) {

        if (item == null || requestQueue.size() >= DHTQueueCapability) {
            logger.warn("drop immutable item" + item);
            return Dropped;
        }

        // Drop this item if it exists.
        if (requestQueue.contains(item)) {
            logger.trace("duplicate immutable item" + item);
            return Duplicated;
        }

        try {
            requestQueue.put(item);
            logger.trace("immutable item is queued(size:" + requestQueue.size()
                     + "):" + item);
        } catch (InterruptedException e) {
            e.printStackTrace();
            // This exception should never happens.
            // Because queue capability isn't set and
            // never block current thread.
        }

        return Success;
    }

    /**
     * Put mutable item asynchronously.
     *
     * @param item mutable item.
     * @return boolean true indicates this item is put into queue,
     *     or else false.
     */
    public DHTReqResult distribute(MutableItem item) {

        if (item == null || requestQueue.size() >= DHTQueueCapability) {
            logger.warn("drop mutable item" + item);
            return Dropped;
        }

        // Drop this item if it exists.
        if (requestQueue.contains(item)) {
            logger.trace("duplicate mutable item" + item);
            return Duplicated;
        }

        try {
            requestQueue.put(item);
            logger.trace("mutable item is queued(size:" + requestQueue.size()
                    + "):" + item);
        } catch (InterruptedException e) {
            e.printStackTrace();
            // This exception should never happens.
            // Because queue capability isn't set and
            // never block current thread.
        }

        return Success;
    }

    /**
     * Request immutable item asynchronously.
     *
     * @param spec immutable item specification
     * @param cb callback interface
     * @param cbData callback data
     * @return boolean true indicates this item is put into queue,
     *     or else false
     */
    public DHTReqResult request(GetImmutableItemSpec spec, GetDHTItemCallback cb,
            Object cbData) {

        if (spec == null || requestQueue.size() >= DHTQueueCapability) {
            logger.warn("drop immutable item req:" + spec);
            return Dropped;
        }

        ImmutableItemRequest req = new ImmutableItemRequest(spec, cb, cbData);

        // Drop this request if it exists.
        if (requestQueue.contains(req)) {
            logger.trace("duplicate immutable item req:" + req);
            return Duplicated;
        }

        try {
            requestQueue.put(req);
            logger.trace("immutable item req is queued(size:" + requestQueue.size()
                    +  "):" + req);
        } catch (InterruptedException e) {
            e.printStackTrace();
            // This exception should never happens.
            // Because queue capability isn't set and
            // never block current thread.
        }

        return Success;
    }

    /**
     * Request mutable item asynchronously.
     *
     * @param spec mutable item specification
     * @param cb callback interface
     * @param cbData callback data
     * @return boolean true indicates this item is put into queue,
     *     or else false.
     */
    public DHTReqResult request(GetMutableItemSpec spec, GetDHTItemCallback cb,
            Object cbData) {

        if (spec == null || requestQueue.size() >= DHTQueueCapability) {
            logger.warn("drop mutable item req:" + spec);
            return Dropped;
        }

        MutableItemRequest req = new MutableItemRequest(spec, cb, cbData);

        // Drop this request if it exists.
        if (requestQueue.contains(req)) {
            logger.trace("duplicate mutable item req:" + req);
            return Duplicated;
        }

        try {
            requestQueue.put(req);
            logger.trace("mutable item req is queued(size:" + requestQueue.size()
                     + "):" + req);
        } catch (InterruptedException e) {
            e.printStackTrace();
            // This exception should never happens.
            // Because queue capability isn't set and
            // never block current thread.
        }

        return Success;
    }

    /**
     * Get occupation of dht middleware blocking queue.
     */
    public int queueOccupation() {
        return requestQueue.size();
    }
}
