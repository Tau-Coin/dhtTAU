package io.taucoin.dht.session;

import io.taucoin.dht.metrics.Counter;
import io.taucoin.dht.util.Utils;

import com.frostwire.jlibtorrent.alerts.DhtPutAlert;
import com.frostwire.jlibtorrent.Sha1Hash;
import com.frostwire.jlibtorrent.swig.dht_put_alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.concurrent.BlockingQueue;
import java.util.Map;

import static io.taucoin.dht.DHT.*;
import static io.taucoin.dht.session.TauSession.ItemPutListener;

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

            session.addListener(putListener);

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
            Map<Sha1Hash, Object> putCache) {

        this.index = index;
        this.session = session;
        this.inputQueue = inputQueue;
        this.counter = counter;
        this.regulator = regulator;
        this.putCache = putCache;

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
            requestImmutableItem((ImmutableItemRequest)req);
        } else if (req instanceof MutableItemRequest) {
            requestMutableItem((MutableItemRequest)req);
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

    private void putImmutableItem(ImmutableItemDistribution d) {
        Sha1Hash hash = session.dhtPut(d.item);

        // add d into putCache
        if (hash != null) {
            putCache.put(d.hash(), d);
        }
    }

    private void putMutableItem(MutableItemDistribution d) {
        boolean ret = session.dhtPut(d.item);

        // add d into putCache
        if (ret) {
            putCache.put(d.hash(), d);
        }
    }

    private ItemPutListener putListener = new ItemPutListener() {

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
