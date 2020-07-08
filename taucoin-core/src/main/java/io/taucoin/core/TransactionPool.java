package io.taucoin.core;

import io.taucoin.db.Repository;
import io.taucoin.types.Block;
import io.taucoin.types.Transaction;

import java.util.List;

public interface TransactionPool {

    /**
     * init transaction pool
     */
    void init();

    /**
     * add local transaction into pool
     * @param tx
     */
    void addLocal(Transaction tx);

    /**
     * add local transactions into pool
     * @param list
     */
    void addLocals(List<Transaction> list);

    /**
     * get all local transactions
     * @return
     */
    List<Transaction> getLocals();

    /**
     * save all local transaction in db
     */
    void saveLocals();

    /**
     * get a local transaction that meet the requirement of nonce continuity
     * @return
     */
    Transaction getLocalBestTransaction();

    /**
     * return the size of local transactions
     * @return
     */
    int localSize();

    /**
     * add a transaction from the remote
     * @param tx
     */
    void addRemote(Transaction tx);

    /**
     * add transactions from the remote
     * @param list
     */
    void addRemotes(List<Transaction> list);

    /**
     * get a transaction that has the maximum fee
     * @return
     */
    Transaction getBestTransaction();

    /**
     * return the size of all remote transactions
     * @return
     */
    int remoteSize();

    /**
     * retrun the size of all transaction, including local and remote ones
     * @return
     */
    int size();

    /**
     * It should be called on each block imported as <b>BEST</b> <br>
     * Does several things:
     * <ul>
     *     <li>removes block's txs from pool</li>
     *     <li>removes outdated txs</li>
     * </ul>
     * @param block block imported into blockchain as a <b>BEST</b> one
     */
    void processBest(Block block);

    /**
     * get a transaction by txid
     * @return
     */
    Transaction getTransactionByTxid(byte[] txid);

    /**
     * get maximum fee in pool
     * @return
     */
    long getMaxFee();

    /**
     * remove transaction by hash
     * @param txid
     */
    void removeTransactionByHash(byte[] txid);

    /**
     * remove transaction by account
     * @param pubKey
     */
    void removeTransactionByAccount(byte[] pubKey);

}

