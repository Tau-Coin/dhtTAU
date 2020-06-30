package io.taucoin.db;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockInfoTest {
    private static final Logger logger = LoggerFactory.getLogger("test");

    @Test
    public void MainChainCodecTest() {
        BlockInfo blockInfo = new BlockInfo("hash1".getBytes(), true);
        logger.info("hash:{}, MainChain:{}", blockInfo.getHash(), blockInfo.isMainChain());
        logger.info("encode:{}", blockInfo.getEncoded());

        BlockInfo blockInfo1 = new BlockInfo(blockInfo.getEncoded());
        Assert.assertEquals(blockInfo, blockInfo1);
    }

    @Test
    public void NonMainChainCodecTest() {
        BlockInfo blockInfo = new BlockInfo("hash1".getBytes(), false);
        logger.info("hash:{}, MainChain:{}", blockInfo.getHash(), blockInfo.isMainChain());
        logger.info("encode:{}", blockInfo.getEncoded());

        BlockInfo blockInfo1 = new BlockInfo(blockInfo.getEncoded());
        Assert.assertEquals(blockInfo, blockInfo1);
    }
}
