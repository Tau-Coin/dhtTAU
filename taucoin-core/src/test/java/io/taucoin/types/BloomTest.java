package io.taucoin.types;

import org.junit.Test;

import java.util.Random;

import io.taucoin.core.Bloom;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.HashUtil;

public class BloomTest {

//    public void testBloomFilterFakePositive() {
//        // 测10个过滤器
//        int k = 10;
//        double averageFalsePositiveRate = 0;
//        for (int n = 0; n < k; n++) {
//            long bloomCap = 200;
//            Bloom totalBloom = new Bloom();
//
//            // 合成
//            for (long i = bloomCap * n; i < bloomCap * (n + 1); i++) {
//                byte[] hash = HashUtil.sha1hash(ByteUtil.longToBytes(i));
////                logger.error("i = {}, hash: {}", i, Hex.toHexString(hash));
//                Bloom bloom = Bloom.create(hash);
//                totalBloom.or(bloom);
//            }
//
//            long totalTestNum = 1000000;
//            long firstNum = 10000;
//            long lastNum = totalTestNum + firstNum;
//            long falsePositive = 0;
//            // 检验
//            for (long i = firstNum; i < lastNum; i++) {
//                byte[] hash = HashUtil.sha1hash(ByteUtil.longToBytes(i));
////            logger.error("i = {}, hash: {}", i, Hex.toHexString(hash));
//                Bloom bloom = Bloom.create(hash);
//                if (totalBloom.matches(bloom)) {
//                    falsePositive++;
//                }
//            }
//
//            double falsePositiveRate = falsePositive * 1.0 / totalTestNum;
//            logger.error("----------[{}]---------false positive num:{}, rate:{}", n, falsePositive, falsePositiveRate);
//
//            averageFalsePositiveRate += falsePositiveRate;
//        }
//
//        logger.error("Average false positive rate:{}", averageFalsePositiveRate / k);
//    }
//
//    public void testMsgBloomFilterFalsePositive() {
//        // 测10个过滤器
//        int k = 10;
//        double averageFalsePositiveRate = 0;
//        for (int n = 0; n < k; n++) {
//            long bloomCap = 200;
//            Bloom totalBloom = new Bloom();
//
//            // 合成
//            for (long i = bloomCap * n; i < bloomCap * (n + 1) - 1; i++) {
//                byte[] firstHash = HashUtil.sha1hash(ByteUtil.longToBytes(i));
//                byte[] secondHash = HashUtil.sha1hash(ByteUtil.longToBytes(i + 1));
//                byte[] mergedHash = ByteUtil.merge(firstHash, secondHash);
//                Bloom bloom = Bloom.create(mergedHash);
//                totalBloom.or(bloom);
//            }
//
//            Bloom bloom = Bloom.create(HashUtil.sha1hash(ByteUtil.longToBytes(bloomCap * n)));
//            totalBloom.or(bloom);
//            bloom = Bloom.create(HashUtil.sha1hash(ByteUtil.longToBytes(bloomCap * (n + 1) - 1)));
//            totalBloom.or(bloom);
//
//            long totalTestNum = 1000000;
//            long firstNum = 10000;
//            long lastNum = totalTestNum + firstNum;
//            long falsePositive = 0;
//            // 检验
//            for (long i = firstNum + 1; i < lastNum - 2; i++) {
//                byte[] formerHash = HashUtil.sha1hash(ByteUtil.longToBytes(i - 1));
//                byte[] middleHash = HashUtil.sha1hash(ByteUtil.longToBytes(i));
//                byte[] latterHash = HashUtil.sha1hash(ByteUtil.longToBytes(i + 1));
//
//                byte[] firstMergedHash = ByteUtil.merge(formerHash, middleHash);
//                Bloom firstBloom = Bloom.create(firstMergedHash);
//
//                byte[] secondMergedHash = ByteUtil.merge(middleHash, latterHash);
//                Bloom secondBloom = Bloom.create(secondMergedHash);
//
//                if (totalBloom.matches(firstBloom) && totalBloom.matches(secondBloom)) {
//                    falsePositive++;
//                }
//            }
//
//            // 处理第1个元素
//            byte[] formerHash = HashUtil.sha1hash(ByteUtil.longToBytes(firstNum));
//            byte[] middleHash = HashUtil.sha1hash(ByteUtil.longToBytes(firstNum + 1));
//
//            Bloom firstBloom = Bloom.create(formerHash);
//
//            byte[] secondMergedHash = ByteUtil.merge(formerHash, middleHash);
//            Bloom secondBloom = Bloom.create(secondMergedHash);
//
//            if (totalBloom.matches(firstBloom) && totalBloom.matches(secondBloom)) {
//                falsePositive++;
//            }
//
//            // 处理最后1个元素
//            formerHash = HashUtil.sha1hash(ByteUtil.longToBytes(lastNum - 2));
//            middleHash = HashUtil.sha1hash(ByteUtil.longToBytes(lastNum - 1));
//
//            byte[] firstMergedHash = ByteUtil.merge(formerHash, middleHash);
//            firstBloom = Bloom.create(firstMergedHash);
//
//            secondBloom = Bloom.create(middleHash);
//
//            if (totalBloom.matches(firstBloom) && totalBloom.matches(secondBloom)) {
//                falsePositive++;
//            }
//
//            double falsePositiveRate = falsePositive * 1.0 / totalTestNum;
//            logger.error("----------[{}]---------false positive num:{}, rate:{}", n, falsePositive, falsePositiveRate);
//
//            averageFalsePositiveRate += falsePositiveRate;
//        }
//
//        logger.error("Average false positive rate:{}", averageFalsePositiveRate / k);
//    }

    @Test
    public void test() {
        for (int i = 0; i < 10; i++) {
            Random random = new Random(System.currentTimeMillis() + i);
            // 取值范围0 ~ size，当index取size时选自己
            int index = random.nextInt(3);
            System.out.println(index);
        }
    }
}
