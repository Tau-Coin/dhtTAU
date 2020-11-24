package io.taucoin.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.taucoin.core.AccountState;
import io.taucoin.types.Transaction;
import io.taucoin.types.TransactionFactory;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.ByteUtil;

import java.math.BigInteger;
import java.util.*;

// 公钥发布的mutable message也会放在stateDB
public class StateDBImpl implements StateDB {

    private static final Logger logger = LoggerFactory.getLogger("StateDBImpl");

    private final KeyValueDataBase db;

    public StateDBImpl(KeyValueDataBase db) {
        this.db = db;
    }

    /**
     * open db
     * @param path database path which can be accessed
     * @throws DBException database exception
     */
    public void open(String path) throws DBException {
        try {
            db.open(path);
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * close db
     */
    public void close() {
        db.close();
    }

    /**
     * Save a snapshot and start tracking future changes
     *
     * @param chainID  chain ID
     * @return the tracker repository
     */
    @Override
    public StateDB startTracking(byte[] chainID) {
        return new StateDBTrack(this, chainID);
    }

    /**
     * Store all the temporary changes made
     * to the repository in the actual database
     */
    @Override
    public void commit() {
        throw new UnsupportedOperationException();
    }

    /**
     * Undo all the changes made so far
     * to a snapshot of the repository
     */
    @Override
    public void rollback() {
        throw new UnsupportedOperationException();
    }

    /**
     * follow a chain
     * @param chainID chain ID
     * @throws DBException database exception
     */
    @Override
    public void followChain(byte[] chainID) throws DBException {
        try {
            db.put(PrefixKey.chainKey(chainID), new byte[1]);
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
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
        try {
            return  null != db.get(PrefixKey.chainKey(chainID));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * get all followed chains
     * @return chain ID set
     * @throws DBException database exception
     */
    @Override
    public Set<byte[]> getAllFollowedChains() throws DBException {
        Set<byte[]> ret = new HashSet<>();
        Set<byte[]> set;
        try {
            set = db.retrieveKeysWithPrefix(PrefixKey.CHAIN);
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }

        if (null != set) {
            for(byte[] chainID: set) {
                ret.add(Arrays.copyOfRange(chainID, PrefixKey.CHAIN.length, chainID.length));
            }
        }

        return ret;
    }

    /**
     * unfollow a chain
     * @param chainID chain ID
     * @throws DBException database exception
     */
    @Override
    public void unfollowChain(byte[] chainID) throws DBException {
        try {
            db.delete(PrefixKey.chainKey(chainID));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * set best block hash
     * @param chainID chain ID
     * @param hash best block hash
     * @throws DBException database exception
     */
    @Override
    public void setBestBlockHash(byte[] chainID, byte[] hash) throws DBException {
        try {
            db.put(PrefixKey.bestBlockHashKey(chainID), hash);
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * get best block hash
     * @param chainID chain ID
     * @return best block hash, null otherwise
     * @throws DBException database exception
     */
    @Override
    public byte[] getBestBlockHash(byte[] chainID) throws DBException {
        try {
            return db.get(PrefixKey.bestBlockHashKey(chainID));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

//    /**
//     * delete best block hash
//     * @param chainID
//     * @throws DBException database exception
//     */
//    @Override
//    public void deleteBestBlockHash(byte[] chainID) throws DBException {
//        db.delete(PrefixKey.bestBlockHashKey(chainID));
//    }

    /**
     * set current chain synced block hash
     *
     * @param chainID chain ID
     * @param hash block hash
     * @throws DBException database exception
     */
    @Override
    public void setSyncBlockHash(byte[] chainID, byte[] hash) throws DBException {
        try {
            db.put(PrefixKey.syncBlockHashKey(chainID), hash);
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * get current chain synced block hash
     *
     * @param chainID chain ID
     * @return sync block hash or null
     * @throws DBException database exception
     */
    @Override
    public byte[] getSyncBlockHash(byte[] chainID) throws DBException {
        try {
            return db.get(PrefixKey.syncBlockHashKey(chainID));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

//    /**
//     * delete current chain synced block hash
//     *
//     * @param chainID
//     * @throws DBException database exception
//     */
//    @Override
//    public void deleteSyncBlockHash(byte[] chainID) throws DBException {
//        db.delete(PrefixKey.syncBlockHashKey(chainID));
//    }

    /**
     * set mutable range
     * @param chainID chain ID
     * @param number mutable block number
     * @throws DBException database exception
     */
    @Override
    public void setMutableRange(byte[] chainID, int number) throws DBException {
        try {
            db.put(PrefixKey.mutableRangeKey(chainID), ByteUtil.intToBytes(number));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * get mutable range
     * @param chainID chain ID
     * @return mutable block number
     * @throws DBException database exception
     */
    @Override
    public int getMutableRange(byte[] chainID) throws DBException {
        byte[] rangeByte;
        try {
            rangeByte = db.get(PrefixKey.mutableRangeKey(chainID));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }

        return ByteUtil.byteArrayToInt(rangeByte);
    }

    /**
     * delete mutable range
     * @param chainID chain ID
     * @throws DBException database exception
     */
    @Override
    public void deleteMutableRange(byte[] chainID) throws DBException {
        try {
            db.delete(PrefixKey.mutableRangeKey(chainID));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * add a new peer
     * @param chainID chain ID
     * @param pubkey public key
     * @throws DBException database exception
     */
    @Override
    public void addPeer(byte[] chainID, byte[] pubkey) throws DBException {
        try {
            db.put(PrefixKey.peerKey(chainID, pubkey), new byte[1]);
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * get all peers of a chain
     * @param chainID chain ID
     * @return peer set
     * @throws DBException database exception
     */
    @Override
    public Set<byte[]> getPeers(byte[] chainID) throws DBException {
        Set<byte[]> ret = new HashSet<>();
        byte[] prefix = PrefixKey.peerKeyPrefix(chainID);

        Set<byte[]> set;
        try {
            set = db.retrieveKeysWithPrefix(prefix);
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }

        if (null != set) {
            for(byte[] peer: set) {
                ret.add(Arrays.copyOfRange(peer, prefix.length, peer.length));
            }
        }

        return ret;
    }

    /**
     * delete a peer
     * @param chainID chain ID
     * @param pubkey public key
     * @throws DBException database exception
     */
    @Override
    public void deletePeer(byte[] chainID, byte[] pubkey) throws DBException {
        try {
            db.delete(PrefixKey.peerKey(chainID, pubkey));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * delete all peers of a chain
     * @param chainID chain ID
     * @throws DBException database exception
     */
    @Override
    public void deleteAllPeers(byte[] chainID) throws DBException {
        try {
            db.removeWithKeyPrefix(PrefixKey.peerKeyPrefix(chainID));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * get self transaction pool
     * @param chainID chain ID
     * @param pubKey public key
     * @return tx set
     * @throws DBException database exception
     */
    @Override
    public Set<Transaction> getSelfTxPool(byte[] chainID, byte[] pubKey) throws DBException {
        Set<Transaction> txs = new HashSet<>();

        try {
            Set<byte[]> keys = db.retrieveKeysWithPrefix(PrefixKey.txPoolAllTxPrefix(chainID, pubKey));

            if (null != keys) {
                for (byte[] key : keys) {
                    byte[] txEncode = db.get(key);
                    txs.add(TransactionFactory.parseTransaction(txEncode));
                }
                return txs;
            }
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }

        return txs;
    }

    /**
     * put transaction into pool
     * @param chainID chain ID
     * @param tx tx to put
     * @throws DBException database exception
     */
    @Override
    public void putTxIntoSelfTxPool(byte[] chainID, Transaction tx) throws DBException {
        try {
            db.put(PrefixKey.txPoolTxKey(chainID, tx.getSenderPubkey(), tx.getNonce().longValue()), tx.getEncoded());
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * delete self transaction pool
     * @param chainID chain ID
     * @param pubKey public key
     * @throws DBException database exception
     */
    @Override
    public void deleteSelfTxPool(byte[] chainID, byte[] pubKey) throws DBException {
        try {
            db.delete(PrefixKey.txPoolAllTxPrefix(chainID, pubKey));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
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
        byte[] txEncode;
        try {
            txEncode = db.get(PrefixKey.txPoolTxKey(chainID, pubKey, nonce));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }

        if (null != txEncode) {
            return TransactionFactory.parseTransaction(txEncode);
        }

        return null;
    }

    /**
     * set immutable point block hash
     * @param chainID chain ID
     * @param hash immutable block hash
     * @throws DBException database exception
     */
    @Override
    public void setImmutablePointBlockHash(byte[] chainID, byte[] hash) throws DBException {
        try {
            db.put(PrefixKey.immutablePointBlockHashKey(chainID), hash);
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * get immutable point block hash
     * @param chainID chain ID
     * @return immutable block hash or null
     * @throws DBException database exception
     */
    @Override
    public byte[] getImmutablePointBlockHash(byte[] chainID) throws DBException {
        try {
            return db.get(PrefixKey.immutablePointBlockHashKey(chainID));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * delete immutable point block hash
     * @param chainID chain ID
     * @throws DBException database exception
     */
    @Override
    public void deleteImmutablePointBlockHash(byte[] chainID) throws DBException {
        try {
            db.delete(PrefixKey.immutablePointBlockHashKey(chainID));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * set votes counting point block hash
     * @param chainID chain ID
     * @param hash votes counting point block hash
     * @throws DBException database exception
     */
    @Override
    public void setVotesCountingPointBlockHash(byte[] chainID, byte[] hash) throws DBException {
        try {
            db.put(PrefixKey.votesCountingPointBlockHashKey(chainID), hash);
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * get votes counting point block hash
     * @param chainID chain ID
     * @return votes counting point block hash null
     * @throws DBException database exception
     */
    @Override
    public byte[] getVotesCountingPointBlockHash(byte[] chainID) throws DBException {
        try {
            return db.get(PrefixKey.votesCountingPointBlockHashKey(chainID));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * delete votes counting point block hash
     * @param chainID chain ID
     * @throws DBException database exception
     */
    @Override
    public void deleteVotesCountingPointBlockHash(byte[] chainID) throws DBException {
        try {
            db.delete(PrefixKey.votesCountingPointBlockHashKey(chainID));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * update accounts state
     * @param chainID chain ID
     * @param accountStateMap account state map
     * @throws DBException database exception
     */
    @Override
    public void updateAccounts(byte[] chainID, Map<ByteArrayWrapper, AccountState> accountStateMap) throws DBException {
        if (null != accountStateMap) {
            Map<byte[], byte[]> map = new HashMap<>(accountStateMap.size());
            for (Map.Entry<ByteArrayWrapper, AccountState> entry: accountStateMap.entrySet()) {
                map.put(PrefixKey.accountKey(chainID, entry.getKey().getData()), entry.getValue().getEncoded());
            }

            try {
                db.updateBatch(map);
            } catch (Exception e) {
                throw new DBException(e.getMessage());
            }
        }
    }

    /**
     * update account state
     *
     * @param chainID chain ID
     * @param pubKey public key
     * @param account account
     * @throws DBException database exception
     */
    @Override
    public void updateAccount(byte[] chainID, byte[] pubKey, AccountState account) throws DBException {
        if (null != account) {
            try {
                db.put(PrefixKey.accountKey(chainID, pubKey), account.getEncoded());
            } catch (Exception e) {
                throw new DBException(e.getMessage());
            }
        }
    }

    /**
     * get a account state
     * @param chainID chain ID
     * @param pubKey public key
     * @return account state or null
     * @throws DBException database exception
     */
    @Override
    public AccountState getAccount(byte[] chainID, byte[] pubKey) throws DBException {
        byte[] encode;
        try {
            encode = db.get(PrefixKey.accountKey(chainID, pubKey));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }

        if (null != encode) {
            return new AccountState(encode);
        }

        return null;
    }

    /**
     * get nonce by pubKey
     *
     * @param chainID chain ID
     * @param pubKey public key
     * @return nonce or zero
     * @throws DBException database exception
     */
    @Override
    public BigInteger getNonce(byte[] chainID, byte[] pubKey) throws DBException {
        byte[] encode;
        try {
            encode = db.get(PrefixKey.accountKey(chainID, pubKey));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }

        if (null != encode) {
            AccountState accountState = new AccountState(encode);
            return accountState.getNonce();
        }

        return BigInteger.ZERO;
    }

//    /**
//     * delete a account
//     * @param chainID
//     * @param pubKey
//     * @throws DBException database exception
//     */
//    @Override
//    public void deleteAccount(byte[] chainID, byte[] pubKey) throws DBException {
//        db.delete(PrefixKey.accountKey(chainID, pubKey));
//    }


    /**
     * Write batch into the database.
     *
     * @param rows key-value batch
     * @throws DBException database exception
     */
    @Override
    public void updateBatch(Map<byte[], byte[]> rows) throws DBException {
        if (null != rows) {
            try {
                this.db.updateBatch(rows);
            } catch (Exception e) {
                throw new DBException(e.getMessage());
            }
        }
    }

    /**
     * clear all state data
     *
     * @param chainID chain ID
     * @throws DBException database exception
     */
    @Override
    public void clearAllState(byte[] chainID) throws DBException {
        try {
            this.db.removeWithKeyPrefix(PrefixKey.accountPrefix(chainID));
            this.db.delete(PrefixKey.bestBlockHashKey(chainID));
            this.db.delete(PrefixKey.syncBlockHashKey(chainID));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

}

