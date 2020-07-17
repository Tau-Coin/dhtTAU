package io.taucoin.db;

import io.taucoin.core.AccountState;
import io.taucoin.types.Transaction;
import io.taucoin.util.ByteArrayWrapper;

import java.math.BigInteger;
import java.util.Map;
import java.util.Set;

public interface StateDB {
    /**
     * Open database.
     *
     * @param path database path which can be accessed
     * @throws Exception
     */
    void open(String path) throws Exception;

    /**
     * Close database.
     */
    void close();

    /*************************state interface*************************/

    /**
     * Save a snapshot and start tracking future changes
     *
     * @param chainID  chainID
     * @return the tracker repository
     */
    StateDB startTracking(byte[] chainID);

    /**
     * Store all the temporary changes made
     * to the repository in the actual database
     */
    void commit() throws Exception;

    /**
     * Undo all the changes made so far
     * to a snapshot of the repository
     */
    void rollback();

    /**
     * follow a chain
     * @param chainID
     * @throws Exception
     */
    void followChain(byte[] chainID) throws Exception;

    /**
     * get all followed chains
     * @return
     * @throws Exception
     */
    Set<byte[]> getAllFollowedChains() throws Exception;

    /**
     * unfollow a chain
     * @param chainID
     * @throws Exception
     */
    void unfollowChain(byte[] chainID) throws Exception;

    // Current Block Hash

    /**
     * set best block hash
     * @param chainID
     * @param hash
     * @throws Exception
     */
    void setBestBlockHash(byte[] chainID, byte[] hash) throws Exception;

    /**
     * get best block hash
     * @param chainID
     * @return
     * @throws Exception
     */
    byte[] getBestBlockHash(byte[] chainID) throws Exception;

    /**
     * delete best block hash
     * @param chainID
     * @throws Exception
     */
    void deleteBestBlockHash(byte[] chainID) throws Exception;

    /**
     * set mutable range
     * @param chainID
     * @param number
     * @throws Exception
     */
    void setMutableRange(byte[] chainID, int number) throws Exception;

    /**
     * get mutable range
     * @param chainID
     * @return
     * @throws Exception
     */
    int getMutableRange(byte[] chainID) throws Exception;

    /**
     * delete mutable range
     * @param chainID
     * @throws Exception
     */
    void deleteMutableRange(byte[] chainID) throws Exception;

    /**
     * add a new peer
     * @param chainID
     * @param pubkey
     * @throws Exception
     */
    void addPeer(byte[] chainID, byte[] pubkey) throws Exception;

    /**
     * get all peers of a chain
     * @param chainID
     * @return
     * @throws Exception
     */
    Set<byte[]> getPeers(byte[] chainID) throws Exception;

    /**
     * delete a peer
     * @param chainID
     * @param pubkey
     * @throws Exception
     */
    void deletePeer(byte[] chainID, byte[] pubkey) throws Exception;

    /**
     * delete all peers of a chain
     * @param chainID
     * @throws Exception
     */
    void deleteAllPeers(byte[] chainID) throws Exception;

    /**
     * get self transaction pool
     * @param chainID
     * @param pubKey public key
     * @return
     * @throws Exception
     */
    Set<Transaction> getSelfTxPool(byte[] chainID, byte[] pubKey) throws Exception;

    /**
     * put transaction into pool
     * @param chainID
     * @param tx
     * @throws Exception
     */
    void putTxIntoSelfTxPool(byte[] chainID, Transaction tx) throws Exception;

    /**
     * delete self transaction pool
     * @param chainID
     * @param pubKey
     * @throws Exception
     */
    void deleteSelfTxPool(byte[] chainID, byte[] pubKey) throws Exception;

    /**
     * set immutable point block hash
     * @param chainID
     * @param hash
     * @throws Exception
     */
    void setImmutablePointBlockHash(byte[] chainID, byte[] hash) throws Exception;

    /**
     * get immutable point block hash
     * @param chainID
     * @return
     * @throws Exception
     */
    byte[] getImmutablePointBlockHash(byte[] chainID) throws Exception;

    /**
     * delete immutable point block hash
     * @param chainID
     * @throws Exception
     */
    void deleteImmutablePointBlockHash(byte[] chainID) throws Exception;

    /**
     * set votes counting point block hash
     * @param chainID
     * @param hash
     * @throws Exception
     */
    void setVotesCountingPointBlockHash(byte[] chainID, byte[] hash) throws Exception;

    /**
     * get votes counting point block hash
     * @param chainID
     * @return
     * @throws Exception
     */
    byte[] getVotesCountingPointBlockHash(byte[] chainID) throws Exception;

    /**
     * delete votes counting point block hash
     * @param chainID
     * @throws Exception
     */
    void deleteVotesCountingPointBlockHash(byte[] chainID) throws Exception;


    /*************************state interface*************************/

    /**
     * update accounts state
     * @param chainID
     * @param accountStateMap
     * @throws Exception
     */
    void updateAccounts(byte[] chainID, Map<ByteArrayWrapper, AccountState> accountStateMap) throws Exception;

    /**
     * get a account state
     * @param chainID
     * @param pubKey
     * @return
     * @throws Exception
     */
    AccountState getAccount(byte[] chainID, byte[] pubKey) throws Exception;

    /**
     * get nonce by pubKey
     * @param chainID
     * @param pubKey
     * @return
     * @throws Exception
     */
    BigInteger getNonce(byte[] chainID, byte[] pubKey) throws Exception;

    /**
     * delete a account
     * @param chainID
     * @param pubKey
     * @throws Exception
     */
    void deleteAccount(byte[] chainID, byte[] pubKey) throws Exception;
}

