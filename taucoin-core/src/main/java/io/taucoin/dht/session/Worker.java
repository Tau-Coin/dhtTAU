package io.taucoin.dht.session;

import io.taucoin.dht.metrics.Counter;
import io.taucoin.dht.util.Utils;

import com.frostwire.jlibtorrent.alerts.DhtImmutableItemAlert;
import com.frostwire.jlibtorrent.alerts.DhtMutableItemAlert;
import com.frostwire.jlibtorrent.alerts.DhtPutAlert;
import com.frostwire.jlibtorrent.Entry;
import com.frostwire.jlibtorrent.Sha1Hash;
import com.frostwire.jlibtorrent.swig.dht_put_alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.concurrent.BlockingQueue;
import java.util.Map;

import static io.taucoin.dht.DHT.*;
import static io.taucoin.dht.session.TauSession.ItemGotPutListener;

/**
 * Worker gets and puts immutable and mutable item from and into dht network.
 */
class Worker {

    // Note: when multi-session is started in different thread,
    // application is crashed at 'aesni_ecb_encrypt'.
    // Often the AES algorithm can't be parallelised, because it is inherently
    // serial. So start multi mession serially.
    private static final Object StartLock = new Object();
    private static final long SessionStartInterval = 100; // milliseconds.

    // Improvement for getting and putting dht items.
    // Only when session's dht nodes is greater than 'DHTNODES_THRESOLD',
    // can dht item be allowed to put and get.
    private static final long DHTNODES_THRESOLD = 50;

    private static final boolean GET_ITEM_ASYNC = true;

    private static final int CACHE_SIZE_LIMIT = 200;

    // Instance index;
    private int index;

    private Logger logger;

    // TAU session which is responsible for getting and putting dht items.
    private TauSession session;

    // Producer queue
    private BlockingQueue<Object> inputQueue;

    // metrics counter for dht immutable and mutable item request
    private Counter counter;

    // Parameters regulator for dht middleware.
    private Regulator regulator;

    // Cache map from sha1 hash to putting immutable or mutable item request.
    private Map<Sha1Hash, Object> putCache;

    // Cache map from sha1 hash to getting immutable or mutable item specification.
    private Map<Sha1Hash, Object> getCache;

    // Components for waiting the result of starting TauSession.
    private final Object signal = new Object();
    private volatile boolean startingResult = false;
    private volatile boolean startingResultReceived = false;

    private volatile boolean stopRequested = false;

    private Runnable task = new Runnable() {

        @Override
        public void run() {
            boolean result = false;

            synchronized(StartLock) {
                result = session.start();

                // maybe wait for a while.
                try {
                    Thread.sleep(SessionStartInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Utils.printStacktraceToLogger(logger, e);
                }
            }

            session.addListener(listener);

            // notify starting result.
            synchronized(signal) {
                startingResultReceived = true;
                startingResult = result;
                signal.notify();
            }

            while (!Thread.currentThread().isInterrupted() && !stopRequested) {
                Object req = null;

                try {
                    Thread.sleep(regulator.getDHTOPInterval());

                    if (stopRequested) {
                        break;
                    }

                    if (session.dhtNodes() <= DHTNODES_THRESOLD
                            || getCache.size() >= CACHE_SIZE_LIMIT) {
                        logger.trace("session dht nodes is too less:" + session.dhtNodes());
                        continue;
                    }

                    // TODO: comments
                    req = inputQueue.take();
                    process(req);
                } catch (InterruptedException e) {
                    break;
                }  catch (Throwable e) {
                    e.printStackTrace();
                    Utils.printStacktraceToLogger(logger, e);
                }
            }
        }
    };

    private Thread worker = null;

    /**
     * Worker constructor.
     *
     * @param index session index
     * @param session TauSession
     * @param inputQueue producer queue
     * @param counter metrics counter
     */
    public Worker(int index, TauSession session,
            BlockingQueue<Object> inputQueue,
            Counter counter, Regulator regulator,
            Map<Sha1Hash, Object> putCache, Map<Sha1Hash, Object> getCache) {

        this.index = index;
        this.session = session;
        this.inputQueue = inputQueue;
        this.counter = counter;
        this.regulator = regulator;
        this.putCache = putCache;
        this.getCache = getCache;

        logger = LoggerFactory.getLogger("Session[" + index + "]");
        this.session.setLogger(logger);
    }

    /**
     * Start worker and tau session.
     */
    public boolean start() {

        if (worker != null) {
            return true;
        }

        worker = new Thread(task, "SessionWoker[" + index + "]");
        worker.start();

        // notify starting result.
        synchronized (signal) {
            while (!startingResultReceived) {
                try {
                    signal.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        return startingResult;
    }

    /**
     * Stop worker and tau session.
     */
    public void stop() {

        if (worker == null) {
            return;
        }

        stopRequested = true;
        // Firstly, interrupt worker thread.
        worker.interrupt();
        /*
        try {
            worker.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
         */
        worker = null;

        // And then stop session manager.
        session.stop();
    }

    private void process(Object req) {

        if (req == null) {
            return;
        }

        // dispatch dht request
        if (req instanceof ImmutableItemRequest) {
            if (GET_ITEM_ASYNC) {
                requestImmutableItemAsync((ImmutableItemRequest)req);
            } else {
                requestImmutableItem((ImmutableItemRequest)req);
            }
        } else if (req instanceof MutableItemRequest) {
            if (GET_ITEM_ASYNC) {
                requestMutableItemAsync((MutableItemRequest)req);
            } else {
                requestMutableItem((MutableItemRequest)req);
            }
        } else if (req instanceof ImmutableItemDistribution) {
            putImmutableItem((ImmutableItemDistribution)req);
        } else if (req instanceof MutableItemDistribution) {
            putMutableItem((MutableItemDistribution)req);
        }
    }

    private void requestImmutableItem(ImmutableItemRequest req) {
        long startTime = System.nanoTime();
        byte[] item = session.dhtGet(req.getSpec());
        long timeCost = (System.nanoTime() - startTime) / 1000000;

        counter.immutableItemRequest();
        if (item == null) {
            counter.immutableGettingFailed();
            logger.debug(String.format("immutable getting failed:"
                    + "time cost %d ms, fail rate:(%d/%d=%.4f)"
                    + ", req %s, callback data %s",
                    timeCost,
                    counter.getImmutableGettingFailCounter(),
                    counter.getImmutableGettingCounter(),
                    counter.getImmutableGettingFailRate(),
                    req,
                    req.getCallbackData() != null ? req.getCallbackData() : "null"
            ));
        } else {
            logger.trace(String.format("immutable getting success:"
                    + "time cost %d ms", timeCost));
        }

        req.onDHTItemGot(item);
    }

   private void requestMutableItem(MutableItemRequest req) {
        long startTime = System.nanoTime();
        byte[] item = session.dhtGet(req.getSpec());
        long timeCost = (System.nanoTime() - startTime) / 1000000;

        counter.mutableItemRequest();
        if (item == null) {
            counter.mutableGettingFailed();
            logger.debug(String.format("mutable getting failed:"
                    + "time cost %d ms, fail rate:(%d/%d=%.4f)"
                    + ", req %s, callback data %s",
                    timeCost,
                    counter.getMutableGettingFailCounter(),
                    counter.getMutableGettingCounter(),
                    counter.getMutableGettingFailRate(),
                    req,
                    req.getCallbackData() != null ? req.getCallbackData() : "null"
            ));
        } else {
            logger.trace(String.format("mutable getting success:"
                    + "time cost %d ms", timeCost));
        }                                        

        req.onDHTItemGot(item);
    }

    private void requestImmutableItemAsync(ImmutableItemRequest req) {
        req.start();
        logger.trace("get immutable item:" + req.toString());
        boolean ret = session.dhtGetAsync(req.getSpec());

        if (ret) {
            counter.immutableItemRequest();
            getCache.put(req.hash(), req);
        }
    }

    private void requestMutableItemAsync(MutableItemRequest req) {
        req.start();
        logger.trace("get mutable item:" + req.toString());
        boolean ret = session.dhtGetAsync(req.getSpec());

        if (ret) {
            counter.mutableItemRequest();
            getCache.put(req.hash(), req);
        }
    }

    private void putImmutableItem(ImmutableItemDistribution d) {
        d.start();
        logger.trace("put immutable item:" + d.toString());
        Sha1Hash hash = session.dhtPut(d.item);

        // add d into putCache
        if (hash != null) {
            putCache.put(d.hash(), d);
        }
    }

    private void putMutableItem(MutableItemDistribution d) {
        d.start();
        logger.trace("put mutable item:" + d.toString());
        boolean ret = session.dhtPut(d.item);

        // add d into putCache
        if (ret) {
            putCache.put(d.hash(), d);
        }
    }

    private ItemGotPutListener listener = new ItemGotPutListener() {

        @Override
        public void onImmutableItemGot(DhtImmutableItemAlert a) {
            handleImmutableItemGot(a);
        }

        @Override
        public void onMutableItemGot(DhtMutableItemAlert a) {
            handleMutableItemGot(a);
        }

        @Override
        public void onItemPut(DhtPutAlert a) {
            Sha1Hash target = a.target();

            if (target.isAllZeros()) {
                handleMutableItemPutCompleted(a);
            } else {
                handleImmutableItemPutCompleted(a);
            }
        }
    };

    private void handleImmutableItemGot(DhtImmutableItemAlert a) {
        if (!GET_ITEM_ASYNC) {
            return;
        }

        Sha1Hash hash = a.target();
        if (hash == null) {
            return;
        }

        Entry item = a.item();
        // Get callback from getCache
        Object r = getCache.get(hash);
        if (r == null) {
            logger.warn("immutable item got:not found cache for " + hash);
            return;
        }

        ImmutableItemRequest req = (ImmutableItemRequest)r;
        req.end();
        long timeCost = req.cost() / 1000000;

        byte[] data = null;

        if (item != null && !Utils.isEntryUndefined(item)) {
            data = Utils.stringEntryToBytes(item);
            logger.trace(String.format("immutable getting success:"
                    + "time cost %d ms, hash: %s", timeCost, hash.toString()));
        } else {
            counter.immutableGettingFailed();
            logger.debug(String.format("immutable getting failed:"
                    + "time cost %d ms, fail rate:(%d/%d=%.4f)"
                    + ", req %s, callback data %s",
                    timeCost,
                    counter.getImmutableGettingFailCounter(),
                    counter.getImmutableGettingCounter(),
                    counter.getImmutableGettingFailRate(),
                    req,
                    req.getCallbackData() != null ? req.getCallbackData() : "null"
            ));
        }

        req.onDHTItemGot(data);
        getCache.remove(hash);
        logger.trace("immutable item got:" + hash + ", cache size:" + getCache.size());
    }

    private void handleMutableItemGot(DhtMutableItemAlert a) {
        if (!GET_ITEM_ASYNC) {
            return;
        }

        byte[] publicKey = a.key();
        byte[] salt = a.salt();

        if (publicKey == null || salt == null) {
            return;
        }

        Sha1Hash hash = MutableItem.computeHash(publicKey, salt);

        Entry item = a.item();
        // Get callback from getCache
        Object r = getCache.get(hash);
        if (r == null) {
            logger.warn("mutable item got:not found cache for "
                    + Hex.toHexString(publicKey) + "/" + new String(salt));
            return;
        }

        MutableItemRequest req = (MutableItemRequest)r;
        req.end();
        long timeCost = req.cost() / 1000000;

        byte[] data = null;

        if (item != null && !Utils.isEntryUndefined(item)) {
            data = Utils.stringEntryToBytes(item);
            logger.trace(String.format("mutable getting success:"
                    + "time cost %d ms, pubkey:%s, salt:%s",
                    timeCost, Hex.toHexString(publicKey), new String(salt)));
        } else {
            counter.mutableGettingFailed();
            logger.debug(String.format("mutable getting failed:"
                    + "time cost %d ms, fail rate:(%d/%d=%.4f)"
                    + ", req %s, callback data %s",
                    timeCost,
                    counter.getMutableGettingFailCounter(),
                    counter.getMutableGettingCounter(),
                    counter.getMutableGettingFailRate(),
                    req,
                    req.getCallbackData() != null ? req.getCallbackData() : "null"
            ));
        }

        req.onDHTItemGot(data);
        getCache.remove(hash);
        logger.trace("mutable item got:" + Hex.toHexString(publicKey)
                + "/" + new String(salt) + ", cache size:" + getCache.size());
    }

    private void handleImmutableItemPutCompleted(DhtPutAlert a) {
        Sha1Hash hash = a.target();
        if (hash == null) {
            return;
        }

        // Get put callback from putCache
        Object distribution = putCache.get(hash);
        if (distribution == null) {
            logger.warn("immutable item put completed:not found cache for " + hash);
            return;
        }

        ImmutableItemDistribution d = (ImmutableItemDistribution)distribution;
        dht_put_alert putAlert = a.swig();
        d.onDHTItemPut(putAlert.getNum_success());

        putCache.remove(hash);

        logger.trace("immutable item put completed:" + hash + ", cache size:" + putCache.size());
    }

    private void handleMutableItemPutCompleted(DhtPutAlert a) {
        byte[] publicKey = a.publicKey();
        byte[] salt = a.salt();

        if (publicKey == null || salt == null) {
            return;
        }

        Sha1Hash hash = MutableItem.computeHash(publicKey, salt);

        // Get put callback from putCache
        Object distribution = putCache.get(hash);
        if (distribution == null) {
            logger.warn("mutable item put completed:not found cache for "
                    + Hex.toHexString(publicKey) + "/" + new String(salt));
            return;
        }

        MutableItemDistribution d = (MutableItemDistribution)distribution;
        dht_put_alert putAlert = a.swig();
        d.onDHTItemPut(putAlert.getNum_success());

        putCache.remove(hash);

        logger.trace("mutable item put completed:" + Hex.toHexString(publicKey)
                + "/" + new String(salt) + ", cache size:" + putCache.size());
    }

}
