/*
package io.taucoin.db;

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
import java.util.List;

public class BlockInfosTest {
    private static final Logger logger = LoggerFactory.getLogger("test");

    private static final String txdata = "f889809a736861726520796f7572206661766f7572697465206d75736963b86b687474703a2f2f7777772e6b75676f752e636f6d2f736f6e672f366e6e796162622e68746d6c3f66726f6d62616964753f66726f6d626169647523686173683d423036413434304234433231453239423334414338303338453330384530324626616c62756d5f69643d30";
    private static final byte[] chainid = "TAUcoin#300#3938383036366633393364383365393338373434".getBytes();
    private static final byte[] sender = ByteUtil.toByte("c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c");
    private static final byte[] signature = ByteUtil.toByte("281f3c2fe309683c74762f965f38bd8f8910d8dbeca1da904d6821e8101075776243379a4efdfdc8c10ae34be767a825f770e6a62b5430c030f179b74057e747");
    private static final String transaction = "f9013b01b4544155636f696e233330302333393338333833303336333636363333333933333634333833333635333933333338333733343334880000000008f0d18084ffffffc4a0c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c880000000000000000f889809a736861726520796f7572206661766f7572697465206d75736963b86b687474703a2f2f7777772e6b75676f752e636f6d2f736f6e672f366e6e796162622e68746d6c3f66726f6d62616964753f66726f6d626169647523686173683d423036413434304234433231453239423334414338303338453330384530324626616c62756d5f69643d30b840281f3c2fe309683c74762f965f38bd8f8910d8dbeca1da904d6821e8101075776243379a4efdfdc8c10ae34be767a825f770e6a62b5430c030f179b74057e747";
    private static final byte version = 1;
    private static final short expire = 30;

    @Test
    public void CodecTest() {
        BlockInfos blockInfos = new BlockInfos();

        long time1 = 1;
        long time2 = 11;
        long time3 = 21;
        TxData txData = new TxData(ByteUtil.toByte(txdata));
        Transaction tx = new Transaction(version,chainid,150000000,-60,sender,0,txData,signature);

        Block block1 = new Block((byte)1, "chain1".getBytes(), time1, 1, new byte[2], new byte[2],
                BigInteger.ONE, BigInteger.ONE, new byte[2], tx, 1, 1, 1, 1,
                new byte[64], new byte[32]);
        logger.info("hash:{}", Hex.toHexString(block1.getBlockHash()));
        blockInfos.putBlock(block1, true);

        Block block2 = new Block((byte)1, "chain1".getBytes(), time2, 1, block1.getBlockHash(), new byte[2],
                BigInteger.ONE, BigInteger.ONE, new byte[2], tx, 1, 1, 1, 1,
                new byte[64], new byte[32]);
        logger.info("hash:{}", Hex.toHexString(block2.getBlockHash()));
        blockInfos.putBlock(block2, false);

        Block block3 = new Block((byte)1, "chain1".getBytes(), time3, 1, block2.getBlockHash(), new byte[2],
                BigInteger.ONE, BigInteger.ONE, new byte[2], tx, 1, 1, 1, 1,
                new byte[64], new byte[32]);
        logger.info("hash:{}", Hex.toHexString(block3.getBlockHash()));
        blockInfos.putBlock(block3, false);

        List<BlockInfo> list = blockInfos.getBlockInfoList();
        for (BlockInfo blockInfo: list) {
            logger.info("Size:{}, Hash:{}, MainChain:{}",
                    list.size(), Hex.toHexString(blockInfo.getHash()), blockInfo.isMainChain());
        }

        byte[] rlp = blockInfos.getEncoded();
        BlockInfos blockInfos1 = new BlockInfos(rlp);
        List<BlockInfo> list1 = blockInfos1.getBlockInfoList();
        for (BlockInfo blockInfo: list1) {
            logger.info("Size:{}, Hash:{}, MainChain:{}",
                    list.size(), Hex.toHexString(blockInfo.getHash()), blockInfo.isMainChain());
        }
        Assert.assertEquals(list.size(), list1.size());
        Assert.assertArrayEquals(blockInfos.getEncoded(), blockInfos1.getEncoded());
        for (int i = 0; i < list.size(); i++) {
            Assert.assertEquals(list.get(i), list1.get(i));
        }
    }

    @Test
    public void PutBlockTest() {
        BlockInfos blockInfos = new BlockInfos();

        long time1 = 1;
        long time2 = 11;
        long time3 = 21;
        TxData txData = new TxData(ByteUtil.toByte(txdata));
        Transaction tx = new Transaction(version,chainid,150000000,-60,sender,0,txData,signature);

        Block block1 = new Block((byte)1, "chain1".getBytes(), time1, 1, new byte[2], new byte[2],
                BigInteger.ONE, BigInteger.ONE, new byte[2], tx, 1, 1, 1, 1,
                new byte[64], new byte[32]);
        logger.info("hash:{}", Hex.toHexString(block1.getBlockHash()));
        blockInfos.putBlock(block1, true);

        Block block2 = new Block((byte)1, "chain1".getBytes(), time2, 1, block1.getBlockHash(), new byte[2],
                BigInteger.ONE, BigInteger.ONE, new byte[2], tx, 1, 1, 1, 1,
                new byte[64], new byte[32]);
        logger.info("hash:{}", Hex.toHexString(block2.getBlockHash()));
        blockInfos.putBlock(block2, false);

        Block block3 = new Block((byte)1, "chain1".getBytes(), time3, 1, block2.getBlockHash(), new byte[2],
                BigInteger.ONE, BigInteger.ONE, new byte[2], tx, 1, 1, 1, 1,
                new byte[64], new byte[32]);
        logger.info("hash:{}", Hex.toHexString(block3.getBlockHash()));
        blockInfos.putBlock(block3, false);

        List<BlockInfo> list = blockInfos.getBlockInfoList();
        for (BlockInfo blockInfo: list) {
            logger.info("Size:{}, Hash:{}, MainChain:{}",
                    list.size(), Hex.toHexString(blockInfo.getHash()), blockInfo.isMainChain());
        }

        byte[] rlp = blockInfos.getEncoded();
        BlockInfos blockInfos1 = new BlockInfos(rlp);
        blockInfos1.putBlock(block3, true);
        List<BlockInfo> list1 = blockInfos1.getBlockInfoList();
        for (BlockInfo blockInfo: list1) {
            logger.info("Size:{}, Hash:{}, MainChain:{}",
                    list.size(), Hex.toHexString(blockInfo.getHash()), blockInfo.isMainChain());
        }
        Assert.assertEquals(list.size(), list1.size());
        Assert.assertNotEquals(blockInfos.getEncoded(), blockInfos1.getEncoded());
        int size = list.size();
        for (int i = 0; i < size - 1; i++) {
            Assert.assertEquals(list.get(i), list1.get(i));
        }
        Assert.assertArrayEquals(list.get(size - 1).getHash(), list1.get(size - 1).getHash());
        Assert.assertNotEquals(list.get(size - 1).isMainChain(), list1.get(size - 1).isMainChain());
    }
}

*/
