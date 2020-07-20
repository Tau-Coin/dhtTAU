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

public class StateDBTrack implements StateDB {

    private static final Logger logger = LoggerFactory.getLogger("repository");

    byte[] chainID;

    Map<ByteArrayWrapper, AccountState> cacheAccounts = new HashMap<>();

    StateDB stateDB;

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

    }

    /**
     * Close database.
     */
    @Override
    public void close() {

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
        stateDB.updateAccounts(chainID, cacheAccounts);
        cacheAccounts.clear();
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

    }

    /**
     * get all followed chains
     *
     * @return
     * @throws Exception
     */
    @Override
    public Set<byte[]> getAllFollowedChains() throws Exception {
        return null;
    }

    /**
     * unfollow a chain
     *
     * @param chainID
     * @throws Exception
     */
    @Override
    public void unfollowChain(byte[] chainID) throws Exception {

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
        return new byte[0];
    }

    /**
     * delete best block hash
     *
     * @param chainID
     * @throws Exception
     */
    @Override
    public void deleteBestBlockHash(byte[] chainID) throws Exception {

    }

    /**
     * set mutable range
     *
     * @param chainID
     * @param number
     * @throws Exception
     */
    @Override
    public void setMutableRange(byte[] chainID, int number) throws Exception {

    }

    /**
     * get mutable range
     *
     * @param chainID
     * @return
     * @throws Exception
     */
    @Override
    public int getMutableRange(byte[] chainID) throws Exception {
        return 0;
    }

    /**
     * delete mutable range
     *
     * @param chainID
     * @throws Exception
     */
    @Override
    public void deleteMutableRange(byte[] chainID) throws Exception {

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
        return null;
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

    }

    /**
     * delete all peers of a chain
     *
     * @param chainID
     * @throws Exception
     */
    @Override
    public void deleteAllPeers(byte[] chainID) throws Exception {

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
        return null;
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

    }

    /**
     * set immutable point block hash
     *
     * @param chainID
     * @param hash
     * @throws Exception
     */
    @Override
    public void setImmutablePointBlockHash(byte[] chainID, byte[] hash) throws Exception {

    }

    /**
     * get immutable point block hash
     *
     * @param chainID
     * @return
     * @throws Exception
     */
    @Override
    public byte[] getImmutablePointBlockHash(byte[] chainID) throws Exception {
        return new byte[0];
    }

    /**
     * delete immutable point block hash
     *
     * @param chainID
     * @throws Exception
     */
    @Override
    public void deleteImmutablePointBlockHash(byte[] chainID) throws Exception {

    }

    /**
     * set votes counting point block hash
     *
     * @param chainID
     * @param hash
     * @throws Exception
     */
    @Override
    public void setVotesCountingPointBlockHash(byte[] chainID, byte[] hash) throws Exception {

    }

    /**
     * get votes counting point block hash
     *
     * @param chainID
     * @return
     * @throws Exception
     */
    @Override
    public byte[] getVotesCountingPointBlockHash(byte[] chainID) throws Exception {
        return new byte[0];
    }

    /**
     * delete votes counting point block hash
     *
     * @param chainID
     * @throws Exception
     */
    @Override
    public void deleteVotesCountingPointBlockHash(byte[] chainID) throws Exception {

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
        return null;
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
        return null;
    }

    /**
     * delete a account
     *
     * @param chainID
     * @param pubKey
     * @throws Exception
     */
    @Override
    public void deleteAccount(byte[] chainID, byte[] pubKey) throws Exception {

    }
}
