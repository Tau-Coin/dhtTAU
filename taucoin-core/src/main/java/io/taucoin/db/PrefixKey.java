package io.taucoin.db;

public class PrefixKey {
    // prefix
    public static final byte[] chain = "C".getBytes();
    private static final byte[] bestBlock = "B".getBytes();
    private static final byte[] mutableRange = "M".getBytes();
    private static final byte[] immutablePointBlockHash = "I".getBytes();
    private static final byte[] votesCountingPointBlockHash = "V".getBytes();
    private static final byte[] peer = "P".getBytes();
    private static final byte[] txPool = "T".getBytes();

    /**
     * chain key: "Chain" + chainID
     * @param chainID
     * @return
     */
    public static byte[] chainKey(byte[] chainID) {
        byte[] key = new byte[chain.length + chainID.length];
        System.arraycopy(chain, 0, key, 0, chain.length);
        System.arraycopy(chainID, 0, key, chain.length, chainID.length);
        return key;
    }

    /**
     * best block hash key: chainID + "BestBlock"
     * @param chainID
     * @return
     */
    public static byte[] bestBlockHashKey(byte[] chainID) {
        byte[] key = new byte[chainID.length + bestBlock.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(bestBlock, 0, key, chainID.length, bestBlock.length);
        return key;
    }

    /**
     * immutable point block hash key: chainID + "ImmutablePointBlockHash"
     * @param chainID
     * @return
     */
    public static byte[] immutablePointBlockHashKey(byte[] chainID) {
        byte[] key = new byte[chainID.length + immutablePointBlockHash.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(immutablePointBlockHash, 0, key, chainID.length, immutablePointBlockHash.length);
        return key;
    }

    /**
     * votes counting point block hash key: chainID + "VotesCountingPointBlockHash"
     * @param chainID
     * @return
     */
    public static byte[] votesCountingPointBlockHashKey(byte[] chainID) {
        byte[] key = new byte[chainID.length + votesCountingPointBlockHash.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(votesCountingPointBlockHash, 0, key, chainID.length, votesCountingPointBlockHash.length);
        return key;
    }

    /**
     * mutable range key: chainID + "MutableRange"
     * @param chainID
     * @return
     */
    public static byte[] mutableRangeKey(byte[] chainID) {
        byte[] key = new byte[chainID.length + mutableRange.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(mutableRange, 0, key, chainID.length, mutableRange.length);
        return key;
    }

    /**
     * peer prefix: chainID + "Peer"
     * @param chainID
     * @return
     */
    public static byte[] peerKeyPrefix(byte[] chainID) {
        byte[] prefix = new byte[chainID.length + peer.length];
        System.arraycopy(chainID, 0, prefix, 0, chainID.length);
        System.arraycopy(peer, 0, prefix, chainID.length, peer.length);
        return prefix;
    }

    /**
     * peer key: chainID + "Peer" + peer pubkey
     * @param chainID
     * @param pubkey
     * @return
     */
    public static byte[] peerKey(byte[] chainID, byte[] pubkey) {
        byte[] key = new byte[chainID.length + peer.length + pubkey.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(peer, 0, key, chainID.length, peer.length);
        System.arraycopy(pubkey, 0, key, chainID.length + peer.length, pubkey.length);
        return key;
    }

    /**
     * tx pool prefix: chainID + "TxPool"
     * @param chainID
     * @return
     */
    public static byte[] txPoolPrefix(byte[] chainID) {
        byte[] prefix = new byte[chainID.length + txPool.length];
        System.arraycopy(chainID, 0, prefix, 0, chainID.length);
        System.arraycopy(txPool, 0, prefix, chainID.length, txPool.length);
        return prefix;
    }

    /**
     * tx pool key: chainID + "TxPool" + txid
     * @param chainID
     * @param txid
     * @return
     */
    public static byte[] txPoolKey(byte[] chainID, byte[] txid) {
        byte[] key = new byte[chainID.length + txPool.length + txid.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(txPool, 0, key, chainID.length, txPool.length);
        System.arraycopy(txid, 0, key, chainID.length + txPool.length, txid.length);
        return key;
    }

    /**
     * account key: chainID + pubkey
     * @param chainID
     * @param pubkey
     * @return
     */
    public static byte[] accountKey(byte[] chainID, byte[] pubkey) {
        byte[] key = new byte[chainID.length + pubkey.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(pubkey, 0, key, chainID.length, pubkey.length);
        return key;
    }

    /**
     * block key: chainID + hash
     * @param chainID
     * @param hash
     * @return
     */
    public static byte[] blockKey(byte[] chainID, byte[] hash) {
        byte[] key = new byte[chainID.length + hash.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(hash, 0, key, chainID.length, hash.length);
        return key;
    }

}
