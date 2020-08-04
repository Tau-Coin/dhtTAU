package io.taucoin.db;

import io.taucoin.core.AccountState;
import io.taucoin.types.Transaction;
import io.taucoin.util.ByteArrayWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static io.taucoin.util.ByteUtil.wrap;

public class StateDBTrack implements StateDB {

    private static final Logger logger = LoggerFactory.getLogger("StateTrack");

    private byte[] chainID;

    // cache changed account only
    // key: chainID + pubKey
    // value: account state
    private Map<ByteArrayWrapper, AccountState> cacheAccounts = new HashMap<>();

    private byte[] bestBlockHash;

    private byte[] syncBlockHash;

    private StateDB stateDB;

    public StateDBTrack(StateDB stateDB, byte[] chainID) {
        this.stateDB = stateDB;
        this.chainID = chainID;
    }

    /**
     * Open database.
     *
     * @param path database path which can be accessed
     * @throws Exception
     */
    @Override
    public void open(String path) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * Close database.
     */
    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    /**
     * Save a snapshot and start tracking future changes
     *
     * @param chainID  chainID
     * @return the tracker repository
     */
    @Override
    public StateDB startTracking(byte[] chainID) {
        logger.debug("start tracking");

        StateDB stateDB = new StateDBTrack(this, chainID);

        return stateDB;
    }

    /**
     * Store all the temporary changes made
     * to the repository in the actual database
     */
    @Override
    public void commit() throws Exception {
        // update changed accounts
        Map<byte[], byte[]> rows = new HashMap<>();
        if (null != cacheAccounts) {
            for (Map.Entry<ByteArrayWrapper, AccountState> entry: cacheAccounts.entrySet()) {
                rows.put(entry.getKey().getData(), entry.getValue().getEncoded());
            }
        }

        // update best block hash
        if (null != this.bestBlockHash) {
            rows.put(PrefixKey.bestBlockHashKey(this.chainID), this.bestBlockHash);
        }

        // update synced block hash
        if (null != this.syncBlockHash) {
            rows.put(PrefixKey.syncBlockHashKey(this.chainID), this.syncBlockHash);
        }

        // commit
        this.stateDB.updateBatch(rows);

        cacheAccounts.clear();
        this.bestBlockHash = null;
        this.syncBlockHash = null;

        logger.debug("committed changes");
    }

    /**
     * Undo all the changes made so far
     * to a snapshot of the repository
     */
    @Override
    public void rollback() {
        logger.debug("rollback changes");
        cacheAccounts.clear();
    }

    /**
     * follow a chain
     *
     * @param chainID
     * @throws Exception
     */
    @Override
    public void followChain(byte[] chainID) throws Exception {
        this.stateDB.followChain(chainID);
    }

    /**
     * if follow a chain
     *
     * @param chainID chain ID
     * @return true:followed, false: not followed
     * @throws Exception
     */
    @Override
    public boolean isChainFollowed(byte[] chainID) throws Exception {
        return this.stateDB.isChainFollowed(chainID);
    }

    /**
     * get all followed chains
     *
     * @return
     * @throws Exception
     */
    @Override
    public Set<byte[]> getAllFollowedChains() throws Exception {
        return this.stateDB.getAllFollowedChains();
    }

    /**
     * unfollow a chain
     *
     * @param chainID
     * @throws Exception
     */
    @Override
    public void unfollowChain(byte[] chainID) throws Exception {
        this.stateDB.unfollowChain(chainID);
    }

    /**
     * set best block hash
     *
     * @param chainID
     * @param hash
     * @throws Exception
     */
    @Override
    public void setBestBlockHash(byte[] chainID, byte[] hash) throws Exception {
        this.bestBlockHash = hash;
    }

    /**
     * get best block hash
     *
     * @param chainID
     * @return
     * @throws Exception
     */
    @Override
    public byte[] getBestBlockHash(byte[] chainID) throws Exception {
        if (null != this.bestBlockHash) {
            return this.bestBlockHash;
        } else {
            return this.stateDB.getBestBlockHash(chainID);
        }
    }

    /**
     * delete best block hash
     *
     * @param chainID
     * @throws Exception
     */
//    @Override
//    public void deleteBestBlockHash(byte[] chainID) throws Exception {
//    }

    /**
     * set current chain synced block hash
     *
     * @param chainID
     * @param hash
     * @throws Exception
     */
    @Override
    public void setSyncBlockHash(byte[] chainID, byte[] hash) throws Exception {
        this.syncBlockHash = hash;
    }

    /**
     * get current chain synced block hash
     *
     * @param chainID
     * @return
     * @throws Exception
     */
    @Override
    public byte[] getSyncBlockHash(byte[] chainID) throws Exception {
        if (null != this.syncBlockHash) {
            return this.syncBlockHash;
        } else {
            return this.stateDB.getSyncBlockHash(chainID);
        }
    }

    /**
     * delete current chain synced block hash
     *
     * @param chainID
     * @throws Exception
     */
//    @Override
//    public void deleteSyncBlockHash(byte[] chainID) throws Exception {
//
//    }

    /**
     * set mutable range
     *
     * @param chainID
     * @param number
     * @throws Exception
     */
    @Override
    public void setMutableRange(byte[] chainID, int number) {
        throw new UnsupportedOperationException();
    }

    /**
     * get mutable range
     *
     * @param chainID
     * @return
     * @throws Exception
     */
    @Override
    public int getMutableRange(byte[] chainID) {
        throw new UnsupportedOperationException();
    }

    /**
     * delete mutable range
     *
     * @param chainID
     * @throws Exception
     */
    @Override
    public void deleteMutableRange(byte[] chainID) {
        throw new UnsupportedOperationException();
    }

    /**
     * add a new peer
     *
     * @param chainID
     * @param pubkey
     * @throws Exception
     */
    @Override
    public void addPeer(byte[] chainID, byte[] pubkey) throws Exception {
        this.stateDB.addPeer(chainID, pubkey);
    }

    /**
     * get all peers of a chain
     *
     * @param chainID
     * @return
     * @throws Exception
     */
    @Override
    public Set<byte[]> getPeers(byte[] chainID) throws Exception {
        return this.stateDB.getPeers(chainID);
    }

    /**
     * delete a peer
     *
     * @param chainID
     * @param pubkey
     * @throws Exception
     */
    @Override
    public void deletePeer(byte[] chainID, byte[] pubkey) throws Exception {
        this.stateDB.deletePeer(chainID, pubkey);
    }

    /**
     * delete all peers of a chain
     *
     * @param chainID
     * @throws Exception
     */
    @Override
    public void deleteAllPeers(byte[] chainID) throws Exception {
        this.stateDB.deleteAllPeers(chainID);
    }

    /**
     * get self transaction pool
     *
     * @param chainID
     * @param pubKey  public key
     * @return
     * @throws Exception
     */
    @Override
    public Set<Transaction> getSelfTxPool(byte[] chainID, byte[] pubKey) throws Exception {
        return this.stateDB.getSelfTxPool(chainID, pubKey);
    }

    /**
     * put transaction into pool
     *
     * @param chainID
     * @param tx
     * @throws Exception
     */
    @Override
    public void putTxIntoSelfTxPool(byte[] chainID, Transaction tx) throws Exception {
        this.stateDB.putTxIntoSelfTxPool(chainID, tx);
    }

    /**
     * delete self transaction pool
     *
     * @param chainID
     * @param pubKey
     * @throws Exception
     */
    @Override
    public void deleteSelfTxPool(byte[] chainID, byte[] pubKey) throws Exception {
        this.stateDB.deleteSelfTxPool(chainID, pubKey);
    }

    /**
     * set immutable point block hash
     *
     * @param chainID
     * @param hash
     * @throws Exception
     */
    @Override
    public void setImmutablePointBlockHash(byte[] chainID, byte[] hash) {
        throw new UnsupportedOperationException();
    }

    /**
     * get immutable point block hash
     *
     * @param chainID
     * @return
     * @throws Exception
     */
    @Override
    public byte[] getImmutablePointBlockHash(byte[] chainID) {
        throw new UnsupportedOperationException();
    }

    /**
     * delete immutable point block hash
     *
     * @param chainID
     * @throws Exception
     */
    @Override
    public void deleteImmutablePointBlockHash(byte[] chainID) {
        throw new UnsupportedOperationException();
    }

    /**
     * set votes counting point block hash
     *
     * @param chainID
     * @param hash
     * @throws Exception
     */
    @Override
    public void setVotesCountingPointBlockHash(byte[] chainID, byte[] hash) {
        throw new UnsupportedOperationException();
    }

    /**
     * get votes counting point block hash
     *
     * @param chainID
     * @return
     * @throws Exception
     */
    @Override
    public byte[] getVotesCountingPointBlockHash(byte[] chainID) {
        throw new UnsupportedOperationException();
    }

    /**
     * delete votes counting point block hash
     *
     * @param chainID
     * @throws Exception
     */
    @Override
    public void deleteVotesCountingPointBlockHash(byte[] chainID) {
        throw new UnsupportedOperationException();
    }

    /**
     * update accounts state
     *
     * @param chainID
     * @param accountStateMap
     * @throws Exception
     */
    @Override
    public void updateAccounts(byte[] chainID, Map<ByteArrayWrapper, AccountState> accountStateMap) throws Exception {
        if (null != accountStateMap) {
            for (Map.Entry<ByteArrayWrapper, AccountState> entry: accountStateMap.entrySet()) {
                updateAccount(chainID, entry.getKey().getData(), entry.getValue());
            }
        }
    }

    /**
     * update account state
     *
     * @param chainID
     * @param pubKey
     * @param account
     * @throws Exception
     */
    @Override
    public void updateAccount(byte[] chainID, byte[] pubKey, AccountState account) throws Exception {
        this.cacheAccounts.put(wrap(PrefixKey.accountKey(chainID, pubKey)), account);
    }

    /**
     * get a account state
     *
     * @param chainID
     * @param pubKey
     * @return
     * @throws Exception
     */
    @Override
    public AccountState getAccount(byte[] chainID, byte[] pubKey) throws Exception {

        AccountState accountState = cacheAccounts.get(wrap(PrefixKey.accountKey(chainID, pubKey)));

        if (accountState == null) {
            return this.stateDB.getAccount(chainID, pubKey);
        }

        return accountState;
    }

    /**
     * get nonce by pubKey
     *
     * @param chainID
     * @param pubKey
     * @return
     * @throws Exception
     */
    @Override
    public BigInteger getNonce(byte[] chainID, byte[] pubKey) throws Exception {
        AccountState accountState = cacheAccounts.get(wrap(PrefixKey.accountKey(chainID, pubKey)));

        if (accountState == null) {
            return this.stateDB.getNonce(chainID, pubKey);
        }

        return accountState.getNonce();
    }

//    /**
//     * delete a account
//     *
//     * @param chainID
//     * @param pubKey
//     * @throws Exception
//     */
//    @Override
//    public void deleteAccount(byte[] chainID, byte[] pubKey) throws Exception {
//
//    }


    /**
     * Write batch into the database.
     *
     * @param rows key-value batch
     * @throws Exception
     */
    @Override
    public void updateBatch(Map<byte[], byte[]> rows) {
        throw new UnsupportedOperationException();
    }

    /**
     * clear all state data
     *
     * @param chainID
     * @throws Exception
     */
    @Override
    public void clearAllState(byte[] chainID) throws Exception {
        throw new UnsupportedOperationException();
    }
}

