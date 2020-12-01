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

    private final byte[] chainID;

    // cache changed account only
    // key: chainID + pubKey
    // value: account state
    private final Map<ByteArrayWrapper, AccountState> cacheAccounts = new HashMap<>();

    private byte[] bestBlockHash;

    private byte[] syncBlockHash;

    private final StateDB stateDB;

    public StateDBTrack(StateDB stateDB, byte[] chainID) {
        this.stateDB = stateDB;
        this.chainID = chainID;
    }

    /**
     * Open database.
     *
     * @param path database path which can be accessed
     */
    @Override
    public void open(String path) {
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

        return new StateDBTrack(this, chainID);
    }

    /**
     * Store all the temporary changes made
     * to the repository in the actual database
     *
     * @throws DBException data base exception
     */
    @Override
    public void commit() throws DBException {
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
     * @param chainID chain ID
     * @throws DBException database exception
     */
    @Override
    public void followChain(byte[] chainID) throws DBException {
        this.stateDB.followChain(chainID);
    }

    /**
     * if a chain has been followed
     *
     * @param chainID chain ID
     * @return true if followed, false otherwise
     * @throws DBException database exception
     */
    @Override
    public boolean isChainFollowed(byte[] chainID) throws DBException {
        return this.stateDB.isChainFollowed(chainID);
    }

    /**
     * get all followed chains
     *
     * @return chain ID set
     * @throws DBException database exception
     */
    @Override
    public Set<byte[]> getAllFollowedChains() throws DBException {
        return this.stateDB.getAllFollowedChains();
    }

    /**
     * unfollow a chain
     *
     * @param chainID chain ID
     * @throws DBException database exception
     */
    @Override
    public void unfollowChain(byte[] chainID) throws DBException {
        this.stateDB.unfollowChain(chainID);
    }

    /**
     * set best block hash
     *
     * @param chainID chain ID
     * @param hash best block hash
     */
    @Override
    public void setBestBlockHash(byte[] chainID, byte[] hash) {
        this.bestBlockHash = hash;
    }

    /**
     * get best block hash
     *
     * @param chainID chain ID
     * @return best block hash, null otherwise
     * @throws DBException database exception
     */
    @Override
    public byte[] getBestBlockHash(byte[] chainID) throws DBException {
        if (null != this.bestBlockHash) {
            return this.bestBlockHash;
        } else {
            return this.stateDB.getBestBlockHash(chainID);
        }
    }

    /**
     * set current chain synced block hash
     *
     * @param chainID chain ID
     * @param hash block hash
     */
    @Override
    public void setSyncBlockHash(byte[] chainID, byte[] hash) {
        this.syncBlockHash = hash;
    }

    /**
     * get current chain synced block hash
     *
     * @param chainID chain ID
     * @return block hash
     * @throws DBException database exception
     */
    @Override
    public byte[] getSyncBlockHash(byte[] chainID) throws DBException {
        if (null != this.syncBlockHash) {
            return this.syncBlockHash;
        } else {
            return this.stateDB.getSyncBlockHash(chainID);
        }
    }

    /**
     * set mutable range
     *
     * @param chainID chain ID
     * @param number mutable block number
     */
    @Override
    public void setMutableRange(byte[] chainID, int number) {
        throw new UnsupportedOperationException();
    }

    /**
     * get mutable range
     *
     * @param chainID chain ID
     * @return mutable block number
     */
    @Override
    public int getMutableRange(byte[] chainID) {
        throw new UnsupportedOperationException();
    }

    /**
     * delete mutable range
     *
     * @param chainID chain ID
     */
    @Override
    public void deleteMutableRange(byte[] chainID) {
        throw new UnsupportedOperationException();
    }

    /**
     * add a new peer
     *
     * @param chainID chain ID
     * @param pubkey public key
     * @throws DBException database exception
     */
    @Override
    public void addPeer(byte[] chainID, byte[] pubkey) throws DBException {
        this.stateDB.addPeer(chainID, pubkey);
    }

    /**
     * get all peers of a chain
     *
     * @param chainID chain ID
     * @return peer set
     * @throws DBException database exception
     */
    @Override
    public Set<byte[]> getPeers(byte[] chainID) throws DBException {
        return this.stateDB.getPeers(chainID);
    }

    /**
     * delete a peer
     *
     * @param chainID chain ID
     * @param pubkey public key
     * @throws DBException database exception
     */
    @Override
    public void deletePeer(byte[] chainID, byte[] pubkey) throws DBException {
        this.stateDB.deletePeer(chainID, pubkey);
    }

    /**
     * delete all peers of a chain
     *
     * @param chainID chain ID
     * @throws DBException database exception
     */
    @Override
    public void deleteAllPeers(byte[] chainID) throws DBException {
        this.stateDB.deleteAllPeers(chainID);
    }

    /**
     * get self transaction pool
     *
     * @param chainID chain ID
     * @param pubKey  public key
     * @return tx set
     * @throws DBException database exception
     */
    @Override
    public Set<Transaction> getSelfTxPool(byte[] chainID, byte[] pubKey) throws DBException {
        return this.stateDB.getSelfTxPool(chainID, pubKey);
    }

    /**
     * get self transaction pool
     *
     * @param chainID chain ID
     * @param pubKey  public key
     * @param nonce   tx nonce
     * @return tx or null
     * @throws DBException database exception
     */
    @Override
    public Transaction getSelfTx(byte[] chainID, byte[] pubKey, long nonce) throws DBException {
        return this.stateDB.getSelfTx(chainID, pubKey, nonce);
    }

    /**
     * put transaction into pool
     *
     * @param chainID chain ID
     * @param tx tx to put
     * @throws DBException database exception
     */
    @Override
    public void putTxIntoSelfTxPool(byte[] chainID, Transaction tx) throws DBException {
        this.stateDB.putTxIntoSelfTxPool(chainID, tx);
    }

    /**
     * delete self transaction pool
     *
     * @param chainID chain ID
     * @param pubKey public key
     * @throws DBException database exception
     */
    @Override
    public void deleteSelfTxPool(byte[] chainID, byte[] pubKey) throws DBException {
        this.stateDB.deleteSelfTxPool(chainID, pubKey);
    }

    /**
     * set immutable point block hash
     *
     * @param chainID chain ID
     * @param hash immutable block hash
     */
    @Override
    public void setImmutablePointBlockHash(byte[] chainID, byte[] hash) {
        throw new UnsupportedOperationException();
    }

    /**
     * get immutable point block hash
     *
     * @param chainID chain ID
     * @return immutable block hash or null
     */
    @Override
    public byte[] getImmutablePointBlockHash(byte[] chainID) {
        throw new UnsupportedOperationException();
    }

    /**
     * delete immutable point block hash
     *
     * @param chainID chain ID
     */
    @Override
    public void deleteImmutablePointBlockHash(byte[] chainID) {
        throw new UnsupportedOperationException();
    }

    /**
     * set votes counting point block hash
     *
     * @param chainID chain ID
     * @param hash votes counting point block hash
     */
    @Override
    public void setVotesCountingPointBlockHash(byte[] chainID, byte[] hash) {
        throw new UnsupportedOperationException();
    }

    /**
     * get votes counting point block hash
     *
     * @param chainID chain ID
     * @return votes counting point block hash or null
     */
    @Override
    public byte[] getVotesCountingPointBlockHash(byte[] chainID) {
        throw new UnsupportedOperationException();
    }

    /**
     * delete votes counting point block hash
     *
     * @param chainID chain ID
     */
    @Override
    public void deleteVotesCountingPointBlockHash(byte[] chainID) {
        throw new UnsupportedOperationException();
    }

    /**
     * update accounts state
     *
     * @param chainID chain ID
     * @param accountStateMap account state map
     */
    @Override
    public void updateAccounts(byte[] chainID, Map<ByteArrayWrapper, AccountState> accountStateMap) {
        if (null != accountStateMap) {
            for (Map.Entry<ByteArrayWrapper, AccountState> entry: accountStateMap.entrySet()) {
                updateAccount(chainID, entry.getKey().getData(), entry.getValue());
            }
        }
    }

    /**
     * update account state
     *
     * @param chainID chain ID
     * @param pubKey public key
     * @param account account
     */
    @Override
    public void updateAccount(byte[] chainID, byte[] pubKey, AccountState account) {
        this.cacheAccounts.put(wrap(PrefixKey.accountKey(chainID, pubKey)), account);
    }

    /**
     * get a account state
     *
     * @param chainID chain ID
     * @param pubKey public key
     * @return account state or null
     * @throws DBException database exception
     */
    @Override
    public AccountState getAccount(byte[] chainID, byte[] pubKey) throws DBException {

        AccountState accountState = cacheAccounts.get(wrap(PrefixKey.accountKey(chainID, pubKey)));

        if (accountState == null) {
            return this.stateDB.getAccount(chainID, pubKey);
        }

        return accountState;
    }

    /**
     * get nonce by pubKey
     *
     * @param chainID chain ID
     * @param pubKey public key
     * @return nonce or null
     * @throws DBException database exception
     */
    @Override
    public BigInteger getNonce(byte[] chainID, byte[] pubKey) throws DBException {
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
//     * @throws DBException database exception
//     */
//    @Override
//    public void deleteAccount(byte[] chainID, byte[] pubKey) throws DBException {
//
//    }


    /**
     * Write batch into the database.
     *
     * @param rows key-value batch
     */
    @Override
    public void updateBatch(Map<byte[], byte[]> rows) {
        throw new UnsupportedOperationException();
    }

    /**
     * clear all state data
     *
     * @param chainID chain ID
     */
    @Override
    public void clearAllState(byte[] chainID) {
        throw new UnsupportedOperationException();
    }
}

