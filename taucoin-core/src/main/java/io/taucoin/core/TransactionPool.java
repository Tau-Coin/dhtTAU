package io.taucoin.core;

import io.taucoin.types.Transaction;
import io.taucoin.util.ByteArrayWrapper;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

public interface TransactionPool {

    /**
     * init transaction pool
     */
    void init();

    /**
     * re-init the pool
     */
    void reinit();

    /**
     * clear the pool
     */
    void clearPool();

    /**
     * update user public key
     * @param pubKey public key
     */
    void updatePubKey(byte[] pubKey);

    /**
     * add transaction into pool
     *
     * @param tx the tx to add into pool
     */
    void addTx(Transaction tx);

    /**
     * add local transaction into pool
     * @param tx the tx to add into local pool
     */
    void addLocal(Transaction tx);

    /**
     * add local transactions into pool
     * @param list the tx list to add into local pool
     */
    void addLocals(List<Transaction> list);

    /**
     * get all transactions in pool
     * @return tx list in pool
     */
    List<Transaction> getAllTransactions();

    /**
     * get all local transactions
     * @return local tx list
     */
    List<Transaction> getLocals();

    /**
     * save all local transaction in db
     */
    void saveLocals();

    /**
     * get a local transaction that meet the requirement of nonce continuity
     * @return the local tx that nonce match state, null otherwise
     */
    Transaction getLocalBestTransaction();

    /**
     * return the size of local transactions
     * @return local tx size
     */
    int localSize();

    /**
     * add a transaction from the remote
     * @param tx remote tx to add
     */
    void addRemote(Transaction tx);

    /**
     * add transactions from the remote
     * @param list remote tx list to add
     */
    void addRemotes(List<Transaction> list);

    /**
     * get a best transaction that can be mined
     * @return local tx that match state,
     *         or remote tx that has max fee,
     *         null otherwise
     */
    Transaction getBestTransaction();

    /**
     * return the size of all remote transactions
     * @return remote tx size
     */
    int remoteSize();

    /**
     * return the size of all transaction, including local and remote ones
     * @return all tx size in pool
     */
    int size();

//    /**
//     * It should be called on each block imported as <b>BEST</b> <br>
//     * Does several things:
//     * <ul>
//     *     <li>removes block's txs from pool</li>
//     *     <li>removes outdated txs</li>
//     * </ul>
//     * @param block block imported into blockchain as a <b>BEST</b> one
//     */
//    void processBest(Block block);

    /**
     * get a transaction by txid
     * @return transaction or null if not found
     */
    Transaction getTransactionByTxid(byte[] txid);

    /**
     * get maximum fee in pool
     * @return max tx fee in pool
     */
    BigInteger getMaxFee();

    /**
     * remove a transaction from pool
     * @param tx transaction to be removed
     */
    void removeTransactionFromPool(Transaction tx);

//    /**
//     * remove transaction by hash
//     * @param txid
//     */
//    void removeTransactionByHash(byte[] txid);
//
//    /**
//     * remove transaction by account
//     * @param pubKey
//     */
//    void removeTransactionByAccount(byte[] pubKey);

    /**
     * check if a transaction is in pool
     * @param txid tx hash to check
     * @return true if in pool, false otherwise
     */
    boolean isInPool(byte[] txid);

    /**
     * try to slim down the pool
     * when pool size is over 3 * mutable range, keep half of it
     */
    void trySlimDownPool();

    /**
     * get a peer that has max fee
     * @return peer that has max tx fee in remote, or null if no peer
     */
    byte[] getOptimalPeer();

    /**
     * re-check the legality of the corresponding account transaction
     * @param pubKey public key
     */
    void recheckAccoutTx(byte[] pubKey);

    /**
     * re-check the legality of the corresponding accounts transaction
     * @param accounts public key set
     */
    void recheckAccoutTx(Set<ByteArrayWrapper> accounts);

}

