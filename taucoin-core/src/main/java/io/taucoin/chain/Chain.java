package io.taucoin.chain;

import io.taucoin.listener.TauListener;

/**
 * Chain represents one blockchain for tau multi-chain system.
 * It manages blockchain core actvity, etc. voting process.
 */
public class Chain {

    // Chain id specified by the transaction of creating new blockchain.
    private byte[] chainID;

    // Chain nick name specified by the transaction of creating new blockchain.
    private String nickName;

    // Voting thread.
    private Thread votingThread;

    private TauListener tauListener;

    /**
     * Chain constructor.
     *
     * @param chainID chain identity.
     */
    public Chain(byte[] chainID, TauListener tauListener) {
        this.chainID = chainID;
        this.tauListener = tauListener;
    }

    /**
     * Start activities of this chain, mainly including votint and mining.
     *
     * @return boolean successful or not.
     */
    public boolean start() {
        return false;
    }

    /**
     * Stop all activities of this chain.
     */
    public void stop() {
    }
}
