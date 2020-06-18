package io.taucoin.chain;

/**
 * Chain represents one blockchain for tau multi-chain system.
 * It manages blockchain core actvity, etc. voting process.
 */
public class Chain {

    // Chain id specified by the transaction of creating new blockchain.
    private String chainID;

    // Chain nick name specified by the transaction of creating new blockchain.
    private String nickName;

    /**
     * Chain constructor.
     *
     * @param chainID chain identity.
     */
    public Chain(String chainID) {
        this.chainID = chainID;
    }
}
