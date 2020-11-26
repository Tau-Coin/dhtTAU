package io.taucoin.db;

import io.taucoin.core.AccountState;
import io.taucoin.types.Message;
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
     * @throws DBException database exception
     */
    void open(String path) throws DBException;

    /**
     * Close database.
     */
    void close();

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
    void commit() throws DBException;

    /**
     * Undo all the changes made so far
     * to a snapshot of the repository
     */
    void rollback();

    /**
     * follow a chain
     * @param chainID chain ID
     * @throws DBException database exception database exception
     */
    void followChain(byte[] chainID) throws DBException;

    /**
     * if a chain has been followed
     * @param chainID chain ID
     * @return true if followed, false otherwise
     * @throws DBException database exception database exception
     */
    boolean isChainFollowed(byte[] chainID) throws DBException;

    /**
     * get all followed chains
     * @return chain ID set
     * @throws DBException database exception database exception
     */
    Set<byte[]> getAllFollowedChains() throws DBException;

    /**
     * unfollow a chain
     * @param chainID chain ID
     * @throws DBException database exception database exception
     */
    void unfollowChain(byte[] chainID) throws DBException;

    // Current Block Hash

    /**
     * set best block hash
     * @param chainID chain ID
     * @param hash best block hash
     * @throws DBException database exception database exception
     */
    void setBestBlockHash(byte[] chainID, byte[] hash) throws DBException;

    /**
     * get best block hash
     * @param chainID chain ID
     * @return best block hash, null otherwise
     * @throws DBException database exception database exception
     */
    byte[] getBestBlockHash(byte[] chainID) throws DBException;

    /**
     * set current chain synced block hash
     * @param chainID chain ID
     * @param hash block hash
     * @throws DBException database exception database exception
     */
    void setSyncBlockHash(byte[] chainID, byte[] hash) throws DBException;

    /**
     * get current chain synced block hash
     * @param chainID chain ID
     * @return sync block hash
     * @throws DBException database exception database exception
     */
    byte[] getSyncBlockHash(byte[] chainID) throws DBException;

    /**
     * set mutable range
     * @param chainID chain ID
     * @param number mutable block number
     * @throws DBException database exception
     */
    void setMutableRange(byte[] chainID, int number) throws DBException;

    /**
     * get mutable range
     * @param chainID chain ID
     * @return mutable block number
     * @throws DBException database exception
     */
    int getMutableRange(byte[] chainID) throws DBException;

    /**
     * delete mutable range
     * @param chainID chain ID
     * @throws DBException database exception
     */
    void deleteMutableRange(byte[] chainID) throws DBException;

    /**
     * add a new peer
     * @param chainID chain ID
     * @param pubkey public key
     * @throws DBException database exception
     */
    void addPeer(byte[] chainID, byte[] pubkey) throws DBException;

    /**
     * get all peers of a chain
     * @param chainID chain ID
     * @return peer set
     * @throws DBException database exception
     */
    Set<byte[]> getPeers(byte[] chainID) throws DBException;

    /**
     * delete a peer
     * @param chainID chain ID
     * @param pubkey public key
     * @throws DBException database exception
     */
    void deletePeer(byte[] chainID, byte[] pubkey) throws DBException;

    /**
     * delete all peers of a chain
     * @param chainID chain ID
     * @throws DBException database exception
     */
    void deleteAllPeers(byte[] chainID) throws DBException;

    /**
     * get self transaction pool
     * @param chainID chain ID
     * @param pubKey public key
     * @return tx set
     * @throws DBException database exception
     */
    Set<Transaction> getSelfTxPool(byte[] chainID, byte[] pubKey) throws DBException;


    /**
     * get self transaction pool
     * @param chainID chain ID
     * @param pubKey public key
     * @param nonce tx nonce
     * @return tx or null
     * @throws DBException database exception
     */
    Transaction getSelfTx(byte[] chainID, byte[] pubKey, long nonce) throws DBException;

    /**
     * put transaction into pool
     * @param chainID chain ID
     * @param tx tx to put
     * @throws DBException database exception
     */
    void putTxIntoSelfTxPool(byte[] chainID, Transaction tx) throws DBException;

    /**
     * delete self transaction pool
     * @param chainID chain ID
     * @param pubKey public key
     * @throws DBException database exception
     */
    void deleteSelfTxPool(byte[] chainID, byte[] pubKey) throws DBException;

    /**
     * set immutable point block hash
     * @param chainID chain ID
     * @param hash immutable block hash
     * @throws DBException database exception
     */
    void setImmutablePointBlockHash(byte[] chainID, byte[] hash) throws DBException;

    /**
     * get immutable point block hash
     * @param chainID chain ID
     * @return immutable block hash or null
     * @throws DBException database exception
     */
    byte[] getImmutablePointBlockHash(byte[] chainID) throws DBException;

    /**
     * delete immutable point block hash
     * @param chainID chain ID
     * @throws DBException database exception
     */
    void deleteImmutablePointBlockHash(byte[] chainID) throws DBException;

    /**
     * set votes counting point block hash
     * @param chainID chain ID
     * @param hash votes counting point block hash
     * @throws DBException database exception
     */
    void setVotesCountingPointBlockHash(byte[] chainID, byte[] hash) throws DBException;

    /**
     * get votes counting point block hash
     * @param chainID chain ID
     * @return votes counting point block hash or null
     * @throws DBException database exception
     */
    byte[] getVotesCountingPointBlockHash(byte[] chainID) throws DBException;

    /**
     * delete votes counting point block hash
     * @param chainID chain ID
     * @throws DBException database exception
     */
    void deleteVotesCountingPointBlockHash(byte[] chainID) throws DBException;

    /**
     * update accounts state
     * @param chainID chain ID
     * @param accountStateMap account state map
     * @throws DBException database exception
     */
    void updateAccounts(byte[] chainID, Map<ByteArrayWrapper, AccountState> accountStateMap) throws DBException;

    /**
     * update account state
     * @param chainID chain ID
     * @param pubKey public key
     * @param account account
     * @throws DBException database exception
     */
    void updateAccount(byte[] chainID, byte[] pubKey, AccountState account) throws DBException;

    /**
     * get a account state
     * @param chainID chain ID
     * @param pubKey public key
     * @return account state or null
     * @throws DBException database exception
     */
    AccountState getAccount(byte[] chainID, byte[] pubKey) throws DBException;

    /**
     * get nonce by pubKey
     * @param chainID chain ID
     * @param pubKey public key
     * @return nonce or null
     * @throws DBException database exception
     */
    BigInteger getNonce(byte[] chainID, byte[] pubKey) throws DBException;

//    /**
//     * delete a account
//     * @param chainID
//     * @param pubKey
//     * @throws DBException database exception
//     */
//    void deleteAccount(byte[] chainID, byte[] pubKey) throws DBException;

    /**
     * Write batch into the database.
     *
     * @param rows key-value batch
     * @throws DBException database exception
     */
    void updateBatch(Map<byte[], byte[]> rows) throws DBException;

    /**
     * clear all state data
     * @param chainID  chain ID
     * @throws DBException database exception
     */
    void clearAllState(byte[] chainID) throws DBException;

    /**
     * add a new friend
     * @param pubkey public key
     * @throws DBException database exception
     */
    void addFriend(byte[] pubkey) throws DBException;

    /**
     * get all friends
     * @return friend set
     * @throws DBException database exception
     */
    Set<byte[]> getFriends() throws DBException;

    /**
     * set friend message root hash
     * @param pubKey public key
     * @param hash message root hash
     * @throws DBException database exception database exception
     */
    void setFriendMessageRoot(byte[] pubKey, byte[] hash) throws DBException;

    /**
     * get friend message root hash
     * @param pubKey public key
     * @return friend message root hash, null otherwise
     * @throws DBException database exception database exception
     */
    byte[] getFriendMessageRoot(byte[] pubKey) throws DBException;

    /**
     * put a message data into db
     * @param hash message hash
     * @param data message data to put
     * @throws DBException database exception database exception
     */
    void putMessage(byte[] hash, byte[] data) throws DBException;

    /**
     * get message by hash
     * @param hash message hash
     * @return message data, null otherwise
     * @throws DBException database exception database exception
     */
    byte[] getMessageByHash(byte[] hash) throws DBException;

}

