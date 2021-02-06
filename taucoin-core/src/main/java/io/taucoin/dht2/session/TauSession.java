package io.taucoin.dht2.session;

import io.taucoin.dht2.util.Utils;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.*;
import com.frostwire.jlibtorrent.swig.entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;

import static io.taucoin.dht2.DHT.*;

/**
 * TauSession represents one torrent session.
 */
public class TauSession {

    private static final Logger logger = LoggerFactory.getLogger("TauSession");

    // enable torrent log or not.
    private static final boolean EnableTorrentLog = false;

    private static final String MUTABLE_PUT_MESSAGE_FORMAT
            = "DHT put complete (success=%d key=%s sig=%s salt=%s seq=%d)";

    private static final String IMMUTABLE_PUT_MESSAGE_FORMAT
            = "DHT put commplete (success=%d hash=%s)";

    // Torrent session manager.
    private SessionManager sessionManager;

    private volatile SessionHandle sessionHandle;

    // node id(s)
    private String nids = null;

    // Torrent session settings.
    private SessionSettings settings;

    private static int[] listenAlerts = new int[] {
            AlertType.LISTEN_SUCCEEDED.swig(),
            AlertType.LISTEN_FAILED.swig(),
            AlertType.DHT_IMMUTABLE_ITEM.swig(),
            AlertType.DHT_MUTABLE_ITEM.swig(),
            AlertType.DHT_PUT.swig(),
            AlertType.DHT_LOG.swig(),
            AlertType.PORTMAP.swig(),
            //AlertType.DHT_PKT.swig(),
    };

    private AlertListener torrentListener = new AlertListener() {

        @Override
        public int[] types() {
            return listenAlerts;
        }

        @Override
        public void alert(Alert<?> alert) {
            AlertType type = alert.type();

            if (type == AlertType.LISTEN_SUCCEEDED) {
                ListenSucceededAlert a = (ListenSucceededAlert) alert;
                logger.info(a.message());

                // notify starting result.
                synchronized (signal) {
                    if (!TauSession.this.startingResultReceived) {
                        TauSession.this.startingResultReceived = true;
                        TauSession.this.startingResult = true;
                        signal.notify();
                    }
                }
            } else if (type == AlertType.LISTEN_FAILED) {
                ListenFailedAlert a = (ListenFailedAlert) alert;
                logger.info(a.message());

                // notify starting result.
                synchronized (signal) {
                    if (!TauSession.this.startingResultReceived) {
                        TauSession.this.startingResultReceived = true;
                        TauSession.this.startingResult = false;
                        signal.notify();
                    }
                }
            } else if (type == AlertType.DHT_PUT) {
                DhtPutAlert a = (DhtPutAlert) alert;
                // logger.info(a.message());
                logger.info(putAlertMessage(a));
                notifyItemPutEvent(a);
            } else if (type == AlertType.DHT_IMMUTABLE_ITEM) {
                DhtImmutableItemAlert a = (DhtImmutableItemAlert) alert;
                notifyImmutableItemGotEvent(a);
            } else if (type == AlertType.DHT_MUTABLE_ITEM) {
                DhtMutableItemAlert a = (DhtMutableItemAlert) alert;
                notifyMutableItemGotEvent(a);
            } else if (type == AlertType.PORTMAP) {
                PortmapAlert a = (PortmapAlert) alert;
                logger.info(a.message());
            } else if (type == AlertType.DHT_LOG) {
                if (EnableTorrentLog) {
                    DhtLogAlert a = (DhtLogAlert) alert;
                    logger.info(a.message());
                }
            }
        }
    };

    // Components for waiting the result of starting session manager.
    private final Object signal = new Object();
    private volatile boolean startingResult = false;
    private volatile boolean startingResultReceived = false;

    private final Object stopSignal = new Object();
    private volatile boolean stopRequested = false;

    private Runnable task = new Runnable() {

        @Override
        public void run() {

            sessionManager.start(settings.getSessionParams());
            sessionHandle = new SessionHandle(sessionManager.swig());

            synchronized(stopSignal) {
                while (!stopRequested) {
                    logger.info("waiting for stop");
                    try {
                        stopSignal.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        logger.error(e.toString());
                    }
                }
            }
            logger.info("session worker is exiting");
       }
    };
    private Thread worker = new Thread(task);

    // TODO: 'ItemGotPutListener'
    public static interface ItemGotPutListener {

        void onImmutableItemGot(DhtImmutableItemAlert a);

        void onMutableItemGot(DhtMutableItemAlert a);

        void onItemPut(DhtPutAlert a);
    }

    private List<ItemGotPutListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * TauSession constructor.
     *
     * @param settings SessionSettings
     */
    public TauSession(SessionSettings settings) {
        sessionManager = new SessionManager(EnableTorrentLog);
        sessionManager.addListener(torrentListener);

        this.settings = settings;
    }

    /**
     * Register ItemGotPutListener
     *
     * @param listener
     */
    public void addListener(ItemGotPutListener listener) {
        listeners.add(listener);
    }

    /**
     * Unregister ItemGotPutListener
     *
     * @param listener
     */
    public void removeListener(ItemGotPutListener listener) {
        listeners.remove(listener);
    }

    private void notifyImmutableItemGotEvent(DhtImmutableItemAlert a) {
        for (ItemGotPutListener listener : listeners) {
            listener.onImmutableItemGot(a);
        }
    }
    private void notifyMutableItemGotEvent(DhtMutableItemAlert a) {
        for (ItemGotPutListener listener : listeners) {
            listener.onMutableItemGot(a);
        }
    }

    private void notifyItemPutEvent(DhtPutAlert a) {
        for (ItemGotPutListener listener : listeners) {
            listener.onItemPut(a);
        }
    }

    /**
     * Start Tau Session.
     */
    public boolean start() {
        if (sessionManager.isRunning()) {
            return true;
        }

        logger.info("starting session");
        worker.start();

        // wait for starting result.
        synchronized (signal) {
            while (!startingResultReceived) {
                try {
                    signal.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    logger.error(e.getMessage());
                    Utils.printStacktraceToLogger(logger, e);
                }
            }
        }

        return startingResult;
    }

    /**
     * Stop torrent dht engine.
     */
    public void stop() {
        if (!sessionManager.isRunning()) {
            return;
        }

        synchronized(stopSignal) {
            stopRequested = true;
            stopSignal.notify();
        }

        logger.info("stopping session");
        sessionManager.stop();
    }

    /**
     * Session is running or not.
     *
     * @return boolean
     */

    public boolean isRunning() {
        return sessionManager.isRunning();
    }

    /**
     * Get node id(s).
     *
     * @return String
     */
    public String nids() {
        if (nids != null) {
            return nids;
        }

        // get nids
        nids = getNodeIds();
        logger.info("node ids:" + nids);

        return nids;
    }

    /**
     * Get account of dht nodes online.
     *
     * @return long
     */
    public long dhtNodes() {
        return sessionManager.dhtNodes();
    }

    /**
     * Set read only mode for this session.
     *
     * When set, the other nodes won't keep this node in their routing
     * tables, it's meant for low-power and/or ephemeral devices that
     * cannot support the DHT, it is also useful for mobile devices which
     * are sensitive to network traffic and battery life.
     * this node no longer responds to 'query' messages, and will place a
     * 'ro' key (value = 1) in the top-level message dictionary of outgoing
     * query messages.
     *
     * @param value
     */
    public void setReadOnly(boolean value) {
        DhtSettings ds = new DhtSettings();
        ds.readOnly(value);

        sessionManager.swig().set_dht_settings(ds.swig());
    }

    /**
     * Put immutable item.
     *
     * @param item immutable item
     * @return Sha1Hash sha1 hash of the immutable item
     */
    public Sha1Hash dhtPut(ImmutableItem item) {

        if (!sessionManager.isRunning()) {
            logger.warn("Session is not running");
            return null;
        }

        logger.trace("put dht item:" + item);
        return sessionManager.dhtPutItem(item.entry);
    }

    /**
     * Put mutable item.
     *
     * @param item mutable item
     */
    public boolean dhtPut(MutableItem item) {

        if (!sessionManager.isRunning()) {
            logger.warn("Session is not running");
            return false;
        }

        logger.trace("put dht item:" + item);
        sessionManager.dhtPutItem(item.publicKey, item.privateKey,
                item.entry, item.salt);

        return true;
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
            // logger.trace("immutable entry [" + spec.sha1 + "] got:" + entry.toString()
            //        + ", type:" + getEntryType(entry));

            if (!Utils.isEntryUndefined(entry)) {
                data = Utils.stringEntryToBytes(entry);
            }
        }

        return data;
    }

    /**
     * dht gets immutable item asynchronously.
     *
     * @param spec getting immutable item specification
     * @return boolean
     */
    public boolean dhtGetAsync(GetImmutableItemSpec spec) {

        if (!sessionManager.isRunning()) {
            return false;
        }

        sessionHandle.dhtGetItem(spec.sha1);

        return true;
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

        if (result == null || result.item == null
                || Utils.isEntryUndefined(result.item)) {
            return null;
        }

        //logger.trace("mutable item got:" + result.item.toString()
        //        + ", type:" + getEntryType(result.item));

        logger.info("mutable item type:" + result.item.swig().type());
        return Utils.stringEntryToBytes(result.item);
    }

    /**
     * dht tau gets mutable item asynchronously.
     *
     * @param spec getting mutable item specification
     * @return boolean
     */
    public boolean dhtGetAsync(GetMutableItemSpec spec) {

        if (!sessionManager.isRunning()) {
            return false;
        }

        sessionHandle.dhtGetItem(spec.publicKey, spec.salt);

        return true;
    }

    private String getNodeIds() {
        byte[] state = new SessionHandle(sessionManager.swig())
                .saveState(SessionHandle.SAVE_DHT_STATE);
        if (state == null) {
            return null;
        }

        Entry e = Entry.bdecode(state);
        if (e == null) {
            return null;
        }

        Map<String, Entry> map = e.dictionary();
        if (map == null) {
            return null;
        }
        Entry dhtState = map.get("dht state");
        if (dhtState == null) {
            return null;
        }

        Map<String, Entry> dhtMap = dhtState.dictionary();
        if (dhtMap == null) {
            return null;
        }
        Entry nidList = dhtMap.get("node-id");
        if (nidList == null) {
            return null;
        }
        List<Entry> nids = nidList.list();
        if (nids == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nids.size(); i++) {
            sb.append(nids.get(i).toString());
            if (i != nids.size() - 1) {
                sb.append(",");
            }
        }

        return sb.toString();
    }

    public static String putAlertMessage(DhtPutAlert a) {
        Sha1Hash target = a.target();

        if (target.isAllZeros()) {
            // mutable item put alert
            return String.format(MUTABLE_PUT_MESSAGE_FORMAT,
                    a.swig().getNum_success(),
                    Hex.toHexString(a.publicKey()),
                    Hex.toHexString(a.signature()),
                    new String(a.salt()),
                    a.seq());
        } else {
            // immutable item put alert
            return String.format(IMMUTABLE_PUT_MESSAGE_FORMAT,
                    a.swig().getNum_success(),
                    target.toString());
        }
    }
}
