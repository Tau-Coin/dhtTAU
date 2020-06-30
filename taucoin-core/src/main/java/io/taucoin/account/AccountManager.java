package io.taucoin.account;

import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Pair;

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
    }

    /**
     * Update public key and private key.
     *
     * @param seed the seed to generate key pair
     */
    public synchronized void updateKey(byte[] seed) {
        this.seed = seed;
	this.key = Ed25519.createKeypair(seed);
    }

    /**
     * Get key pair.
     *
     * @return Pair<byte[], byte[]> the pair of public key and private key.
     */
    public synchronized Pair<byte[], byte[]> getKeyPair() {
        return this.key;
    }

    /**
     * Get public key.
     *
     * @return public key
     */
    public synchronized byte[] getPublickey() {
        if (this.key == null) {
	    return null;
	}
        return this.key.first;
    }

    /**
     * Get private key.
     *
     * @return private key
     */
    public synchronized byte[] getPrivatekey() {
        if (this.key == null) {
	    return null;
	}
        return this.key.second;
    }
}
