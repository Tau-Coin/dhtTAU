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

    // Cache map from sha1 hash to getting immutable or mutable item specification.
    private static Map<Sha1Hash, Object> getCache = new ConcurrentHashMap<Sha1Hash, Object>();

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

                getCache.clear();
            } else if (key == null) {
                key = new Pair<byte[], byte[]>(newKey.first, newKey.second);
                logger.info("update new key");
            }
        }
    };

    private Timer getCacheCheker = null;

    private static class GetCacheTimeoutTask extends TimerTask {

        @Override
        public void run() {
            checkGetCache();
        }
    }

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
        this.session.setTauListener(tauListener);

        boolean ok = this.session.start();

        if (ok) {
            logger.info("dht sessions start successfully");
            this.session.addListener(listener);
            tauListener.onDHTStarted(true, "");
        } else {
            logger.error("dht sessions start failed");
            tauListener.onDHTStarted(false, "listen failed");
        }

        getCache.clear();

        getCacheCheker = new Timer(true);
        getCacheCheker.schedule(new GetCacheTimeoutTask(), 0, CACHE_CHECK_PERIOD);

        return ok;
    }

    /**
     * Stop dht engine.
     */
    public void stop() {
        session.removeListener(listener);
        session.stop();
        getCache.clear();
        getCacheCheker.cancel();
        getCacheCheker = null;
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
     * Get session total download.
     *
     * @return long, unit: byte
     */
    public long getSessionTotalDownload() {
        return session.dhtTotalDownload();
    }

    /**
     * Get session total upload.
     *
     * @return long, unit: byte
     */
    public long getSessionTotalUpload() {
        return session.dhtTotalUpload();
    }

    /**
     * Get session download rate
     *
     * @return long, unit: byte
     */
    public long getSessionDownloadRate() {
        return session.dhtDownloadRate();
    }

    /**
     * Get session upload rate
     *
     * @return long, unit: byte
     */
    public long getSessionUploadRate() {
        return session.dhtUploadRate();
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
     * Set the number of concurrent search request the node will send when
     * announcing and refreshing the routing table. This parameter is called
     * alpha in the kademlia paper.
     *
     * @param value
     */
    public void setSearchBranching(int value) {
        session.setSearchBranching(value);
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

        MutableItemDistribution distribution
                = new MutableItemDistribution(item, cb, cbData);

        if (putMutableItem(distribution)) {
            return Success;
        }

        return Dropped;
    }

    /**
     * Put mutable item batch asynchronously.
     *
     * @param items mutable item batch.
     * @param cb callback interface
     * @param cbData callback data
     * @return DHTReqResult
     */
    public DHTReqResult distribute(MutableItemBatch items, PutDHTItemCallback cb,
            Object cbData) {

        if (items == null || !session.isRunning() || !items.isValid()) {
            logger.warn("drop mutable item batch:" + items);
            return Dropped;
        }

        MutableItemBatchDistribution distribution
                = new MutableItemBatchDistribution(items, cb, cbData);

        if (putMutableItemBatch(distribution)) {
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

        /*
        // Drop this request if it exists.
        if (getCache.get(req.hash()) != null) {
            logger.trace("duplicate immutable item req:" + req);
            return Duplicated;
        }*/

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

        /*
        // Drop this request if it exists.
        if (getCache.get(req.hash()) != null) {
            logger.trace("duplicate mutable item req:" + req);
            return Duplicated;
        }*/

        if (requestMutableItemAsync(req)) {
            return Success;
        }

        return Dropped;
    }

    public void reopenNetworks() {
        session.reopenNetworks();
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

        if (hash != null) {
            return true;
        }

        return false;
    }

    private boolean putMutableItem(MutableItemDistribution d) {
        d.start();
        logger.trace("put mutable item:" + d.toString());
        boolean ret = session.dhtPut(d.item);

        if (ret) {
            return true;
        }

        return false;
    }

    private boolean putMutableItemBatch(MutableItemBatchDistribution d) {
        d.start();
        logger.trace("put mutable item batch:" + d.toString());
        boolean ret = session.dhtPut(d.items);

        if (ret) {
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
    }

    private void handleMutableItemPutCompleted(DhtPutAlert a) {
    }

    private static void checkGetCache() {
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
