//package io.taucoin.db;
//
//import io.taucoin.types.Block;
//import io.taucoin.types.Transaction;
//import io.taucoin.util.ByteArrayWrapper;
//import io.taucoin.util.ByteUtil;
//import org.junit.Assert;
//import org.junit.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.spongycastle.util.encoders.Hex;
//
//import java.math.BigInteger;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Set;
//
//public class BlockDBTest {
//    private static final Logger logger = LoggerFactory.getLogger("test");
//
//    private static final String txdata = "f889809a736861726520796f7572206661766f7572697465206d75736963b86b687474703a2f2f7777772e6b75676f752e636f6d2f736f6e672f366e6e796162622e68746d6c3f66726f6d62616964753f66726f6d626169647523686173683d423036413434304234433231453239423334414338303338453330384530324626616c62756d5f69643d30";
//    private static final byte[] chainid = "TAUcoin#300#3938383036366633393364383365393338373434".getBytes();
//    private static final byte[] sender = ByteUtil.toByte("c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c");
//    private static final byte[] signature = ByteUtil.toByte("281f3c2fe309683c74762f965f38bd8f8910d8dbeca1da904d6821e8101075776243379a4efdfdc8c10ae34be767a825f770e6a62b5430c030f179b74057e747");
//    private static final String transaction = "f9013b01b4544155636f696e233330302333393338333833303336333636363333333933333634333833333635333933333338333733343334880000000008f0d18084ffffffc4a0c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c880000000000000000f889809a736861726520796f7572206661766f7572697465206d75736963b86b687474703a2f2f7777772e6b75676f752e636f6d2f736f6e672f366e6e796162622e68746d6c3f66726f6d62616964753f66726f6d626169647523686173683d423036413434304234433231453239423334414338303338453330384530324626616c62756d5f69643d30b840281f3c2fe309683c74762f965f38bd8f8910d8dbeca1da904d6821e8101075776243379a4efdfdc8c10ae34be767a825f770e6a62b5430c030f179b74057e747";
//    private static final byte version = 1;
//    private static final short expire = 30;
//
//    @Test
//    public void GetForkPointBlockTest() {
//        try {
//            MockDB mockDB = new MockDB();
//            BlockDB blockDB = new BlockDB(mockDB);
//
//            long time1 = 1;
//            long time2 = 11;
//            long time3 = 21;
//            TxData txData = new TxData(ByteUtil.toByte(txdata));
//            Transaction tx = new Transaction(version, chainid, 150000000, -60, sender, 0, txData, signature);
//
//            // number: 0
//            Block block0 = new Block((byte) 1, "chain1".getBytes(), 0, 0, new byte[20], new byte[2],
//                    BigInteger.ONE, BigInteger.ONE, new byte[2], tx, 1, 1, 1, 1,
//                    new byte[64], new byte[32]);
//            logger.info("number:{}, hash:{}", block0.getBlockNum(), Hex.toHexString(block0.getBlockHash()));
//            blockDB.saveBlock("chain1".getBytes(), block0, true);
//
//            // number: 1, fork point
//            Block block1 = new Block((byte) 1, "chain1".getBytes(), time1, 1, block0.getBlockHash(), new byte[2],
//                    BigInteger.ONE, BigInteger.ONE, new byte[2], tx, 1, 1, 1, 1,
//                    new byte[64], new byte[32]);
//            logger.info("number:{}, hash:{}", block1.getBlockNum(), Hex.toHexString(block1.getBlockHash()));
//            blockDB.saveBlock("chain1".getBytes(), block1, true);
//
//            // number: 2
//            Block block2 = new Block((byte) 1, "chain1".getBytes(), time2, 2, block1.getBlockHash(), new byte[2],
//                    BigInteger.ONE, BigInteger.ONE, new byte[2], tx, 1, 1, 1, 1,
//                    new byte[64], new byte[32]);
//            logger.info("number:{}, hash:{}", block2.getBlockNum(), Hex.toHexString(block2.getBlockHash()));
//            blockDB.saveBlock("chain1".getBytes(), block2, true);
//
//            Block block21 = new Block((byte) 1, "chain1".getBytes(), time2 + 1, 2, block1.getBlockHash(), new byte[2],
//                    BigInteger.ONE, BigInteger.ONE, new byte[2], tx, 1, 1, 1, 1,
//                    new byte[64], new byte[32]);
//            logger.info("number:{}, hash:{}, previous:{}", block21.getBlockNum(),
//                    Hex.toHexString(block21.getBlockHash()), Hex.toHexString(block21.getPreviousBlockHash()));
//            blockDB.saveBlock("chain1".getBytes(), block21, false);
//
//            // number: 3
//            Block block3 = new Block((byte) 1, "chain1".getBytes(), time3, 3, block2.getBlockHash(), new byte[2],
//                    BigInteger.ONE, BigInteger.ONE, new byte[2], tx, 1, 1, 1, 1,
//                    new byte[64], new byte[32]);
//            logger.info("number:{}, hash:{}", block3.getBlockNum(), Hex.toHexString(block3.getBlockHash()));
//            blockDB.saveBlock("chain1".getBytes(), block3, true);
//
//            Block block31 = new Block((byte) 1, "chain1".getBytes(), time3 + 1, 3, block21.getBlockHash(), new byte[2],
//                    BigInteger.ONE, BigInteger.ONE, new byte[2], tx, 1, 1, 1, 1,
//                    new byte[64], new byte[32]);
//            logger.info("number:{}, hash:{}, previous:{}", block31.getBlockNum(),
//                    Hex.toHexString(block31.getBlockHash()), Hex.toHexString(block31.getPreviousBlockHash()));
//            blockDB.saveBlock("chain1".getBytes(), block31, false);
//
//            // number: 4
//            Block nonChainBlock = new Block((byte) 1, "chain1".getBytes(), time3 + time1, 4, block31.getBlockHash(), new byte[2],
//                    BigInteger.ONE, BigInteger.ONE, new byte[2], tx, 1, 1, 1, 1,
//                    new byte[64], new byte[32]);
//            logger.info("non main chain number:{}, hash:{}, previous:{}", nonChainBlock.getBlockNum(),
//                    Hex.toHexString(nonChainBlock.getBlockHash()), Hex.toHexString(nonChainBlock.getPreviousBlockHash()));
//
//            Block forkPointBlock = blockDB.getForkPointBlock(block3);
//            logger.info("Fork point block, number:{}, hash:{}",
//                    forkPointBlock.getBlockNum(), Hex.toHexString(forkPointBlock.getBlockHash()));
//            Assert.assertArrayEquals(block3.getBlockHash(), forkPointBlock.getBlockHash());
//
//            forkPointBlock = blockDB.getForkPointBlock(nonChainBlock);
//            logger.info("Fork point block, number:{}, hash:{}",
//                    forkPointBlock.getBlockNum(), Hex.toHexString(forkPointBlock.getBlockHash()));
//            Assert.assertArrayEquals(block1.getBlockHash(), forkPointBlock.getBlockHash());
//
//
//        } catch (Exception e) {
//            logger.info(e.getMessage(), e);
//        }
//    }
//
//    private class MockDB implements KeyValueDataBase {
//        private Map<ByteArrayWrapper, byte[]> map = new HashMap<>();
//
//        @Override
//        public void open(String path) throws Exception {
//
//        }
//
//        @Override
//        public void close() {
//
//        }
//
//        @Override
//        public byte[] get(byte[] key) throws Exception {
//            return map.get(new ByteArrayWrapper(key));
//        }
//
//        @Override
//        public void put(byte[] key, byte[] value) throws Exception {
//            map.put(new ByteArrayWrapper(key), value);
//        }
//
//        @Override
//        public void delete(byte[] key) throws Exception {
//            map.remove(new ByteArrayWrapper(key));
//        }
//
//        @Override
//        public void updateBatch(Map<byte[], byte[]> rows) throws Exception {
//
//        }
//
//        @Override
//        public void updateBatch(Map<byte[], byte[]> writes, Set<byte[]> delKeys) throws Exception {
//
//        }
//
//        @Override
//        public Set<byte[]> retrieveKeysWithPrefix(byte[] prefix) throws Exception {
//            return null;
//        }
//
//        @Override
//        public void removeWithKeyPrefix(byte[] prefix) throws Exception {
//
//        }
//    }
//
//}
