package io.taucoin.dht2;

import io.taucoin.account.AccountManager;
import io.taucoin.account.KeyChangedListener;
import io.taucoin.dht2.metrics.Counter;
import io.taucoin.dht2.session.SessionInfo;
import io.taucoin.dht2.session.SessionSettings;
import io.taucoin.dht2.session.TauSession;
import io.taucoin.dht2.util.Utils;
import io.taucoin.listener.TauListener;

import com.frostwire.jlibtorrent.alerts.DhtImmutableItemAlert;
import com.frostwire.jlibtorrent.alerts.DhtMutableItemAlert;
import com.frostwire.jlibtorrent.alerts.DhtPutAlert;
import com.frostwire.jlibtorrent.Entry;
import com.frostwire.jlibtorrent.Pair;
import com.frostwire.jlibtorrent.Sha1Hash;
import com.frostwire.jlibtorrent.swig.dht_put_alert;
import org.spongycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static io.taucoin.dht2.DHT.*;
import static io.taucoin.dht2.DHTReqResult.*;
import static io.taucoin.dht2.session.TauSession.ItemGotPutListener;

/**
 * DHTEngine is the bridge between TAU and torrent dht network.
 */
public class DHTEngine {

    private static final Logger logger = LoggerFactory.getLogger("DHTEngine");

    // libtorrent listening port
    private static final int LISTEN_PORT = 6881;

    private static final long CACHE_CHECK_PERIOD = 60 * 1000; // milliseconds

    private static final long CACHE_TIMEOUT_THRESOLD = 60 * 1000; // milliseconds

    private static volatile DHTEngine INSTANCE;

    // counter for dht immutable and mutable item request
    private Counter counter;

    private TauListener tauListener;

    // Cache map from sha1 hash to putting immutable or mutable item request.
    private Map<Sha1Hash, Object> putCache = new ConcurrentHashMap<Sha1Hash, Object>();

    // Cache map from sha1 hash to getting immutable or mutable item specification.
    private Map<Sha1Hash, Object> getCache = new ConcurrentHashMap<Sha1Hash, Object>();

    // The wrapper of session manager.
    private TauSession session;

    // Clear all caches when key changed.
    private Pair<byte[], byte[]> key;
    private KeyChangedListener keyChangedHandler = new KeyChangedListener() {

        @Override
        public void onKeyChanged(Pair<byte[], byte[]> newKey) {

            if (key != null && !Arrays.equals(key.first, newKey.first)) {
                key = new Pair<byte[], byte[]>(newKey.first, newKey.second);
                logger.info("Key changed and clear all caches");

                putCache.clear();
                getCache.clear();
            } else if (key == null) {
                key = new Pair<byte[], byte[]>(newKey.first, newKey.second);
                logger.info("update new key");
            }
        }
    };

    private Timer getCacheCheker = new Timer(true);
    private Timer putCacheCheker = new Timer(true);

    private TimerTask getCacheTimeoutTask = new TimerTask() {

        @Override
        public void run() {
            checkGetCache();
        }
    };

    private TimerTask putCacheTimeoutTask = new TimerTask() {

        @Override
        public void run() {
            checkPutCache();
        }
    };

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
        this.session = null;

        // register the event listener of key changed.
        this.key = null;
        AccountManager.getInstance().addListener(keyChangedHandler);
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
     * @param interfacesQuota interfaces quota
     */
    public boolean start(int interfacesQuota) {

        SessionSettings.Builder builder = new SessionSettings.Builder()
                .setNetworkInterfaces(NetworkInterfacePolicy
                        .networkInterfaces(0, interfacesQuota));
        this.session = new TauSession(builder.build());

        boolean ok = this.session.start();

        if (ok) {
            logger.info("dht sessions start successfully");
            this.session.addListener(listener);
            tauListener.onDHTStarted(true, "");
        } else {
            logger.error("dht sessions start failed");
            tauListener.onDHTStarted(false, "listen failed");
        }

        putCache.clear();
        getCache.clear();

        getCacheCheker.schedule(getCacheTimeoutTask, 0, CACHE_CHECK_PERIOD);
        putCacheCheker.schedule(putCacheTimeoutTask, 0, CACHE_CHECK_PERIOD);

        return ok;
    }

    /**
     * Stop dht engine.
     */
    public void stop() {
        session.removeListener(listener);
        session.stop();
        putCache.clear();
        getCache.clear();
        getCacheCheker.cancel();
        putCacheCheker.cancel();
        tauListener.onDHTStopped();
    }

    /**
     * Restart dht engine.
     *
     * @param interfacesQuota interfaces quota
     */
    public boolean restart(int interfacesQuota) {
        stop();
        return start(interfacesQuota);
    }

    public SessionInfo getSessionInfo() {
        return new SessionInfo(0, session.nids(), session.dhtNodes());
    }

    /**
     * Get sessions dht nodes account.
     *
     * @return long
     */
    public long getSessionNodes() {
        return session.dhtNodes();
    }

    /**
     * Set read only mode for all sessions.
     *
     * @param value
     */
    public void setReadOnly(boolean value) {
        session.setReadOnly(value);
    }

    /**
     * Put immutable item asynchronously.
     *
     * @param item immutable item.
     * @param cb callback interface
     * @param cbData callback data
     * @return DHTReqResult
     */
    public DHTReqResult distribute(ImmutableItem item, PutDHTItemCallback cb,
            Object cbData) {

        if (item == null || !session.isRunning()) {
            logger.warn("drop immutable item:" + item);
            return Dropped;
        }

        // Drop this item if it exists.
        if (putCache.get(item.hash()) != null) {
            logger.trace("duplicate immutable item:" + item);
            return Duplicated;
        }

        ImmutableItemDistribution distribution
                = new ImmutableItemDistribution(item, cb, cbData);

        if (putImmutableItem(distribution)) {
            return Success;
        }

        return Dropped;
    }

    /**
     * Put mutable item asynchronously.
     *
     * @param item mutable item.
     * @param cb callback interface
     * @param cbData callback data
     * @return DHTReqResult
     */
    public DHTReqResult distribute(MutableItem item, PutDHTItemCallback cb,
            Object cbData) {

        if (item == null || !session.isRunning()) {
            logger.warn("drop mutable item:" + item);
            return Dropped;
        }

        // Drop this item if it exists.
        if (putCache.get(item.hash()) != null) {
            logger.trace("duplicate mutable item:" + item);
            return Duplicated;
        }

        MutableItemDistribution distribution
                = new MutableItemDistribution(item, cb, cbData);

        if (putMutableItem(distribution)) {
            return Success;
        }

        return Dropped;
    }

    /**
     * Request immutable item asynchronously.
     *
     * @param spec immutable item specification
     * @param cb callback interface
     * @param cbData callback data
     * @return DHTReqResult
     */
    public DHTReqResult request(GetImmutableItemSpec spec, GetImmutableItemCallback cb,
            Object cbData) {

        if (spec == null || !session.isRunning()) {
            logger.warn("drop immutable item req:" + spec);
            return Dropped;
        }

        ImmutableItemRequest req = new ImmutableItemRequest(spec, cb, cbData);

        // Drop this request if it exists.
        if (getCache.get(req.hash()) != null) {
            logger.trace("duplicate immutable item req:" + req);
            return Duplicated;
        }

        if (requestImmutableItemAsync(req)) {
            return Success;
        }

        return Dropped;
    }

    /**
     * Request mutable item asynchronously.
     *
     * @param spec mutable item specification
     * @param cb callback interface
     * @param cbData callback data
     * @return DHTReqResult
     */
    public DHTReqResult request(GetMutableItemSpec spec, GetMutableItemCallback cb,
            Object cbData) {

        if (spec == null || !session.isRunning()) {
            logger.warn("drop mutable item req:" + spec);
            return Dropped;
        }

        MutableItemRequest req = new MutableItemRequest(spec, cb, cbData);

        // Drop this request if it exists.
        if (getCache.get(req.hash()) != null) {
            logger.trace("duplicate mutable item req:" + req);
            return Duplicated;
        }

        if (requestMutableItemAsync(req)) {
            return Success;
        }

        return Dropped;
    }

    private boolean requestImmutableItemAsync(ImmutableItemRequest req) {
        req.start();
        logger.trace("get immutable item:" + req.toString());
        boolean ret = session.dhtGetAsync(req.getSpec());

        if (ret) {
            counter.immutableItemRequest();
            getCache.put(req.hash(), req);
            return ret;
        }

        return false;
    }

    private boolean requestMutableItemAsync(MutableItemRequest req) {
        req.start();
        logger.trace("get mutable item:" + req.toString());
        boolean ret = session.dhtGetAsync(req.getSpec());

        if (ret) {
            counter.mutableItemRequest();
            getCache.put(req.hash(), req);
            return ret;
        }

        return false;
    }

    private boolean putImmutableItem(ImmutableItemDistribution d) {
        d.start();
        logger.trace("put immutable item:" + d.toString());
        Sha1Hash hash = session.dhtPut(d.item);

        // add d into putCache
        if (hash != null) {
            putCache.put(d.hash(), d);
            return true;
        }

        return false;
    }

    private boolean putMutableItem(MutableItemDistribution d) {
        d.start();
        logger.trace("put mutable item:" + d.toString());
        boolean ret = session.dhtPut(d.item);

        // add d into putCache
        if (ret) {
            putCache.put(d.hash(), d);
            return true;
        }

        return false;
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

        boolean auth = a.swig().getAuthoritative();

        req.onDHTItemGot(data, auth);
        if (auth) {
            getCache.remove(hash);
        }
        logger.trace("mutable item got:" + Hex.toHexString(publicKey)
                + "/" + new String(salt) + ", auth:" + auth
                + ", cache size:" + getCache.size());
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

    private void checkGetCache() {
        Iterator<Map.Entry<Sha1Hash, Object>> it = getCache.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<Sha1Hash, Object> entry = it.next();
            Object req = entry.getValue();

            if (req instanceof ImmutableItemRequest) {
                ImmutableItemRequest imReq = (ImmutableItemRequest)req;
                if (imReq.duration() / 1000000 >= CACHE_TIMEOUT_THRESOLD) {
                    logger.trace("immutable req timeout:" + imReq.toString());
                    it.remove();
                }
            } else if (req instanceof MutableItemRequest) {
                MutableItemRequest mReq = (MutableItemRequest)req;
                if (mReq.duration() / 1000000 >= CACHE_TIMEOUT_THRESOLD) {
                    logger.trace("mutable req timeout:" + mReq.toString());
                    it.remove();
                }
            } else {
                logger.warn("timeout unknow obj:" + req.toString());
            }
        }
    }

    private void checkPutCache() {
        Iterator<Map.Entry<Sha1Hash, Object>> it = putCache.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<Sha1Hash, Object> entry = it.next();
            Object d = entry.getValue();

            if (d instanceof ImmutableItemDistribution) {
                ImmutableItemDistribution imPut = (ImmutableItemDistribution)d; 
                if (imPut.duration() / 1000000 >= CACHE_TIMEOUT_THRESOLD) {
                    logger.trace("immutable put timeout:" + imPut.toString());
                    it.remove();
                }
            } else if (d instanceof MutableItemDistribution) {
                MutableItemDistribution mPut = (MutableItemDistribution)d;
                if (mPut.duration() / 1000000 >= CACHE_TIMEOUT_THRESOLD) {
                    logger.trace("mutable put timeout:" + mPut.toString());
                    it.remove();
                }
            } else {
                logger.warn("timeout unknow obj:" + d.toString());
            }
        }
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
