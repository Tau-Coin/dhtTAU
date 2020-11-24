package io.taucoin.db;

import io.taucoin.util.ByteUtil;

public class PrefixKey {
    // prefix
    private static final byte[] ACCOUNT = "A-".getBytes();
    private static final byte[] BLOCK = "B-".getBytes();
    private static final byte[] BEST_BLOCK = "b-".getBytes();
    public static final byte[] CHAIN = "C-".getBytes();
    public static final byte[] HASH_LIST = "H-".getBytes();
    private static final byte[] BLOCK_INFO = "I-".getBytes();
    private static final byte[] IMMUTABLE_POINT_BLOCK_HASH = "i-".getBytes();
    private static final byte[] PEER_LATEST_MESSAGE = "LM-".getBytes();
    private static final byte[] MUTABLE_RANGE = "M-".getBytes();
    private static final byte[] MESSAGE = "m-".getBytes();
    private static final byte[] PEER = "P-".getBytes();
    private static final byte[] COMMUNICATION_PEER = "CP-".getBytes();
    private static final byte[] SYNC_BLOCK_HASH = "s-".getBytes();
    private static final byte[] TX_POOL = "T-".getBytes();
    private static final byte[] TX = "t-".getBytes();
    private static final byte[] VOTES_COUNTING_POINT_BLOCK_HASH = "V-".getBytes();

    // key

    /**
     * chain key: 'Chain' + chainID
     * @param chainID
     * @return
     */
    public static byte[] chainKey(byte[] chainID) {
        byte[] key = new byte[CHAIN.length + chainID.length];
        System.arraycopy(CHAIN, 0, key, 0, CHAIN.length);
        System.arraycopy(chainID, 0, key, CHAIN.length, chainID.length);
        return key;
    }

    /**
     * best block hash key: chainID + 'BestBlock'
     * @param chainID
     * @return
     */
    public static byte[] bestBlockHashKey(byte[] chainID) {
        byte[] key = new byte[chainID.length + BEST_BLOCK.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(BEST_BLOCK, 0, key, chainID.length, BEST_BLOCK.length);
        return key;
    }

    /**
     * synced block hash key: chainID + 'SyncBlockHash'
     * @param chainID
     * @return
     */
    public static byte[] syncBlockHashKey(byte[] chainID) {
        byte[] key = new byte[chainID.length + SYNC_BLOCK_HASH.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(SYNC_BLOCK_HASH, 0, key, chainID.length, SYNC_BLOCK_HASH.length);
        return key;
    }

    /**
     * immutable point block hash key: chainID + 'ImmutablePointBlockHash'
     * @param chainID
     * @return
     */
    public static byte[] immutablePointBlockHashKey(byte[] chainID) {
        byte[] key = new byte[chainID.length + IMMUTABLE_POINT_BLOCK_HASH.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(IMMUTABLE_POINT_BLOCK_HASH, 0, key, chainID.length, IMMUTABLE_POINT_BLOCK_HASH.length);
        return key;
    }

    /**
     * votes counting point block hash key: chainID + 'VotesCountingPointBlockHash'
     * @param chainID
     * @return
     */
    public static byte[] votesCountingPointBlockHashKey(byte[] chainID) {
        byte[] key = new byte[chainID.length + VOTES_COUNTING_POINT_BLOCK_HASH.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(VOTES_COUNTING_POINT_BLOCK_HASH, 0, key, chainID.length, VOTES_COUNTING_POINT_BLOCK_HASH.length);
        return key;
    }

    /**
     * mutable range key: chainID + 'MutableRange'
     * @param chainID
     * @return
     */
    public static byte[] mutableRangeKey(byte[] chainID) {
        byte[] key = new byte[chainID.length + MUTABLE_RANGE.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(MUTABLE_RANGE, 0, key, chainID.length, MUTABLE_RANGE.length);
        return key;
    }

    /**
     * peer prefix: chainID + 'Peer'
     * @param chainID
     * @return
     */
    public static byte[] peerKeyPrefix(byte[] chainID) {
        byte[] prefix = new byte[chainID.length + PEER.length];
        System.arraycopy(chainID, 0, prefix, 0, chainID.length);
        System.arraycopy(PEER, 0, prefix, chainID.length, PEER.length);
        return prefix;
    }

    /**
     * peer key: chainID + 'Peer' + peer pubkey
     * @param chainID
     * @param pubkey
     * @return
     */
    public static byte[] peerKey(byte[] chainID, byte[] pubkey) {
        byte[] key = new byte[chainID.length + PEER.length + pubkey.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(PEER, 0, key, chainID.length, PEER.length);
        System.arraycopy(pubkey, 0, key, chainID.length + PEER.length, pubkey.length);
        return key;
    }

    /**
     * tx pool prefix: chainID + 'txPool' + pubKey
     * @param chainID
     * @return
     */
    public static byte[] txPoolAllTxPrefix(byte[] chainID, byte[] pubKey) {
        byte[] prefix = new byte[chainID.length + TX_POOL.length + pubKey.length];
        System.arraycopy(chainID, 0, prefix, 0, chainID.length);
        System.arraycopy(TX_POOL, 0, prefix, chainID.length, TX_POOL.length);
        System.arraycopy(pubKey, 0, prefix, chainID.length + TX_POOL.length, pubKey.length);
        return prefix;
    }

    /**
     * tx pool key: chainID + 'txPool' + pubKey + nonce
     * @param chainID chain ID
     * @param pubKey public key
     * @param nonce nonce
     * @return key
     */
    public static byte[] txPoolTxKey(byte[] chainID, byte[] pubKey, long nonce) {
        byte[] nonceBytes = ByteUtil.longToBytes(nonce);
        byte[] key = new byte[chainID.length + TX_POOL.length + pubKey.length + nonceBytes.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(TX_POOL, 0, key, chainID.length, TX_POOL.length);
        System.arraycopy(pubKey, 0, key, chainID.length + TX_POOL.length, pubKey.length);
        System.arraycopy(nonceBytes, 0, key, chainID.length + TX_POOL.length + pubKey.length, nonceBytes.length);
        return key;
    }

    /**
     * account prefix: chainID + 'account'
     * @param chainID
     * @return
     */
    public static byte[] accountPrefix(byte[] chainID) {
        byte[] prefix = new byte[chainID.length + ACCOUNT.length];
        System.arraycopy(chainID, 0, prefix, 0, chainID.length);
        System.arraycopy(ACCOUNT, 0, prefix, chainID.length, ACCOUNT.length);
        return prefix;
    }

    /**
     * account key: chainID + 'account' + pubkey
     * @param chainID
     * @param pubkey
     * @return
     */
    public static byte[] accountKey(byte[] chainID, byte[] pubkey) {
        byte[] key = new byte[chainID.length + ACCOUNT.length + pubkey.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(ACCOUNT, 0, key, chainID.length, ACCOUNT.length);
        System.arraycopy(pubkey, 0, key, chainID.length + ACCOUNT.length, pubkey.length);
        return key;
    }

    /**
     * block prefix: chainID + 'block'
     * @param chainID
     * @return
     */
    public static byte[] blockPrefix(byte[] chainID) {
        byte[] prefix = new byte[chainID.length + BLOCK.length];
        System.arraycopy(chainID, 0, prefix, 0, chainID.length);
        System.arraycopy(BLOCK, 0, prefix, chainID.length, BLOCK.length);
        return prefix;
    }

    /**
     * block key: chainID + 'block' + hash
     * @param chainID
     * @param hash
     * @return
     */
    public static byte[] blockKey(byte[] chainID, byte[] hash) {
        byte[] key = new byte[chainID.length + BLOCK.length + hash.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(BLOCK, 0, key, chainID.length, BLOCK.length);
        System.arraycopy(hash, 0, key, chainID.length + BLOCK.length, hash.length);
        return key;
    }

    /**
     * hash list item prefix: chainID + 'hashList'
     * @param chainID chain ID
     * @return prefix
     */
    public static byte[] hashListPrefix(byte[] chainID) {
        byte[] prefix = new byte[chainID.length + HASH_LIST.length];
        System.arraycopy(chainID, 0, prefix, 0, chainID.length);
        System.arraycopy(HASH_LIST, 0, prefix, chainID.length, HASH_LIST.length);
        return prefix;
    }

    /**
     * hash list item key: chainID + 'hashList' + hash
     * @param chainID chain ID
     * @param hash hash list item hash
     * @return key
     */
    public static byte[] hashListKey(byte[] chainID, byte[] hash) {
        byte[] key = new byte[chainID.length + HASH_LIST.length + hash.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(HASH_LIST, 0, key, chainID.length, HASH_LIST.length);
        System.arraycopy(hash, 0, key, chainID.length + HASH_LIST.length, hash.length);
        return key;
    }

    /**
     * tx prefix: chainID + 'tx'
     * @param chainID chain ID
     * @return
     */
    public static byte[] txPrefix(byte[] chainID) {
        byte[] prefix = new byte[chainID.length + TX.length];
        System.arraycopy(chainID, 0, prefix, 0, chainID.length);
        System.arraycopy(TX, 0, prefix, chainID.length, TX.length);
        return prefix;
    }

    /**
     * tx key: chainID + 'tx' + hash
     * @param chainID chain ID
     * @param txid txid
     * @return
     */
    public static byte[] txKey(byte[] chainID, byte[] txid) {
        byte[] key = new byte[chainID.length + TX.length + txid.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(TX, 0, key, chainID.length, TX.length);
        System.arraycopy(txid, 0, key, chainID.length + TX.length, txid.length);
        return key;
    }

    /**
     * block info key: chainID + 'blockInfo' + number
     * @param chainID chain ID
     * @return block info
     */
    public static byte[] blockInfoPrefix(byte[] chainID) {
        byte[] prefix = new byte[chainID.length + BLOCK_INFO.length];
        System.arraycopy(chainID, 0, prefix, 0, chainID.length);
        System.arraycopy(BLOCK_INFO, 0, prefix, chainID.length, BLOCK_INFO.length);

        return prefix;
    }

    /**
     * block info key: chainID + 'blockInfo' + number
     * @param chainID chain ID
     * @param number block number
     * @return block info
     */
    public static byte[] blockInfoKey(byte[] chainID, long number) {
        byte[] numberBytes = ByteUtil.longToBytes(number);
        byte[] key = new byte[chainID.length + BLOCK_INFO.length + numberBytes.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(BLOCK_INFO, 0, key, chainID.length, BLOCK_INFO.length);
        System.arraycopy(numberBytes, 0, key, chainID.length + BLOCK_INFO.length, numberBytes.length);
        return key;
    }

    /**
     * communication peer prefix: COMMUNICATION_PEER
     * @return perfix
     */
    public static byte[] communicationPeerPrefix() {
        return COMMUNICATION_PEER;
    }

    /**
     * communication peer key: COMMUNICATION_PEER + peer pubkey
     * @param pubkey public key
     * @return key
     */
    public static byte[] communicationPeerKey(byte[] pubkey) {
        byte[] key = new byte[COMMUNICATION_PEER.length + pubkey.length];
        System.arraycopy(COMMUNICATION_PEER, 0, key, 0, COMMUNICATION_PEER.length);
        System.arraycopy(pubkey, 0, key, COMMUNICATION_PEER.length, pubkey.length);
        return key;
    }

    /**
     * message prefix: MESSAGE
     * @return message prefix
     */
    public static byte[] messagePrefix() {
        return MESSAGE;
    }

    /**
     * message key: MESSAGE + hash
     * @param hash message hash
     * @return message key
     */
    public static byte[] messageKey(byte[] hash) {
        byte[] key = new byte[MESSAGE.length + hash.length];
        System.arraycopy(MESSAGE, 0, key, 0, MESSAGE.length);
        System.arraycopy(hash, 0, key, MESSAGE.length, hash.length);
        return key;
    }

    /**
     * peer latest message prefix: PEER_LATEST_MESSAGE
     * @return perfix
     */
    public static byte[] PeerLatestMessagePrefix() {
        return PEER_LATEST_MESSAGE;
    }

    /**
     * peer latest message key: PEER_LATEST_MESSAGE + peer pubkey
     * @param pubkey public key
     * @return key
     */
    public static byte[] PeerLatestMessageKey(byte[] pubkey) {
        byte[] key = new byte[PEER_LATEST_MESSAGE.length + pubkey.length];
        System.arraycopy(PEER_LATEST_MESSAGE, 0, key, 0, PEER_LATEST_MESSAGE.length);
        System.arraycopy(pubkey, 0, key, PEER_LATEST_MESSAGE.length, pubkey.length);
        return key;
    }

}
