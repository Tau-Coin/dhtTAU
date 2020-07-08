package io.taucoin.core;

import io.taucoin.db.Repository;
import io.taucoin.types.Block;
import io.taucoin.types.Transaction;
import io.taucoin.util.ByteArrayWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.*;

public class TransactionPoolImpl implements TransactionPool {
    private static final Logger logger = LoggerFactory.getLogger("TxPool");

    private byte[] chainID;
    private byte[] userPubKey;
    private Repository repository;
    private int maxFee;
    private TxNoncer pendingNonce;
    // all transaction: hash <-> transaction
    private Map<ByteArrayWrapper, Transaction> all = new HashMap<>();
    // local transaction queue
    private PriorityQueue<LocalTxEntry> locals = new PriorityQueue<>(1, new LocalTxPolicy());
    // remote transaction queue
    private PriorityQueue<MemoryPoolEntry> remotes = new PriorityQueue<>(1, new MemoryPoolPolicy());
    // remote account transaction: pubKey <-> hash
    private Map<ByteArrayWrapper, byte[]> accountTx = new HashMap<>();


    private TransactionPoolImpl() {
    }

    public TransactionPoolImpl(byte[] chainID, byte[] pubKey, Repository repository) {
        this.chainID = chainID;
        this.userPubKey = pubKey;
        this.repository = repository;
        this.pendingNonce = new TxNoncer(chainID, repository);
    }

    /**
     * init transaction pool
     */
    @Override
    public void init() {
        getSelfTxsFromDB();
    }

    /**
     * get self transaction from db
     */
    private void getSelfTxsFromDB() {
        try {
            Set<Transaction> transactionSet = this.repository.getSelfTxPool(chainID);
            if (null != transactionSet) {
                long currentNonce = this.pendingNonce.getNonce(this.userPubKey);
                for (Transaction transaction: transactionSet) {
                    // put transactions that are not on chain into pool
                    if (transaction.getNonce() > currentNonce) {
                        locals.add(LocalTxEntry.with(transaction));
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

//    /**
//     * re-init tx pool
//     */
//    public void reinit() {
//        getSelfTxsFromDB();
//    }

    /**
     * add local transaction into pool
     *
     * @param tx
     */
    @Override
    public void addLocal(Transaction tx) {
        // save to db first
        try {
            this.repository.putTxIntoSelfTxPool(chainID, tx);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        long currentNonce = this.pendingNonce.getNonce(this.userPubKey);
        if (tx.getNonce() > currentNonce) {
            // put into pool
            all.put(new ByteArrayWrapper(tx.getTxID()), tx);

            // record in local
            locals.add(LocalTxEntry.with(tx));
        } else {
            logger.info("ChainID[{}]: tx[{}] nonce is not bigger than current nonce.",
                    chainID.toString(), Hex.toHexString(tx.getTxID()));
        }
    }

    /**
     * add local transactions into pool
     *
     * @param list
     */
    @Override
    public void addLocals(List<Transaction> list) {
        if (null != list) {
            for (Transaction transaction: list) {
                addLocal(transaction);
            }
        }
    }

    /**
     * get all local transactions in pool
     *
     * @return
     */
    @Override
    public List<Transaction> getLocals() {
        List<Transaction> list = new ArrayList<>(locals.size());
        Iterator<LocalTxEntry> iterator = locals.iterator();
        while (iterator.hasNext()) {
            Transaction tx = getTransactionByTxid(iterator.next().txid);
            if (null != tx) {
                list.add(tx);
            }
        }
        return list;
    }

    /**
     * save all local transaction in db
     */
    @Override
    public void saveLocals() {
        Iterator<LocalTxEntry> iterator = locals.iterator();
        while (iterator.hasNext()) {
            Transaction tx = getTransactionByTxid(iterator.next().txid);
            if (null != tx) {
                try {
                    this.repository.putTxIntoSelfTxPool(chainID, tx);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * get a local transaction that meet the requirement of nonce continuity
     *
     * @return
     */
    @Override
    public Transaction getLocalBestTransaction() {
        LocalTxEntry localTxEntry = locals.peek();
        if (null != localTxEntry) {
            Transaction tx = getTransactionByTxid(localTxEntry.txid);
            if (null != tx) {
                long current = this.pendingNonce.getNonce(tx.getSenderPubkey());
                if (tx.getNonce() == current + 1) {
                    return tx;
                }
            }
        }
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
        if (null == tx) {
            logger.error("ChainID:[{}]-Add remote null!");
        }

        byte[] pubKey = tx.getSenderPubkey();
        if (Arrays.equals(userPubKey, pubKey)) {
            addLocal(tx);
        }

        long currentNonce = pendingNonce.getNonce(pubKey);
        if (tx.getNonce() != currentNonce + 1) {
            logger.info("ChainID:[{}]-[{}] Nonce mismatch.",
                    chainID.toString(), Hex.toHexString(tx.getTxID()));
        }

//        accountTx.get(new ByteArrayWrapper(pubKey));

        // put into pool
        all.put(new ByteArrayWrapper(tx.getTxID()), tx);
        // record in the remotes
        remotes.add(MemoryPoolEntry.with(tx));
    }

    /**
     * add transactions from the remote
     *
     * @param list
     */
    @Override
    public void addRemotes(List<Transaction> list) {
        if (null != list) {
            for (Transaction tx: list) {
                addRemote(tx);
            }
        }
    }

    /**
     * get a transaction that has the maximum fee
     *
     * @return
     */
    @Override
    public Transaction getBestTransaction() {
        // local first
        Transaction localBest = getLocalBestTransaction();
        if (null != localBest) {
            return localBest;
        }

        // get transaction that has the maximum fee
        MemoryPoolEntry entry = remotes.peek();
        if (null != entry) {
            Transaction tx = getTransactionByTxid(entry.txid);
            if (null != tx) {
                return tx;
            }
        }

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
        return all.get(new ByteArrayWrapper(txid));
    }

    /**
     * get maximum fee in pool
     *
     * @return
     */
    @Override
    public long getMaxFee() {
        MemoryPoolEntry entry = remotes.peek();
        if (null != entry) {
            Transaction tx = getTransactionByTxid(entry.txid);
            return tx.getTxFee();
        }
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
