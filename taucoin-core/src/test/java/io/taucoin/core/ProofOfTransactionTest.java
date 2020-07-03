package io.taucoin.core;

import io.taucoin.db.BlockStore;
import io.taucoin.types.Block;
import io.taucoin.types.Transaction;
import io.taucoin.types.TxData;
import io.taucoin.util.ByteUtil;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ProofOfTransactionTest {
    private static final Logger logger = LoggerFactory.getLogger("pot_test");

    private static final String txdata = "f889809a736861726520796f7572206661766f7572697465206d75736963b86b687474703a2f2f7777772e6b75676f752e636f6d2f736f6e672f366e6e796162622e68746d6c3f66726f6d62616964753f66726f6d626169647523686173683d423036413434304234433231453239423334414338303338453330384530324626616c62756d5f69643d30";
    private static final byte[] chainid = "TAUcoin#300#3938383036366633393364383365393338373434".getBytes();
    private static final byte[] sender = ByteUtil.toByte("c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c");
    private static final byte[] signature = ByteUtil.toByte("281f3c2fe309683c74762f965f38bd8f8910d8dbeca1da904d6821e8101075776243379a4efdfdc8c10ae34be767a825f770e6a62b5430c030f179b74057e747");
    private static final String transaction = "f9013b01b4544155636f696e233330302333393338333833303336333636363333333933333634333833333635333933333338333733343334880000000008f0d18084ffffffc4a0c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c880000000000000000f889809a736861726520796f7572206661766f7572697465206d75736963b86b687474703a2f2f7777772e6b75676f752e636f6d2f736f6e672f366e6e796162622e68746d6c3f66726f6d62616964753f66726f6d626169647523686173683d423036413434304234433231453239423334414338303338453330384530324626616c62756d5f69643d30b840281f3c2fe309683c74762f965f38bd8f8910d8dbeca1da904d6821e8101075776243379a4efdfdc8c10ae34be767a825f770e6a62b5430c030f179b74057e747";
    private static final byte version = 1;
    private static final short expire = 30;

    private byte[] testGenerationSignature = Hex.decode("442c29a4d18f192164006030640fb54c8b9ffd4f5750d2f6dca192dc653c52ad");
    private BigInteger testHit = new BigInteger("762657575297429274", 10);
    private BigInteger testPower = BigInteger.valueOf(117);
    private BigInteger testBaseTarget = new BigInteger("21D0369D036978",16);
    private BigInteger testLastCumulativeDifficulty = new BigInteger("1283794322", 10);
    private long testTimeInterval = 3;
    private BigInteger testTarget = new BigInteger("285528216375286800", 10);
    private byte[] testPubKey = Hex.decode("02e504e474d05950ad5032632d8f73895185a07dd34656a5b2c7b273e8d74d09cb");
    private BigInteger testDifficulty = BigInteger.valueOf(1283796260);

    private class MockDB implements BlockStore {
        private Map<String, byte[]> mapDB = new HashMap<>();

        @Override
        public void open(String path) {

        }

        @Override
        public void close() {

        }

        @Override
        public Block getBlockByHash(byte[] chainID, byte[] hash) {
            byte[] rlp = mapDB.get(Hex.toHexString(hash));
            if (null == rlp) {
                return null;
            } else {
                return new Block(rlp);
            }
        }

        @Override
        public void saveBlock(Block block, boolean isMainChain) {
            mapDB.put(Hex.toHexString(block.getBlockHash()), block.getEncoded());
        }

        @Override
        public Block getMainChainBlockByNumber(byte[] chainID, long number) throws Exception {
            return null;
        }

        @Override
        public Set<Block> getChainAllBlocks(byte[] chainID) throws Exception {
            return null;
        }

        @Override
        public void removeChain(byte[] chainID) {

        }

        @Override
        public Block getForkPointBlock(Block block) throws Exception {
            return null;
        }
    }

    private MockDB newMockDB() {
        MockDB mockDB = new MockDB();
        long time1 = 1;
        long time2 = 11;
        long time3 = 21;
        TxData txData = new TxData(ByteUtil.toByte(txdata));
        Transaction tx = new Transaction(version,chainid,150000000,-60,sender,0,txData,signature);

        Block block1 = new Block((byte)1, "chain1".getBytes(), time1, 1, new byte[2], new byte[2],
                BigInteger.ONE, BigInteger.ONE, new byte[2], tx, 1, 1, 1, 1,
                new byte[64], new byte[32]);
        logger.info("hash:{}", Hex.toHexString(block1.getBlockHash()));
        mockDB.saveBlock(block1, true);

        Block block2 = new Block((byte)1, "chain1".getBytes(), time2, 2, block1.getBlockHash(), new byte[2],
                BigInteger.ONE, BigInteger.ONE, new byte[2], tx, 1, 1, 1, 1,
                new byte[64], new byte[32]);
        logger.info("hash:{}", Hex.toHexString(block2.getBlockHash()));
        mockDB.saveBlock(block2, true);

        Block block3 = new Block((byte)1, "chain1".getBytes(), time3, 3, block2.getBlockHash(), new byte[2],
                BigInteger.ONE, BigInteger.ONE, new byte[2], tx, 1, 1, 1, 1,
                new byte[64], new byte[32]);
        logger.info("hash:{}", Hex.toHexString(block3.getBlockHash()));
        mockDB.saveBlock(block3, true);

        return mockDB;
    }

    @Test
    public void testCalculateRequiredBaseTarget() {
        int maxRatio = ProofOfTransaction.AverageCommunityChainBlockTime + ProofOfTransaction.AverageCommunityChainBlockTime*7/60;
        int minRatio = ProofOfTransaction.AverageCommunityChainBlockTime - ProofOfTransaction.AverageCommunityChainBlockTime*7/60;

        MockDB mockDB = newMockDB();
        TxData txData = new TxData(ByteUtil.toByte(txdata));
        Transaction tx = new Transaction(version,chainid,150000000,-60,sender,0,txData,signature);

        Block block4 = new Block((byte)1, "chain1".getBytes(), minRatio*3 - 5, 4, Hex.decode("07a79dec7be361161ce19954164e565f5d5fe7860de0877a02381a2480f18579"), new byte[2],
                new BigInteger("21D0369D036978", 16), BigInteger.ONE, new byte[2], tx, 1, 1, 1, 1,
                new byte[64], new byte[32]);
        logger.info("hash:{}", Hex.toHexString(block4.getBlockHash()));
        logger.info("prehash:{}", Hex.toHexString(block4.getPreviousBlockHash()));

        ProofOfTransaction pot = new ProofOfTransaction(ProofOfTransaction.AverageCommunityChainBlockTime);
        BigInteger baseTarget = pot.calculateRequiredBaseTarget(block4, mockDB);

        Assert.assertEquals(baseTarget, new BigInteger("8806959207308847", 10));

        Block block41 = new Block((byte)1, "chain1".getBytes(), minRatio*3 + 5, 4, Hex.decode("07a79dec7be361161ce19954164e565f5d5fe7860de0877a02381a2480f18579"), new byte[2],
                new BigInteger("21D0369D036978", 16), BigInteger.ONE, new byte[2], tx, 1, 1, 1, 1,
                new byte[64], new byte[32]);

        BigInteger baseTarget41 = pot.calculateRequiredBaseTarget(block41, mockDB);
        Assert.assertEquals(baseTarget41, new BigInteger("8827263436028867", 10));

        Block block42 = new Block((byte)1, "chain1".getBytes(), maxRatio*3 - 5, 4, Hex.decode("07a79dec7be361161ce19954164e565f5d5fe7860de0877a02381a2480f18579"), new byte[2],
                new BigInteger("21D0369D036978", 16), BigInteger.ONE, new byte[2], tx, 1, 1, 1, 1,
                new byte[64], new byte[32]);

        BigInteger baseTarget42 = pot.calculateRequiredBaseTarget(block42, mockDB);
        Assert.assertEquals(baseTarget42, new BigInteger("10564544005885611", 10));

        Block block43 = new Block((byte)1, "chain1".getBytes(), maxRatio*3 + 5, 4, Hex.decode("07a79dec7be361161ce19954164e565f5d5fe7860de0877a02381a2480f18579"), new byte[2],
                new BigInteger("21D0369D036978", 16), BigInteger.ONE, new byte[2], tx, 1, 1, 1, 1,
                new byte[64], new byte[32]);

        BigInteger baseTarget43 = pot.calculateRequiredBaseTarget(block43, mockDB);
        Assert.assertEquals(baseTarget43, new BigInteger("10627994720635675", 10));
    }

    @Test
    public void testCalculateGenerationSignature() {
        ProofOfTransaction pot = new ProofOfTransaction(ProofOfTransaction.AverageCommunityChainBlockTime);
        byte[] genSig = pot.calculateGenerationSignature(testGenerationSignature, testPubKey);
        Assert.assertArrayEquals(genSig, Hex.decode("d2803778544375ff7f4eb44644c87a0d5dcf5268b80d0ab8e74ac9fb23dd73bb"));
    }

    @Test
    public void testCalculateMinerTargetValue() {
        ProofOfTransaction pot = new ProofOfTransaction(ProofOfTransaction.AverageCommunityChainBlockTime);
        BigInteger target = pot.calculateMinerTargetValue(testBaseTarget, testPower, testTimeInterval);
        Assert.assertEquals(target, testTarget);
    }

    @Test
    public void testCalculateCumulativeDifficulty() {
        ProofOfTransaction pot = new ProofOfTransaction(ProofOfTransaction.AverageCommunityChainBlockTime);
        BigInteger difficulty = pot.calculateCumulativeDifficulty(testLastCumulativeDifficulty, testBaseTarget);
        Assert.assertEquals(difficulty, testDifficulty);
    }

    @Test
    public void testCalculateRandomHit() {
        ProofOfTransaction pot = new ProofOfTransaction(ProofOfTransaction.AverageCommunityChainBlockTime);
        BigInteger hit = pot.calculateRandomHit(testGenerationSignature);
        Assert.assertEquals(hit, testHit);
    }

    @Test
    public void testCalculateMiningTimeInterval() {
        ProofOfTransaction pot = new ProofOfTransaction(ProofOfTransaction.AverageCommunityChainBlockTime);
        long timeInterval = pot.calculateMiningTimeInterval(testHit, testBaseTarget, testPower);
        Assert.assertEquals(timeInterval, ProofOfTransaction.AverageCommunityChainBlockTime / 5);

        timeInterval = pot.calculateMiningTimeInterval(testHit, BigInteger.ONE, BigInteger.ONE);
        Assert.assertEquals(timeInterval, ProofOfTransaction.AverageCommunityChainBlockTime * 9 / 5);

        timeInterval = pot.calculateMiningTimeInterval(testHit, new BigInteger("99999999999"), BigInteger.valueOf(999999999));
        Assert.assertEquals(timeInterval, 242);
    }

}

