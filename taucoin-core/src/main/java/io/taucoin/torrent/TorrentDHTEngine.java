package io.taucoin.torrent;

import io.taucoin.listener.TauListener;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.*;
import com.frostwire.jlibtorrent.swig.entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static io.taucoin.torrent.DHT.*;

/**
 * TorrentDHTEngine is the bridge between tau blockchain and torrent SessionManager.
 * It is responsible for starting and stopping SessionManager.
 */
public class TorrentDHTEngine {

    private static final Logger logger = LoggerFactory.getLogger("DHT-Engine");

    private static volatile TorrentDHTEngine INSTANCE;

    // enable torrent log or not.
    private static final boolean EnableTorrentLog = false;

    // Torrent session manager.
    private SessionManager sessionManager;

    // Torrent session settings.
    private SessionSettings settings;

    private TauListener tauListener;

    private AlertListener torrentListener = new AlertListener() {

        @Override
        public int[] types() {
            return null;
        }

        @Override
        public void alert(Alert<?> alert) {
            AlertType type = alert.type();

            if (type == AlertType.LISTEN_SUCCEEDED) {
                ListenSucceededAlert a = (ListenSucceededAlert) alert;
                logger.info(a.message());
                TorrentDHTEngine.this.tauListener.onDHTStarted(true, "");

                // start session statistic after listening successfully.
                startStatsPoller();

                // start thread of getting dht items.
                startDHTItemWorkers();
            }

            if (type == AlertType.LISTEN_FAILED) {
                ListenFailedAlert a = (ListenFailedAlert) alert;
                logger.info(a.message());
                TorrentDHTEngine.this.tauListener.onDHTStarted(false, "listen failed");
            }

            if (type == AlertType.DHT_MUTABLE_ITEM) {
                DhtMutableItemAlert a = (DhtMutableItemAlert) alert;
                logger.info(a.message());
            }

            if (type == AlertType.DHT_IMMUTABLE_ITEM) {
                DhtImmutableItemAlert a = (DhtImmutableItemAlert) alert;
                logger.info(a.message());
            }

            if (type == AlertType.DHT_PUT) {
                DhtPutAlert a = (DhtPutAlert) alert;
                logger.info(a.message());
            }
        }
    };

    // blocks torrent daemon thread
    private final Object signal = new Object();
    private boolean stopped = false;

    private Runnable torrentSession = new Runnable() {
        @Override
        public void run() {
            sessionManager.start(settings.getSessionParams());

            synchronized (TorrentDHTEngine.this.signal) {
                while (!TorrentDHTEngine.this.stopped) {
                    try {
                        TorrentDHTEngine.this.signal.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        logger.error(e.getMessage());
                    }
                }
            }

            logger.info("torrent daemon is exiting...");
        }
    };

    private Thread torrentDaemon;

    // timer to poll session statistics.
    // Ture indicates this timer runs as a daemon thread.
    private Timer statsPoller = new Timer(true);

    private SessionStats stats = new SessionStats();
    private TimerTask statsTask = new TimerTask() {
        @Override
        public void run() {
            stats.update(sessionManager.dhtNodes(),
                    sessionManager.downloadRate(),
                    sessionManager.uploadRate(),
                    sessionManager.totalDownload(),
                    sessionManager.totalUpload());

            //logger.info("session stats:" + stats);
            TorrentDHTEngine.this.tauListener.onSessionStats(stats);
        }
    };

    private AtomicBoolean statsPollerStarted = new AtomicBoolean(false);

    private static final long START_STATS_DELAY = 2 * 1000;
    private static final long STATS_PERIOD = 10 * 1000;

    private static final String sUndefinedEntry = entry.data_type.undefined_t.toString();

    // The feature of the queue of getting mutable and immutable item.

    // The time interval for dht operation.
    // TODO:
    private static final long DHTOperationInterval = 1 * 1000; // milliseconds.

    // Dht blocking queue size limit.
    private static final int DHTBlockingQueueCapability = 1000;

    private static final int ImmutableRequestQueueWorkers = 2;

    private static final int MutableRequestQueueWorkers = 2;

    // Queue of getting immutable item request.
    private BlockingQueue<ImmutableItemRequest> gettingImmutableItemQueue
            = new LinkedBlockingQueue<>();
    private ExecutorService gettingImmutableItemThreadPool;

    // Queue of getting mutable item request.
    private BlockingQueue<MutableItemRequest> gettingMutableItemQueue
            = new LinkedBlockingQueue<>();
    private ExecutorService gettingMutableItemThreadPool;

    // Queue of putting immutable item request.
    private BlockingQueue<ImmutableItem> puttingImmutableItemQueue
            = new LinkedBlockingQueue<>();
    private Thread puttingImmutableItemThread;

    // Queue of putting mutable item request.
    private BlockingQueue<MutableItem> puttingMutableItemQueue
            = new LinkedBlockingQueue<>();
    private Thread puttingMutableItemThread;

    private AtomicBoolean dhtItemWorkersStarted = new AtomicBoolean(false);

    /**
     * Get TorrentDHTEngine instance.
     *
     * @return TorrentDHTEngine instance.
     */
    public static TorrentDHTEngine getInstance() {
        if (INSTANCE == null) {
            synchronized (TorrentDHTEngine.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TorrentDHTEngine();
                }
            }
        }

        return INSTANCE;
    }

    private TorrentDHTEngine() {
        sessionManager = new SessionManager(EnableTorrentLog);
        sessionManager.addListener(torrentListener);
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
     * Get SessionManager from torrect session context.
     *
     * @return SessionManager
     */
    public SessionManager getSessionManager() {
        return sessionManager;
    }

    /**
     * Start torrent dht engine.
     *
     * @param settings SessionSettings
     */
    public void start(SessionSettings settings) {
        if (sessionManager.isRunning()) {
            return;
        }

        logger.info("starting dht engine daemon");
        this.settings = settings;
        torrentDaemon = new Thread(torrentSession);
        torrentDaemon.setDaemon(true);
        torrentDaemon.start();
    }

    /**
     * Stop torrent dht engine.
     */
    public void stop() {
        if (!sessionManager.isRunning()) {
            return;
        }

        logger.info("stopping dht engine daemon");

        // First of all, stop dht item workers.
        stopDHTItemWorkers();

        // Then, session statistic should be canceled.
        stopStatsPoller();

        sessionManager.stop();

        synchronized (signal) {
            stopped = true;
            signal.notify();
        }

        torrentDaemon.interrupt();
        try {
            torrentDaemon.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        torrentDaemon = null;
        this.settings = null;

        this.tauListener.onDHTStopped();
    }

    /**
     * Put immutable item.
     *
     * @param item immutable item
     * @return Sha1Hash sha1 hash of the immutable item
     */
    public Sha1Hash dhtPut(ImmutableItem item) {

        if (!sessionManager.isRunning()) {
            return null;
        }

        logger.debug("put immutable item:" + item.entry.toString());
        return sessionManager.dhtPutItem(item.entry);
    }

    /**
     * Put mutable item.
     *
     * @param item mutable item
     */
    public void dhtPut(MutableItem item) {

        if (!sessionManager.isRunning()) {
            return;
        }

        logger.debug("put mutable item:" + item.entry.toString());
        sessionManager.dhtPutItem(item.publicKey, item.privateKey,
                item.entry, item.salt);
    }

    /**
     * Put immutable item asynchronously.
     *
     * @param item immutable item.
     * @return boolean true indicates this item is put into queue,
     *     or else false.
     */
    public boolean distribute(ImmutableItem item) {

        if (item == null || !sessionManager.isRunning()
                || puttingImmutableItemQueue.size() >= DHTBlockingQueueCapability) {
            return false;
        }

        if (puttingImmutableItemQueue.contains(item)) {
            logger.warn("drop immutable item" + item);
            return false;
        }

        puttingImmutableItemQueue.add(item);
        return true;
    }

    /**
     * Put mutable item asynchronously.
     *
     * @param item mutable item.
     * @return boolean true indicates this item is put into queue,
     *     or else false.
     */
    public boolean distribute(MutableItem item) {

        if (item == null || !sessionManager.isRunning()
                || puttingMutableItemQueue.size() >= DHTBlockingQueueCapability) {
            return false;
        }

        if (puttingMutableItemQueue.contains(item)) {
            logger.warn("drop mutable item" + item);
            return false;
        }

        puttingMutableItemQueue.add(item);
        return true;
    }

    /**
     * dht tau gets immutable item.
     * Before getting any item, it must put an immutable item.
     * This is the republish and reannouncement implementation
     * to ensure the data is alive.
     *
     * @param spec getting immutable item specification
     * @param item immutable item put
     * @return ExchangeImmutableItemResult exchange result
     */
    public ExchangeImmutableItemResult dhtTauGet(GetImmutableItemSpec spec,
            ImmutableItem item) {
        if (!sessionManager.isRunning()) {
            return null;
        }

        Sha1Hash hash = this.dhtPut(item);

        Entry entry = sessionManager.dhtGetItem(spec.sha1, spec.timeout);
        byte[] data = null;
        if (entry != null) {
            data = entry.bencode();
        }

        return new ExchangeImmutableItemResult(data, hash);
    }

    /**
     * dht tau gets mutable item.
     * Before getting any item, it must put a mutable item.
     *
     * @param spec getting mutable item specification
     * @param item mutable item put
     * @return ExchangeMutableItemResult exchange result
     */
    public ExchangeMutableItemResult dhtTauGet(GetMutableItemSpec sepc,
            MutableItem item) {
        return null;
    }

    /**
     * dht gets immutable item.
     *
     * @param spec getting immutable item specification
     * @return immutable data
     */
    public byte[] dhtGet(GetImmutableItemSpec spec) {

        if (!sessionManager.isRunning()) {
            return null;
        }

        Entry entry = sessionManager.dhtGetItem(spec.sha1, spec.timeout);
        byte[] data = null;
        if (entry != null) {
            logger.debug("immutable entry [" + spec.sha1 + "] got:" + entry.toString()
                    + ", type:" + getEntryType(entry));

            if (!isEntryUndefined(entry)) {
                data = entry.bencode();
            }
        }

        return data;
    }

    /**
     * dht tau gets mutable item.
     *
     * @param spec getting mutable item specification
     * @return mutable item
     */
    public byte[] dhtGet(GetMutableItemSpec spec) {

        if (!sessionManager.isRunning()) {
            return null;
        }

        com.frostwire.jlibtorrent.SessionManager.MutableItem result
                = sessionManager.dhtGetItem(spec.publicKey,
                        spec.salt, spec.timeout);

        if (result == null || result.item == null || isEntryUndefined(result.item)) {
            return null;
        }

        logger.debug("mutable item got:" + result.item.toString()
                + ", type:" + getEntryType(result.item));

        return result.item.bencode();
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
    public boolean request(GetImmutableItemSpec spec, GetDHTItemCallback cb,
            Object cbData) {

        if (spec == null || !sessionManager.isRunning()
                || gettingImmutableItemQueue.size() >= DHTBlockingQueueCapability) {
            return false;
        }

        ImmutableItemRequest req = new ImmutableItemRequest(spec, cb, cbData);

        if (gettingImmutableItemQueue.contains(req)) {
            logger.warn("drop immutable item req:" + req);
            return false;
        }

        gettingImmutableItemQueue.add(req);
        return true;
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
    public boolean request(GetMutableItemSpec spec, GetDHTItemCallback cb,
            Object cbData) {

        if (spec == null || !sessionManager.isRunning()
                || gettingMutableItemQueue.size() >= DHTBlockingQueueCapability) {
            return false;
        }

        MutableItemRequest req = new MutableItemRequest(spec, cb, cbData);

        if (gettingMutableItemQueue.contains(req)) {
            logger.warn("drop mutable item req:" + req);
            return false;
        }

        gettingMutableItemQueue.add(req);
        return true;
    }

    /**
     * takes a host name and port pair. That endpoint will be
     * pinged, and if a valid DHT reply is received, the node will be added to
     * the routing table.
     *
     * @param node
     */
    public void addDhtNode(Pair<String, Integer> node) {
        new SessionHandle(sessionManager.swig()).addDhtNode(node);
    }

    // start polling session statistics
    private void startStatsPoller() {
        if (statsPollerStarted.get()) {
            return;
        }
        statsPollerStarted.set(true);

        logger.debug("starting stats poller");
        statsPoller.schedule(statsTask, START_STATS_DELAY, STATS_PERIOD);
    }

    // stop polling session statistics
    private void stopStatsPoller() {
        if (!statsPollerStarted.get()) {
            return;
        }
        statsPollerStarted.set(false);
        logger.debug("stopping stats poller");
        statsPoller.cancel();
    }

    private void startDHTItemWorkers() {
        if (dhtItemWorkersStarted.get()) {
            return;
        }
        dhtItemWorkersStarted.set(true);

        puttingImmutableItemThread = new Thread(this::immutableItemsDistributeLoop,
                "puttingImmutableItemThread");
        puttingImmutableItemThread.start();

        puttingMutableItemThread = new Thread(this::mutableItemsDistributeLoop,
                "puttingMutableItemThread");
        puttingMutableItemThread.start();

        gettingImmutableItemThreadPool
                = Executors.newCachedThreadPool();
        for (int i = 0; i < ImmutableRequestQueueWorkers; i++) {
            gettingImmutableItemThreadPool.execute(new GettingImmutableItemTask());
        }

        gettingMutableItemThreadPool
                = Executors.newCachedThreadPool();
        for (int i = 0; i < MutableRequestQueueWorkers; i++) {
            gettingMutableItemThreadPool.execute(new GettingMutableItemTask());
        }

        logger.info("starting dht items workers");
    }

    private void stopDHTItemWorkers() {
        if (!dhtItemWorkersStarted.get()) {
            return;
        }
        dhtItemWorkersStarted.set(false);

        if (puttingImmutableItemThread != null) {
            puttingImmutableItemThread.interrupt();
        }
        if (puttingMutableItemThread != null) {
            puttingMutableItemThread.interrupt();
        }

        if (gettingImmutableItemThreadPool != null) {
            gettingImmutableItemThreadPool.shutdownNow();
        }
        if (gettingMutableItemThreadPool != null) {
            gettingMutableItemThreadPool.shutdownNow();
        }

        try {
            puttingImmutableItemThread.join();
            puttingImmutableItemThread = null;
            puttingMutableItemThread.join();
            puttingMutableItemThread = null;

            gettingImmutableItemThreadPool.awaitTermination(
                    1, TimeUnit.SECONDS);
            gettingImmutableItemThreadPool = null;
            gettingMutableItemThreadPool.awaitTermination(
                    1, TimeUnit.SECONDS);
            gettingMutableItemThreadPool = null;
        } catch (InterruptedException e) {
            // ignore this exception
        }

        logger.info("stopping dht items workers");
    }

    private class GettingImmutableItemTask implements Runnable {

        public GettingImmutableItemTask() {}

        @Override
        public void run() {
            immutableItemsRetriveLoop();
        }
    }

    private class GettingMutableItemTask implements Runnable {

        public GettingMutableItemTask() {}

        @Override
        public void run() {
            mutableItemsRetriveLoop();
        }
    }

    /**
     * Retrive immutable item.
     */
    private void immutableItemsRetriveLoop() {

        while (!Thread.currentThread().isInterrupted()) {

            ImmutableItemRequest req = null;
            byte[] item = null;

            try {
                Thread.sleep(DHTOperationInterval);
                req = gettingImmutableItemQueue.take();
                item = dhtGet(req.getSpec());
            } catch (InterruptedException e) {
                break;
            } catch (Throwable e) {
                e.printStackTrace();
            }

            if (req != null) {
                req.onDHTItemGot(item);
            }
        }
    }

    /**
     * Retrive mutable item.
     */
    private void mutableItemsRetriveLoop() {

        while (!Thread.currentThread().isInterrupted()) {

            MutableItemRequest req = null;
            byte[] item = null;

            try {
                Thread.sleep(DHTOperationInterval);
                req = gettingMutableItemQueue.take();
                item = dhtGet(req.getSpec());
            } catch (InterruptedException e) {
                break;
            } catch (Throwable e) {
                e.printStackTrace();
            }

            if (req != null) {
                req.onDHTItemGot(item);
            }
        }
    }

    /**
     * Distribute immutable item.
     */
    private void immutableItemsDistributeLoop() {

        while (!Thread.currentThread().isInterrupted()) {

            ImmutableItem item;
            try {
                Thread.sleep(DHTOperationInterval);
                item = puttingImmutableItemQueue.take();
                dhtPut(item);
            } catch (InterruptedException e) {
                break;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Distribute mutable item.
     */
    private void mutableItemsDistributeLoop() {

        while (!Thread.currentThread().isInterrupted()) {

            MutableItem item;
            try {
                Thread.sleep(DHTOperationInterval);
                item = puttingMutableItemQueue.take();
                dhtPut(item);
            } catch (InterruptedException e) {
                break;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private static String getEntryType(Entry e) {
        if (e == null || e.swig() == null) {
            return "";
        }

        entry eswig = e.swig();
        return eswig.type().toString();
    }

    private static boolean isEntryUndefined(Entry e) {
        return sUndefinedEntry.equals(getEntryType(e));
    }
}
