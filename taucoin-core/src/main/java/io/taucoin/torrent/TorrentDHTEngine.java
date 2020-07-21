package io.taucoin.torrent;

import io.taucoin.listener.TauListener;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

import static io.taucoin.torrent.DHT.*;

/**
 * TorrentDHTEngine is the bridge between tau blockchain and torrent SessionManager.
 * It is responsible for starting and stopping SessionManager.
 */
public class TorrentDHTEngine {

    private static final Logger logger = LoggerFactory.getLogger(TorrentDHTEngine.class);

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
            }

            if (type == AlertType.LISTEN_FAILED) {
                ListenFailedAlert a = (ListenFailedAlert) alert;
                logger.info(a.message());
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

    private Runnable torrentSession = new Runnable() {
        @Override
        public void run() {
            sessionManager.start(settings.getSessionParams());
        }
    };

    private Thread torrentDaemon;

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

        sessionManager.stop();

        torrentDaemon.interrupt();
        try {
            torrentDaemon.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        torrentDaemon = null;
        this.settings = null;
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
            String str =  entry.string();
            try {
                data = str.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
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

        if (result == null || result.item == null) {
            return null;
        }

        return result.item.bencode();
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
}
