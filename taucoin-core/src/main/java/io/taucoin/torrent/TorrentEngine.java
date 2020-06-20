package io.taucoin.torrent;

/**
 * TorrentEngine is the bridge between tau blockchain and torrent SessionManager.
 * It is responsible for starting and stopping SessionManager.
 */
public class TorrentEngine {

    private static volatile TorrentEngine INSTANCE;

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
    }

    /**
     * Start torrent engine.
     */
    public void start() {
    }

    /**
     * Stop torrent engine.
     */
    public void stop() {
    }
}
