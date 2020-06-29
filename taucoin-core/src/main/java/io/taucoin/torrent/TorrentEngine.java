package io.taucoin.torrent;

import io.taucoin.listener.TauListener;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.taucoin.torrent.DHT.*;

/**
 * TorrentEngine is the bridge between tau blockchain and torrent SessionManager.
 * It is responsible for starting and stopping SessionManager.
 */
public class TorrentEngine {

    private static final Logger logger = LoggerFactory.getLogger(TorrentEngine.class);

    private static volatile TorrentEngine INSTANCE;

    // enable torrent log or not.
    private static final boolean EnableTorrentLog = false;

    // Torrent session manager.
    private SessionManager sessionManager;

    // Torrent session settings.
    private SessionSettings settings;

    // SessionManager lock.
    private Object lock;

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
	    synchronized (TorrentEngine.this.lock) {
	        sessionManager.start(settings.getSessionParams());
	    }
	}
    };

    private Thread torrentDaemon;

    /**
     * Get TorrentEngine instance.
     *
     * @return TorrentEngine instance.
     */
    public static TorrentEngine getInstance() {
        if (INSTANCE == null) {
            synchronized (TorrentEngine.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TorrentEngine();
		}
	    }
	}

	return INSTANCE;
    }

    private TorrentEngine() {

        synchronized (this.lock) {
            sessionManager = new SessionManager(EnableTorrentLog);
	    sessionManager.addListener(torrentListener);
	}
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
        synchronized (this.lock) {
	    return sessionManager;
	}
    }

    /**
     * Start torrent engine.
     *
     * @param settings SessionSettings
     */
    public void start(SessionSettings settings) {
        if (torrentDaemon != null) {
	    return;
	}

	this.settings = settings;
        torrentDaemon = new Thread(torrentSession);
	torrentDaemon.setDaemon(true);
	torrentDaemon.start();
    }

    /**
     * Stop torrent engine.
     */
    public void stop() {
        if (torrentDaemon == null) {
	    return;
	}

        synchronized (this.lock) {
	    sessionManager.stop();
	}

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
        return null;
    }

    /**
     * Put mutable item.
     *
     * @param item mutable item
     */
    public void dhtPut(MutableItem item) {
    }

    /**
     * dht tau gets immutable item.
     * Before getting any item, it must put an immutable item.
     *
     * @param spec getting immutable item specification
     * @param item immutable item put
     * @return ExchangeImmutableItemResult exchange result
     */
    public ExchangeImmutableItemResult exchange(GetImmutableItemSpec spec,
            ImmutableItem item) {
        return null;
    }

    /**
     * dht tau gets mutable item.
     * Before getting any item, it must put a mutable item.
     *
     * @param spec getting mutable item specification
     * @param item mutable item put
     * @return ExchangeMutableItemResult exchange result
     */
    public ExchangeMutableItemResult exchange(GetMutableItemSpec sepc,
            MutableItem item) {
        return null;
    }
}
