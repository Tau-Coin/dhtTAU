package io.taucoin.db;

import io.taucoin.util.ByteUtil;

public class PrefixKey {
    // prefix
    private static final byte[] account = "A-".getBytes();
    private static final byte[] block = "B-".getBytes();
    private static final byte[] bestBlock = "b-".getBytes();
    public static final byte[] chain = "C-".getBytes();
    private static final byte[] blockInfo = "I-".getBytes();
    private static final byte[] immutablePointBlockHash = "i-".getBytes();
    private static final byte[] mutableRange = "M-".getBytes();
    private static final byte[] peer = "P-".getBytes();
    private static final byte[] syncBlockHash = "s-".getBytes();
    private static final byte[] txPool = "T-".getBytes();
    private static final byte[] tx = "t-".getBytes();
    private static final byte[] votesCountingPointBlockHash = "V-".getBytes();

    /**
     * chain key: 'Chain' + chainID
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
     * best block hash key: chainID + 'BestBlock'
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
     * synced block hash key: chainID + 'SyncBlockHash'
     * @param chainID
     * @return
     */
    public static byte[] syncBlockHashKey(byte[] chainID) {
        byte[] key = new byte[chainID.length + syncBlockHash.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(syncBlockHash, 0, key, chainID.length, syncBlockHash.length);
        return key;
    }

    /**
     * immutable point block hash key: chainID + 'ImmutablePointBlockHash'
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
     * votes counting point block hash key: chainID + 'VotesCountingPointBlockHash'
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
     * mutable range key: chainID + 'MutableRange'
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
     * peer prefix: chainID + 'Peer'
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
     * peer key: chainID + 'Peer' + peer pubkey
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
     * tx pool prefix: chainID + 'txPool' + pubKey
     * @param chainID
     * @return
     */
    public static byte[] txPoolAllTxPrefix(byte[] chainID, byte[] pubKey) {
        byte[] prefix = new byte[chainID.length + txPool.length + pubKey.length];
        System.arraycopy(chainID, 0, prefix, 0, chainID.length);
        System.arraycopy(txPool, 0, prefix, chainID.length, txPool.length);
        System.arraycopy(pubKey, 0, prefix, chainID.length + txPool.length, pubKey.length);
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
        byte[] key = new byte[chainID.length + txPool.length + pubKey.length + nonceBytes.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(txPool, 0, key, chainID.length, txPool.length);
        System.arraycopy(pubKey, 0, key, chainID.length + txPool.length, pubKey.length);
        System.arraycopy(nonceBytes, 0, key, chainID.length + txPool.length + pubKey.length, nonceBytes.length);
        return key;
    }

    /**
     * account prefix: chainID + 'account'
     * @param chainID
     * @return
     */
    public static byte[] accountPrefix(byte[] chainID) {
        byte[] prefix = new byte[chainID.length + account.length];
        System.arraycopy(chainID, 0, prefix, 0, chainID.length);
        System.arraycopy(account, 0, prefix, chainID.length, account.length);
        return prefix;
    }

    /**
     * account key: chainID + 'account' + pubkey
     * @param chainID
     * @param pubkey
     * @return
     */
    public static byte[] accountKey(byte[] chainID, byte[] pubkey) {
        byte[] key = new byte[chainID.length + account.length + pubkey.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(account, 0, key, chainID.length, account.length);
        System.arraycopy(pubkey, 0, key, chainID.length + account.length, pubkey.length);
        return key;
    }

    /**
     * block prefix: chainID + 'block'
     * @param chainID
     * @return
     */
    public static byte[] blockPrefix(byte[] chainID) {
        byte[] prefix = new byte[chainID.length + block.length];
        System.arraycopy(chainID, 0, prefix, 0, chainID.length);
        System.arraycopy(block, 0, prefix, chainID.length, block.length);
        return prefix;
    }

    /**
     * block key: chainID + 'block' + hash
     * @param chainID
     * @param hash
     * @return
     */
    public static byte[] blockKey(byte[] chainID, byte[] hash) {
        byte[] key = new byte[chainID.length + block.length + hash.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(block, 0, key, chainID.length, block.length);
        System.arraycopy(hash, 0, key, chainID.length + block.length, hash.length);
        return key;
    }

    /**
     * tx prefix: chainID + 'tx'
     * @param chainID chain ID
     * @return
     */
    public static byte[] txPrefix(byte[] chainID) {
        byte[] prefix = new byte[chainID.length + tx.length];
        System.arraycopy(chainID, 0, prefix, 0, chainID.length);
        System.arraycopy(tx, 0, prefix, chainID.length, tx.length);
        return prefix;
    }

    /**
     * tx key: chainID + 'tx' + hash
     * @param chainID chain ID
     * @param txid txid
     * @return
     */
    public static byte[] txKey(byte[] chainID, byte[] txid) {
        byte[] key = new byte[chainID.length + tx.length + txid.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(tx, 0, key, chainID.length, tx.length);
        System.arraycopy(txid, 0, key, chainID.length + tx.length, txid.length);
        return key;
    }

    /**
     * block info key: chainID + 'blockInfo' + number
     * @param chainID chain ID
     * @return block info
     */
    public static byte[] blockInfoPrefix(byte[] chainID) {
        byte[] prefix = new byte[chainID.length + blockInfo.length];
        System.arraycopy(chainID, 0, prefix, 0, chainID.length);
        System.arraycopy(blockInfo, 0, prefix, chainID.length, blockInfo.length);

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
        byte[] key = new byte[chainID.length + blockInfo.length + numberBytes.length];
        System.arraycopy(chainID, 0, key, 0, chainID.length);
        System.arraycopy(blockInfo, 0, key, chainID.length, blockInfo.length);
        System.arraycopy(numberBytes, 0, key, chainID.length + blockInfo.length, numberBytes.length);
        return key;
    }

}
