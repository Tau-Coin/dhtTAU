package io.taucoin.core;

import io.taucoin.db.Repository;
import io.taucoin.types.Block;
import io.taucoin.types.Transaction;

import java.util.List;

public class TransactionPoolImpl implements TransactionPool {
    byte[] chainID;
    Repository repository;
    int maxFee;
    TxNoncer pendingNonce;

    /**
     * init transaction pool with chainID and repository
     *
     * @param chainID
     * @param repository
     */
    @Override
    public void init(byte[] chainID, Repository repository) {
        this.chainID = chainID;
        this.repository = repository;
        this.pendingNonce = new TxNoncer(chainID, repository);
    }

    /**
     * add local transaction into pool
     *
     * @param tx
     */
    @Override
    public void addLocal(Transaction tx) {

    }

    /**
     * add local transactions into pool
     *
     * @param list
     */
    @Override
    public void addLocals(List<Transaction> list) {

    }

    /**
     * get all local transactions
     *
     * @return
     */
    @Override
    public List<Transaction> getLocals() {
        return null;
    }

    /**
     * save all local transaction in db
     */
    @Override
    public void saveLocals() {

    }

    /**
     * get a local transaction that meet the requirement of nonce continuity
     *
     * @return
     */
    @Override
    public Transaction getLocalBestTransaction() {
        return null;
    }

    /**
     * return the size of local transactions
     *
     * @return
     */
    @Override
    public int localSize() {
        return 0;
    }

    /**
     * add a transaction from the remote
     *
     * @param tx
     */
    @Override
    public void addRemote(Transaction tx) {

    }

    /**
     * add transactions from the remote
     *
     * @param list
     */
    @Override
    public void addRemotes(List<Transaction> list) {

    }

    /**
     * get a transaction that has the maximum fee
     *
     * @return
     */
    @Override
    public Transaction getBestTransaction() {
        return null;
    }

    /**
     * return the size of all remote transactions
     *
     * @return
     */
    @Override
    public int remoteSize() {
        return 0;
    }

    /**
     * retrun the size of all transaction, including local and remote ones
     *
     * @return
     */
    @Override
    public int size() {
        return 0;
    }

    /**
     * It should be called on each block imported as <b>BEST</b> <br>
     * Does several things:
     * <ul>
     * <li>removes block's txs from pool</li>
     * <li>removes outdated txs</li>
     * </ul>
     *
     * @param block block imported into blockchain as a <b>BEST</b> one
     */
    @Override
    public void processBest(Block block) {

    }

    /**
     * get a transaction by hash
     *
     * @return
     */
    @Override
    public Transaction getTransactionByHash() {
        return null;
    }

    /**
     * get maximum fee in pool
     *
     * @return
     */
    @Override
    public long getMaxFee() {
        return 0;
    }

    /**
     * remove transaction by hash
     *
     * @param txid
     */
    @Override
    public void removeTransactionByHash(byte[] txid) {

    }

    /**
     * remove transaction by account
     *
     * @param pubKey
     */
    @Override
    public void removeTransactionByAccount(byte[] pubKey) {

    }
}
