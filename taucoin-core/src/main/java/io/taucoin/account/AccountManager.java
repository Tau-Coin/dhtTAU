package io.taucoin.account;

import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Pair;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AccountManager manages public key and private key for the miner.
 * This class implementation is the singleton.
 * Note: it's not allowed to store the copy of the public key and private key
 * in order to support updating key online.
 */
public class AccountManager {

    private static volatile AccountManager INSTANCE;

    private Pair<byte[], byte[]> key;

    private byte[] seed;

    List<KeyChangedListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Get AccountManager instance.
     *
     * @return AccountManager instance
     */
    public static AccountManager getInstance() {
        if (INSTANCE == null) {
            synchronized (AccountManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AccountManager();
                }
            }
        }

        return INSTANCE;
    }

    /**
     * Update public key and private key.
     *
     * @param key pair of public key and private key.
     */
    public synchronized void updateKey(Pair<byte[], byte[]> key) {
        this.key = key;
        notifyKeyChanged(this.key);
    }

    /**
     * Update public key and private key.
     *
     * @param seed the seed to generate key pair
     */
    public synchronized void updateKey(byte[] seed) {
        this.seed = seed;
        this.key = Ed25519.createKeypair(seed);
        notifyKeyChanged(this.key);
    }

    /**
     * Get key pair.
     *
     * @return Pair<byte[], byte[]> the pair of public key and private key.
     */
    public synchronized Pair<byte[], byte[]> getKeyPair() {
        return this.key;
    }

    public void addListener(KeyChangedListener listener) {
        listeners.add(listener);
    }

    public void removeListener(KeyChangedListener listener) {
        listeners.remove(listener);
    }

    private void notifyKeyChanged(Pair<byte[], byte[]> newKey) {
        for (KeyChangedListener listener : listeners) {
            listener.onKeyChanged(newKey);
        }
    }
}
