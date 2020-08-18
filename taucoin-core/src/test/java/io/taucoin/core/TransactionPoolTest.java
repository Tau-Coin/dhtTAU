//package io.taucoin.core;
//
//import io.taucoin.db.StateDB;
//import io.taucoin.types.Transaction;
//import io.taucoin.types.TxData;
//import io.taucoin.util.ByteArrayWrapper;
//import io.taucoin.util.ByteUtil;
//import org.junit.Assert;
//import org.junit.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.math.BigInteger;
//import java.util.Map;
//import java.util.Set;
//
//public class TransactionPoolTest {
//
//    private static final Logger logger = LoggerFactory.getLogger("test");
//
//    private static final String txdata = "f889809a736861726520796f7572206661766f7572697465206d75736963b86b687474703a2f2f7777772e6b75676f752e636f6d2f736f6e672f366e6e796162622e68746d6c3f66726f6d62616964753f66726f6d626169647523686173683d423036413434304234433231453239423334414338303338453330384530324626616c62756d5f69643d30";
//    private static final byte[] chainid = "TAUcoin#300#3938383036366633393364383365393338373434".getBytes();
//    private static final byte[] sender1 = ByteUtil.toByte("c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb231");
//    private static final byte[] sender2 = ByteUtil.toByte("c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb232");
//    private static final byte[] sender3 = ByteUtil.toByte("c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb233");
//    private static final byte[] sender4 = ByteUtil.toByte("c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb234");
//    private static final byte[] sender5 = ByteUtil.toByte("c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb235");
//    private static final byte[] sender6 = ByteUtil.toByte("c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb236");
//    private static final byte[] sender7 = ByteUtil.toByte("c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb237");
//    private static final byte[] signature = ByteUtil.toByte("281f3c2fe309683c74762f965f38bd8f8910d8dbeca1da904d6821e8101075776243379a4efdfdc8c10ae34be767a825f770e6a62b5430c030f179b74057e747");
//    private static final String transaction = "f9013b01b4544155636f696e233330302333393338333833303336333636363333333933333634333833333635333933333338333733343334880000000008f0d18084ffffffc4a0c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c880000000000000000f889809a736861726520796f7572206661766f7572697465206d75736963b86b687474703a2f2f7777772e6b75676f752e636f6d2f736f6e672f366e6e796162622e68746d6c3f66726f6d62616964753f66726f6d626169647523686173683d423036413434304234433231453239423334414338303338453330384530324626616c62756d5f69643d30b840281f3c2fe309683c74762f965f38bd8f8910d8dbeca1da904d6821e8101075776243379a4efdfdc8c10ae34be767a825f770e6a62b5430c030f179b74057e747";
//    private static final byte version = 1;
//    private static final short expire = 30;
//
//    @Test
//    public void AddRemoteTest() {
//        StateDB stateDB = new MockStateDB();
//        TransactionPoolImpl pool = new TransactionPoolImpl(new byte[0], new byte[0], stateDB);
//        TxData txData = new TxData(ByteUtil.toByte(txdata));
//        Transaction tx = new Transaction(version,chainid,150000000,300, sender1,1,txData,signature);
//        pool.addRemote(tx);
//        tx = new Transaction(version,chainid,150000000,200, sender2,1,txData,signature);
//        pool.addRemote(tx);
//        tx = new Transaction(version,chainid,150000000,700, sender3,1,txData,signature);
//        pool.addRemote(tx);
//        tx = new Transaction(version,chainid,150000000,600, sender4,1,txData,signature);
//        pool.addRemote(tx);
//        tx = new Transaction(version,chainid,150000000,400, sender5,1,txData,signature);
//        pool.addRemote(tx);
//        tx = new Transaction(version,chainid,150000000,100, sender6,1,txData,signature);
//        pool.addRemote(tx);
//        tx = new Transaction(version,chainid,150000000,500, sender7,1,txData,signature);
//        pool.addRemote(tx);
//
//        logger.info("{}", pool.remotes);
//        MemoryPoolEntry entry = pool.remotes.peek();
//        Assert.assertEquals(entry.fee, 700);
//    }
//
//    @Test
//    public void AddLocalTest() {
//        StateDB stateDB = new MockStateDB();
//        TransactionPoolImpl pool = new TransactionPoolImpl(new byte[0], new byte[0], stateDB);
//        TxData txData = new TxData(ByteUtil.toByte(txdata));
//        Transaction tx = new Transaction(version,chainid,150000000,300, sender1,1,txData,signature);
//        pool.addLocal(tx);
//        tx = new Transaction(version,chainid,150000000,300, sender1,2,txData,signature);
//        pool.addLocal(tx);
//        tx = new Transaction(version,chainid,150000000,300, sender1,3,txData,signature);
//        pool.addLocal(tx);
//        tx = new Transaction(version,chainid,150000000,300, sender1,4,txData,signature);
//        pool.addLocal(tx);
//        tx = new Transaction(version,chainid,150000000,300, sender1,5,txData,signature);
//        pool.addLocal(tx);
//        tx = new Transaction(version,chainid,150000000,300, sender1,6,txData,signature);
//        pool.addLocal(tx);
//        tx = new Transaction(version,chainid,150000000,300, sender1,7,txData,signature);
//        pool.addLocal(tx);
//
//        logger.info("{}", pool.locals);
//        LocalTxEntry entry = pool.locals.peek();
//        Assert.assertEquals(entry.nonce, 1);
//    }
//
//    @Test
//    public void SlimDownPoolTest() {
//        StateDB stateDB = new MockStateDB();
//        TransactionPoolImpl pool = new TransactionPoolImpl(new byte[0], new byte[0], stateDB);
//        TxData txData = new TxData(ByteUtil.toByte(txdata));
//        Transaction tx = new Transaction(version,chainid,150000000,300, sender1,1,txData,signature);
//        pool.addRemote(tx);
//        tx = new Transaction(version,chainid,150000000,200, sender2,1,txData,signature);
//        pool.addRemote(tx);
//        tx = new Transaction(version,chainid,150000000,700, sender3,1,txData,signature);
//        pool.addRemote(tx);
//        tx = new Transaction(version,chainid,150000000,600, sender4,1,txData,signature);
//        pool.addRemote(tx);
//        tx = new Transaction(version,chainid,150000000,400, sender5,1,txData,signature);
//        pool.addRemote(tx);
//        tx = new Transaction(version,chainid,150000000,100, sender6,1,txData,signature);
//        pool.addRemote(tx);
//        tx = new Transaction(version,chainid,150000000,500, sender7,1,txData,signature);
//        pool.addRemote(tx);
//        logger.info("{}", pool.remotes);
//
//        pool.slimDownPool();
//        Assert.assertEquals(pool.remotes.size(), 3);
//        logger.info("{}", pool.remotes);
//    }
//
//    class MockStateDB implements StateDB {
//        /**
//         * Open database.
//         *
//         * @param path database path which can be accessed
//         * @throws Exception
//         */
//        @Override
//        public void open(String path) throws Exception {
//
//        }
//
//        /**
//         * Close database.
//         */
//        @Override
//        public void close() {
//
//        }
//
//        /**
//         * Save a snapshot and start tracking future changes
//         *
//         * @param chainID chainID
//         * @return the tracker repository
//         */
//        @Override
//        public StateDB startTracking(byte[] chainID) {
//            return null;
//        }
//
//        /**
//         * Store all the temporary changes made
//         * to the repository in the actual database
//         */
//        @Override
//        public void commit() throws Exception {
//
//        }
//
//        /**
//         * Undo all the changes made so far
//         * to a snapshot of the repository
//         */
//        @Override
//        public void rollback() {
//
//        }
//
//        /**
//         * follow a chain
//         *
//         * @param chainID
//         * @throws Exception
//         */
//        @Override
//        public void followChain(byte[] chainID) throws Exception {
//
//        }
//
//        /**
//         * if follow a chain
//         *
//         * @param chainID chain ID
//         * @return true:followed, false: not followed
//         * @throws Exception
//         */
//        @Override
//        public boolean isChainFollowed(byte[] chainID) throws Exception {
//            return false;
//        }
//
//        /**
//         * get all followed chains
//         *
//         * @return
//         * @throws Exception
//         */
//        @Override
//        public Set<byte[]> getAllFollowedChains() throws Exception {
//            return null;
//        }
//
//        /**
//         * unfollow a chain
//         *
//         * @param chainID
//         * @throws Exception
//         */
//        @Override
//        public void unfollowChain(byte[] chainID) throws Exception {
//
//        }
//
//        /**
//         * set best block hash
//         *
//         * @param chainID
//         * @param hash
//         * @throws Exception
//         */
//        @Override
//        public void setBestBlockHash(byte[] chainID, byte[] hash) throws Exception {
//
//        }
//
//        /**
//         * get best block hash
//         *
//         * @param chainID
//         * @return
//         * @throws Exception
//         */
//        @Override
//        public byte[] getBestBlockHash(byte[] chainID) throws Exception {
//            return new byte[0];
//        }
//
////        /**
////         * delete best block hash
////         *
////         * @param chainID
////         * @throws Exception
////         */
////        @Override
////        public void deleteBestBlockHash(byte[] chainID) throws Exception {
////
////        }
//
//        /**
//         * set current chain synced block hash
//         *
//         * @param chainID
//         * @param hash
//         * @throws Exception
//         */
//        @Override
//        public void setSyncBlockHash(byte[] chainID, byte[] hash) throws Exception {
//
//        }
//
//        /**
//         * get current chain synced block hash
//         *
//         * @param chainID
//         * @return
//         * @throws Exception
//         */
//        @Override
//        public byte[] getSyncBlockHash(byte[] chainID) throws Exception {
//            return new byte[0];
//        }
//
////        /**
////         * delete current chain synced block hash
////         *
////         * @param chainID
////         * @throws Exception
////         */
////        @Override
////        public void deleteSyncBlockHash(byte[] chainID) throws Exception {
////
////        }
//
//        /**
//         * set mutable range
//         *
//         * @param chainID
//         * @param number
//         * @throws Exception
//         */
//        @Override
//        public void setMutableRange(byte[] chainID, int number) throws Exception {
//
//        }
//
//        /**
//         * get mutable range
//         *
//         * @param chainID
//         * @return
//         * @throws Exception
//         */
//        @Override
//        public int getMutableRange(byte[] chainID) throws Exception {
//            return 0;
//        }
//
//        /**
//         * delete mutable range
//         *
//         * @param chainID
//         * @throws Exception
//         */
//        @Override
//        public void deleteMutableRange(byte[] chainID) throws Exception {
//
//        }
//
//        /**
//         * add a new peer
//         *
//         * @param chainID
//         * @param pubkey
//         * @throws Exception
//         */
//        @Override
//        public void addPeer(byte[] chainID, byte[] pubkey) throws Exception {
//
//        }
//
//        /**
//         * get all peers of a chain
//         *
//         * @param chainID
//         * @return
//         * @throws Exception
//         */
//        @Override
//        public Set<byte[]> getPeers(byte[] chainID) throws Exception {
//            return null;
//        }
//
//        /**
//         * delete a peer
//         *
//         * @param chainID
//         * @param pubkey
//         * @throws Exception
//         */
//        @Override
//        public void deletePeer(byte[] chainID, byte[] pubkey) throws Exception {
//
//        }
//
//        /**
//         * delete all peers of a chain
//         *
//         * @param chainID
//         * @throws Exception
//         */
//        @Override
//        public void deleteAllPeers(byte[] chainID) throws Exception {
//
//        }
//
//        /**
//         * get self transaction pool
//         *
//         * @param chainID
//         * @param pubKey
//         * @return
//         * @throws Exception
//         */
//        @Override
//        public Set<Transaction> getSelfTxPool(byte[] chainID, byte[] pubKey) throws Exception {
//            return null;
//        }
//
//        /**
//         * put transaction into pool
//         *
//         * @param chainID
//         * @param tx
//         * @throws Exception
//         */
//        @Override
//        public void putTxIntoSelfTxPool(byte[] chainID, Transaction tx) throws Exception {
//
//        }
//
//        /**
//         * delete self transaction pool
//         *
//         * @param chainID
//         * @param pubKey
//         * @throws Exception
//         */
//        @Override
//        public void deleteSelfTxPool(byte[] chainID, byte[] pubKey) throws Exception {
//
//        }
//
//        /**
//         * set immutable point block hash
//         *
//         * @param chainID
//         * @param hash
//         * @throws Exception
//         */
//        @Override
//        public void setImmutablePointBlockHash(byte[] chainID, byte[] hash) throws Exception {
//
//        }
//
//        /**
//         * get immutable point block hash
//         *
//         * @param chainID
//         * @return
//         * @throws Exception
//         */
//        @Override
//        public byte[] getImmutablePointBlockHash(byte[] chainID) throws Exception {
//            return new byte[0];
//        }
//
//        /**
//         * delete immutable point block hash
//         *
//         * @param chainID
//         * @throws Exception
//         */
//        @Override
//        public void deleteImmutablePointBlockHash(byte[] chainID) throws Exception {
//
//        }
//
//        /**
//         * set votes counting point block hash
//         *
//         * @param chainID
//         * @param hash
//         * @throws Exception
//         */
//        @Override
//        public void setVotesCountingPointBlockHash(byte[] chainID, byte[] hash) throws Exception {
//
//        }
//
//        /**
//         * get votes counting point block hash
//         *
//         * @param chainID
//         * @return
//         * @throws Exception
//         */
//        @Override
//        public byte[] getVotesCountingPointBlockHash(byte[] chainID) throws Exception {
//            return new byte[0];
//        }
//
//        /**
//         * delete votes counting point block hash
//         *
//         * @param chainID
//         * @throws Exception
//         */
//        @Override
//        public void deleteVotesCountingPointBlockHash(byte[] chainID) throws Exception {
//
//        }
//
//        /**
//         * update accounts state
//         *
//         * @param chainID
//         * @param accountStateMap
//         * @throws Exception
//         */
//        @Override
//        public void updateAccounts(byte[] chainID, Map<ByteArrayWrapper, AccountState> accountStateMap) throws Exception {
//
//        }
//
//        /**
//         * update account state
//         *
//         * @param chainID
//         * @param pubKey
//         * @param account
//         * @throws Exception
//         */
//        @Override
//        public void updateAccount(byte[] chainID, byte[] pubKey, AccountState account) throws Exception {
//
//        }
//
//        /**
//         * get a account state
//         *
//         * @param chainID
//         * @param pubKey
//         * @return
//         * @throws Exception
//         */
//        @Override
//        public AccountState getAccount(byte[] chainID, byte[] pubKey) throws Exception {
//            return null;
//        }
//
//        /**
//         * get nonce by pubKey
//         *
//         * @param chainID
//         * @param pubKey
//         * @return
//         * @throws Exception
//         */
//        @Override
//        public BigInteger getNonce(byte[] chainID, byte[] pubKey) throws Exception {
//            return null;
//        }
//
//
//        /**
//         * Write batch into the database.
//         *
//         * @param rows key-value batch
//         * @throws Exception
//         */
//        @Override
//        public void updateBatch(Map<byte[], byte[]> rows) throws Exception {
//
//        }
//
//        /**
//         * clear all state data
//         *
//         * @param chainID
//         * @throws Exception
//         */
//        @Override
//        public void clearAllState(byte[] chainID) throws Exception {
//
//        }
//    }
//}
//
