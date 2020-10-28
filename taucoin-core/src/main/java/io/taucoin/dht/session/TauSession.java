package io.taucoin.dht.session;

import io.taucoin.dht.util.Utils;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.*;
import com.frostwire.jlibtorrent.swig.entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.taucoin.dht.DHT.*;

/**
 * TauSession represents one torrent session.
 */
class TauSession {

    // enable torrent log or not.
    private static final boolean EnableTorrentLog = false;

    private Logger logger;

    // Torrent session manager.
    private SessionManager sessionManager;

    // Torrent session settings.
    private SessionSettings settings;

    private static int[] listenAlerts = new int[] {
            AlertType.LISTEN_SUCCEEDED.swig(),
            AlertType.LISTEN_FAILED.swig(),
            AlertType.DHT_PUT.swig(),
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
                logger.info(a.message());
            }
        }
    };

    // Components for waiting the result of starting session manager.
    private final Object signal = new Object();
    private volatile boolean startingResult = false;
    private volatile boolean startingResultReceived = false;

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
     * Set logger for this session.
     *
     * @param logger Logger
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Start Tau Session.
     */
    public boolean start() {
        if (sessionManager.isRunning()) {
            return true;
        }

        logger.info("starting session");
        sessionManager.start(settings.getSessionParams());

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

        logger.info("stopping session");
        sessionManager.stop();
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

        sessionManager.dhtPutItem(item.publicKey, item.privateKey,
                item.entry, item.salt);
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
}
