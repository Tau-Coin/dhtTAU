package io.taucoin.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

import static java.lang.Math.*;

public class ProofOfTransaction {
    private static final Logger logger = LoggerFactory.getLogger("proofoftransaction");

    public final static int AverageCommunityChainBlockTime = 300;
    private final static BigInteger CommunityChainGenesisBaseTarget = new BigInteger("21D0369D036978", 16);
    private final static BigInteger DiffAdjustNumerator = new BigInteger("010000000000000000",16);
    private final static BigInteger DiffAdjustNumeratorHalf = new BigInteger("0100000000",16);
    private final static BigInteger DiffAdjustNumeratorCoe = new BigInteger("800000000000000",16); //2^59

    private int averageBlockTime;

    private int minRatio;
    private int maxRatio;

    private int minBlockTime;
    private int maxBlockTime;

    private BigInteger genesisBaseTarget;

    private ProofOfTransaction() {
    }

    public ProofOfTransaction(int averageBlockTime) {
        this.averageBlockTime = averageBlockTime;

        // minRatio = averageBlockTime * (1 - 7 / 60)
        this.minRatio = this.averageBlockTime - this.averageBlockTime * 7 / 60;
        // maxRatio = averageBlockTime * (1 + 7 / 60)
        this.maxRatio = this.averageBlockTime + this.averageBlockTime * 7 / 60;

        // minBlockTime : aAverageBlockTime : maxBlockTime = 1 : 5 : 9
        this.minBlockTime = this.averageBlockTime / 5;
        this.maxBlockTime = this.averageBlockTime * 9 / 5;

        // BaseTarget and Time are in inverse proportion
        // genesisBaseTarget = CommunityChainGenesisBaseTarget * AverageCommunityChainBlockTime / averageBlockTime
        this.genesisBaseTarget = CommunityChainGenesisBaseTarget.
                multiply(BigInteger.valueOf(AverageCommunityChainBlockTime)).
                divide(BigInteger.valueOf(averageBlockTime));
    }

//    /**
//     * get required base target
//     * @param previousBlock
//     * @param blockStore
//     * @return
//     */
//    public static BigInteger calculateRequiredBaseTarget(Block previousBlock, BlockStore blockStore) {
//        long blockNumber = previousBlock.getNumber();
//        if(blockNumber <= 3) {
//            return (new BigInteger("369D0369D036978",16));
//        }
//
//        Block ancestor1 = blockStore.getBlockByHash(previousBlock.getPreviousHeaderHash());
//        Block ancestor2 = blockStore.getBlockByHash(ancestor1.getPreviousHeaderHash());
//        Block ancestor3 = blockStore.getBlockByHash(ancestor2.getPreviousHeaderHash());
//        if (ancestor3 == null) {
//            logger.error("Can not find ancestor block, block number:" + (blockNumber - 3));
//        }
//
//        BigInteger previousBlockBaseTarget = previousBlock.getBaseTarget();
//        long pastTimeFromLatestBlock = new BigInteger(previousBlock.getTimestamp()).longValue() -
//                new BigInteger(ancestor3.getTimestamp()).longValue();
//
//        if (pastTimeFromLatestBlock < 0)
//            pastTimeFromLatestBlock = 0;
//        long pastTimeAver = pastTimeFromLatestBlock / 3;
//
//        BigInteger newRequiredBaseTarget;
//        if( pastTimeAver > AVERTIME ) {
//            long min = 0;
//
//            if (pastTimeAver < maxRatio){
//                min = pastTimeAver;
//            }else {
//                min = maxRatio;
//            }
//
//            newRequiredBaseTarget = previousBlockBaseTarget.multiply(BigInteger.valueOf(min)).divide(BigInteger.valueOf(AVERTIME));
//        }else{
//            long max = 0;
//
//            if (pastTimeAver > minRatio){
//                max = pastTimeAver;
//            }else{
//                max = minRatio;
//            }
//
//            newRequiredBaseTarget = previousBlockBaseTarget.
//                    subtract(previousBlockBaseTarget.divide(BigInteger.valueOf(1875)).
//                            multiply(BigInteger.valueOf(AVERTIME-max)).multiply(BigInteger.valueOf(4)));
//        }
//        return newRequiredBaseTarget;
//    }


    /**
     * get next block generation signature
     *     Gn+1 = hash(Gn, pubkey)
     * @param preGenerationSignature
     * @param pubkey
     * @return
     */
    public byte[] calculateGenerationSignature(byte[] preGenerationSignature, byte[] pubkey){
        byte[] data = new byte[preGenerationSignature.length + pubkey.length];

        System.arraycopy(preGenerationSignature, 0, data, 0, preGenerationSignature.length);
        System.arraycopy(pubkey, 0, data, preGenerationSignature.length, pubkey.length);

        return Sha256Hash.hash(data);
    }


    /**
     * get miner target value
     * target = base target * sqrt(power) * time
     * @param baseTarget
     * @param power
     * @param time
     * @return
     */
    public BigInteger calculateMinerTargetValue(BigInteger baseTarget, BigInteger power, long time){
        Double p = sqrt(power.doubleValue());
        BigInteger realPower = BigInteger.valueOf(p.longValue());
        return baseTarget.multiply(realPower).
                multiply(BigInteger.valueOf(time));
    }


    /**
     * calculate hit
     * Hit = pow(2, 59) * |ln(((first eight bytes of Gn+1) + 1) / pow(2, 64))|
     * @param generationSignature
     * @return
     */
    public BigInteger calculateRandomHit(byte[] generationSignature){
        byte[] headBytes = new byte[8];
        System.arraycopy(generationSignature,0,headBytes,0,8);

        BigInteger bhit = new BigInteger(1, headBytes);
        logger.info("bhit:{}", bhit);

        BigInteger bhitUzero = bhit.add(BigInteger.ONE);
        logger.info("bhitUzero:{}", bhitUzero);

        double logarithm = abs(log(bhitUzero.doubleValue()) - 2 * log(DiffAdjustNumeratorHalf.doubleValue()));
        // Values of logarithm are mostly distributed between (0, 0.1), and int64(logarithm) == 0
        // To make hit smoother, we use logarithm * 1000 instead
        logarithm = logarithm * 1000;
        logger.info("logarithm:{}", logarithm);

        long ulogarithm = (Double.valueOf(logarithm)).longValue();
        logger.info("ulogarithm:{}", ulogarithm);

        // To offset the impact, divide by 1000
        BigInteger adjustHit = DiffAdjustNumeratorCoe.multiply(BigInteger.valueOf(ulogarithm)).divide(BigInteger.valueOf(1000));
        logger.info("adjustHit:{}", adjustHit);

        return adjustHit;
    }

    /**
     * calculate cumulative difficulty
     * @param lastCumulativeDifficulty
     * @param baseTarget
     * @return
     */
    public BigInteger calculateCumulativeDifficulty(BigInteger lastCumulativeDifficulty, BigInteger baseTarget) {
        return DiffAdjustNumerator.divide(baseTarget).add(lastCumulativeDifficulty);
    }

    /**
     * calculate mining time interval
     * @param hit
     * @param baseTarget
     * @param power
     * @return
     */
    public long calculateMiningTimeInterval(BigInteger hit, BigInteger baseTarget, BigInteger power) {
        // we need 𝐻 < 𝑇 = 𝑇(𝑏,𝑛) × sqrt(𝑃𝑒) × C
        // when T = H, calc C
        Double p = sqrt(power.doubleValue());
        BigInteger realPower = BigInteger.valueOf(p.longValue());
        long timeInterval =
                hit.divide(baseTarget).divide(realPower).longValue();

        // C++ to make sure T > H
        timeInterval++;
        logger.info("Time interval:{}", timeInterval);

        if (timeInterval < this.minBlockTime) {
            timeInterval = this.minBlockTime;
        } else if (timeInterval > this.maxBlockTime) {
            timeInterval = this.maxBlockTime;
        }
        logger.info("Final time interval:{}", timeInterval);

        return timeInterval;
    }

    /**
     * verifyHit verifies that target is greater than hit or the time meets the requirements
     * @param hit
     * @param baseTarget
     * @param power
     * @param timeInterval
     * @return
     */
    public boolean verifyHit(BigInteger hit, BigInteger baseTarget, BigInteger power, int timeInterval) {
        if (timeInterval < this.minBlockTime) {
            logger.error("Time interval is less than MinBlockTime[{}]", this.minBlockTime);
            return false;
        } else if (timeInterval >= this.maxBlockTime) {
            logger.info("OK. Time interval is greater than MaxBlockTime[{}]", this.maxBlockTime);
            return true;
        } else {
            BigInteger target = this.calculateMinerTargetValue(baseTarget, power, timeInterval);
            if (target.compareTo(hit) <= 0) {
                logger.error("Invalid POT: target[{}] <= hit[{}]", target, hit);
                return false;
            }
        }
        return true;
    }

}

