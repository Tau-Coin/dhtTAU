package io.taucoin.core;

import io.taucoin.db.Repository;
import io.taucoin.types.Block;
import io.taucoin.types.Transaction;
import io.taucoin.util.ByteArrayWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TransactionPoolImpl implements TransactionPool {
    private static final Logger logger = LoggerFactory.getLogger("TxPool");

    private byte[] chainID;
    private Repository repository;
    private int maxFee;
    private TxNoncer pendingNonce;
    // hash <-> transaction
    private Map<ByteArrayWrapper, Transaction> all = new HashMap<>();
    // hash
    private Set<ByteArrayWrapper> locals = new HashSet<>();
    // pubKey <-> hash
    private Map<ByteArrayWrapper, byte[]> accountTx = new HashMap<>();
    // MemoryPoolEntry
    private PriorityQueue<MemoryPoolEntry> priorityQueue = new PriorityQueue<MemoryPoolEntry>(1, new MemoryPoolPolicy());

    private TransactionPoolImpl() {
    }

    public TransactionPoolImpl(byte[] chainID, Repository repository) {
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
        return locals.size();
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
        // also accountTx.size()
        return all.size() - locals.size();
    }

    /**
     * retrun the size of all transaction, including local and remote ones
     *
     * @return
     */
    @Override
    public int size() {
        return all.size();
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
     * get a transaction by txid
     *
     * @return
     */
    @Override
    public Transaction getTransactionByTxid(byte[] txid) {
//        all.get(new ByteArrayWrapper(txid));
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
