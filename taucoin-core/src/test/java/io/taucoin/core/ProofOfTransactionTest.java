package io.taucoin.core;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

public class ProofOfTransactionTest {
    private static final Logger logger = LoggerFactory.getLogger("pot_test");

    private byte[] testGenerationSignature = Hex.decode("442c29a4d18f192164006030640fb54c8b9ffd4f5750d2f6dca192dc653c52ad");
    private BigInteger testHit = new BigInteger("762657575297429274", 10);
    private BigInteger testPower = BigInteger.valueOf(117);
    private BigInteger testBaseTarget = new BigInteger("21D0369D036978",16);
    private BigInteger testLastCumulativeDifficulty = new BigInteger("1283794322", 10);
    private long testTimeInterval = 3;
    private BigInteger testTarget = new BigInteger("285528216375286800", 10);
    private byte[] testPubKey = Hex.decode("02e504e474d05950ad5032632d8f73895185a07dd34656a5b2c7b273e8d74d09cb");
    private BigInteger testDifficulty = BigInteger.valueOf(1283796260);

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
