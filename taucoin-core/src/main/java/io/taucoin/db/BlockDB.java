package io.taucoin.db;

import io.taucoin.types.BlockContainer;
import io.taucoin.param.ChainParam;
import io.taucoin.types.Block;
import io.taucoin.types.Transaction;
import io.taucoin.types.TransactionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockDB implements BlockStore {
    private static final Logger logger = LoggerFactory.getLogger("BlockDB");

    private KeyValueDataBase db;

    public BlockDB(KeyValueDataBase db) {
        this.db = db;
    }

    /**
     * open db
     * @param path database path which can be accessed
     * @throws Exception
     */
    public void open(String path) throws Exception {
        db.open(path);
    }

    /**
     * close db
     */
    public void close() {
        db.close();
    }

    /**
     * save tx in db
     * @param chainID chain ID
     * @param tx transaction
     */
    private void saveTransaction(byte[] chainID, Transaction tx) throws Exception {
        if (null != tx) {
            db.put(PrefixKey.txKey(chainID, tx.getTxID()), tx.getEncoded());
        }
    }

    /**
     * get tx by hash
     * @param chainID chain ID
     * @param hash txid
     * @return transaction or null
     * @throws Exception
     */
    @Override
    public Transaction getTransactionByHash(byte[] chainID, byte[] hash) throws Exception {
        byte[] encode = db.get(PrefixKey.txKey(chainID, hash));
        if (null != encode) {
            return TransactionFactory.parseTransaction(encode);
        }

        logger.info("ChainID[{}]:Cannot find block by hash:{}", new String(chainID), Hex.toHexString(hash));
        return null;
    }

    /**
     * get block by hash
     * @param chainID
     * @param hash
     * @return
     * @throws Exception
     */
    @Override
    public Block getBlockByHash(byte[] chainID, byte[] hash) throws Exception {
        byte[] blockEncode = db.get(PrefixKey.blockKey(chainID, hash));
        if (null != blockEncode) {
            return new Block(blockEncode);
        }
        logger.info("ChainID[{}]:Cannot find block by hash:{}", new String(chainID), Hex.toHexString(hash));
        return null;
    }

    /**
     * get block container by hash
     *
     * @param chainID chain ID
     * @param hash    block hash
     * @return block container
     * @throws Exception
     */
    @Override
    public BlockContainer getBlockContainerByHash(byte[] chainID, byte[] hash) throws Exception {
        Block block = getBlockByHash(chainID, hash);
        if (null != block) {
            BlockContainer blockContainer = new BlockContainer(block);
            if (null != block.getTxHash()) {
                Transaction tx = getTransactionByHash(chainID, block.getBlockHash());
                if (null != tx) {
                    blockContainer.setTx(tx);
                }
            }

            return blockContainer;
        }

        return null;
    }

    /**
     * get block info
     * @param chainID
     * @param number
     * @param hash
     * @return
     * @throws Exception
     */
    private BlockInfo getBlockInfo(byte[] chainID, long number, byte[] hash) throws Exception {
        byte[] rlp = db.get(PrefixKey.blockInfoKey(chainID, number));
        if (null == rlp) {
            logger.info("ChainID[{}]:There is no block in this height:{}", new String(chainID), number);
            return null;
        }
        BlockInfos blockInfos = new BlockInfos(rlp);
        List<BlockInfo> list = blockInfos.getBlockInfoList();
        for (BlockInfo blockInfo: list) {
            if (Arrays.equals(blockInfo.getHash(), hash)) {
                return blockInfo;
            }
        }

        return null;
    }

    /**
     * get block info by hash
     *
     * @param chainID
     * @param hash
     * @return
     * @throws Exception
     */
    @Override
    public BlockInfo getBlockInfoByHash(byte[] chainID, byte[] hash) throws Exception {
        byte[] rlp = db.get(PrefixKey.blockKey(chainID, hash));
        if (null != rlp) {
            Block block = new Block(rlp);
            return getBlockInfo(chainID, block.getBlockNum(), hash);
        }

        logger.info("ChainID[{}]:Cannot find block info by hash:{}", new String(chainID), Hex.toHexString(hash));
        return null;
    }

    /**
     * if a block hash is main chain block hash
     *
     * @param chainID chain ID
     * @param hash    block hash
     * @return true/false
     */
    @Override
    public boolean isMainChainBlock(byte[] chainID, byte[] hash) throws Exception {
        byte[] encode = db.get(PrefixKey.blockKey(chainID, hash));
        if (null != encode) {
            Block block = new Block(encode);
            BlockInfo blockInfo = getBlockInfo(chainID, block.getBlockNum(), hash);
            if (null != blockInfo) {
                return blockInfo.isMainChain();
            }
        }

        logger.info("ChainID[{}]:Cannot find block info by hash:{}", new String(chainID), Hex.toHexString(hash));

        return false;
    }

    /**
     * get main chain block by number
     * @param number
     * @return
     * @throws Exception
     */
    @Override
    public Block getMainChainBlockByNumber(byte[] chainID, long number) throws Exception {
        byte[] rlp = db.get(PrefixKey.blockInfoKey(chainID, number));
        if (null == rlp) {
            logger.info("ChainID[{}]:There is no block in this height:{}", new String(chainID), number);
            return null;
        }

        BlockInfos blockInfos = new BlockInfos(rlp);
        List<BlockInfo> list = blockInfos.getBlockInfoList();
        logger.debug("Chain ID[{}]: Block infos in height[{}] size: {}",
                new String(chainID), number, list.size());
        for (BlockInfo blockInfo: list) {
            logger.debug("Chain ID[{}]: block info in height {}: {}",
                    new String(chainID), number, blockInfo.toString());
            if (blockInfo.isMainChain()) {
                return getBlockByHash(chainID, blockInfo.getHash());
            }
        }
        logger.info("ChainID[{}]:There is no main chain block in this height:{}",
                new String(chainID), number);
        return null;
    }

    /**
     * get main chain block container by number
     *
     * @param chainID chain id
     * @param number  block number
     * @return block container or null
     * @throws Exception
     */
    @Override
    public BlockContainer getMainChainBlockContainerByNumber(byte[] chainID, long number) throws Exception {
        Block block = getMainChainBlockByNumber(chainID, number);

        if (null == block) {
            return null;
        }

        BlockContainer blockContainer = new BlockContainer(block);
        if (null != block.getTxHash()) {
            Transaction tx = getTransactionByHash(chainID, block.getTxHash());

            if (null == tx) {
                return null;
            }

            blockContainer.setTx(tx);
        }

        return blockContainer;
    }

    /**
     * get main chain block hash by number
     *
     * @param chainID chain ID
     * @param number  block number
     * @return block hash
     * @throws Exception
     */
    @Override
    public byte[] getMainChainBlockHashByNumber(byte[] chainID, long number) throws Exception {
        byte[] rlp = db.get(PrefixKey.blockInfoKey(chainID, number));
        if (null == rlp) {
            logger.info("ChainID[{}]:There is no block in this height:{}",
                    new String(chainID), number);
            return null;
        }
        BlockInfos blockInfos = new BlockInfos(rlp);
        List<BlockInfo> list = blockInfos.getBlockInfoList();
        logger.debug("Chain ID[{}]: Block infos in height[{}] size: {}",
                new String(chainID), number, list.size());
        for (BlockInfo blockInfo: list) {
            logger.debug("Chain ID[{}]: block info in height {}: {}",
                    new String(chainID), number, blockInfo.toString());
            if (blockInfo.isMainChain()) {
                return blockInfo.getHash();
            }
        }
        logger.info("ChainID[{}]:There is no main chain block hash in this height:{}",
                new String(chainID), number);
        return null;
    }

    /**
     * save block info in db
     * @param chainID chain ID
     * @param block block
     * @param isMainChain if main chain
     * @throws Exception
     */
    private void saveBlockInfo(byte[] chainID, Block block, boolean isMainChain) throws Exception {
        BlockInfos blockInfos;
        // if saved in the same height
        byte[] rlp = db.get(PrefixKey.blockInfoKey(chainID, block.getBlockNum()));
        if (null == rlp) {
            blockInfos = new BlockInfos();
        } else {
            blockInfos = new BlockInfos(rlp);
        }
        blockInfos.putBlock(block, isMainChain);

        db.put(PrefixKey.blockInfoKey(chainID, block.getBlockNum()), blockInfos.getEncoded());
    }

    /**
     * save block
     *
     * @param chainID
     * @param block
     * @throws Exception
     */
    @Override
    public void saveBlock(byte[] chainID, Block block, boolean isMainChain) throws Exception {
        // save block
        db.put(PrefixKey.blockKey(chainID, block.getBlockHash()), block.getEncoded());
        // save block info
        saveBlockInfo(chainID, block, isMainChain);
        // delete fork chain blocks out of 3 * mutable range, when save main chain block
        if (isMainChain && block.getBlockNum() > ChainParam.WARNING_RANGE) {
            long number = block.getBlockNum() - ChainParam.WARNING_RANGE;
            logger.info("ChainID[{}]: Delete fork chain block in height:{}",
                    new String(chainID), number);
            delForkChainBlockByNumber(chainID, number);
        }
    }

    /**
     * save block container
     *
     * @param chainID        chain ID
     * @param blockContainer block container to save
     * @param isMainChain
     * @throws Exception
     */
    @Override
    public void saveBlockContainer(byte[] chainID, BlockContainer blockContainer, boolean isMainChain) throws Exception {
        Block block = blockContainer.getBlock();

        // save block
        db.put(PrefixKey.blockKey(chainID, block.getBlockHash()), block.getEncoded());

        // save tx
        Transaction tx = blockContainer.getTx();
        saveTransaction(chainID, tx);

        // save block info
        saveBlockInfo(chainID, block, isMainChain);

        // delete fork chain blocks out of 3 * mutable range, when save main chain block
        if (isMainChain && block.getBlockNum() > ChainParam.WARNING_RANGE) {
            long number = block.getBlockNum() - ChainParam.WARNING_RANGE;
            logger.info("ChainID[{}]: Delete fork chain block in height:{}",
                    new String(chainID), number);
            delForkChainBlockByNumber(chainID, number);
        }
    }

    /**
     * delete fork chain block, tx and block info
     * @param chainID chain ID
     * @param number number
     * @throws Exception
     */
    public void delForkChainBlockByNumber(byte[] chainID, long number) throws Exception {
        byte[] rlp = db.get(PrefixKey.blockInfoKey(chainID, number));
        if (null == rlp) {
            logger.info("ChainID[{}]: There is no block in this height:{}", new String(chainID), number);
            return;
        }
        BlockInfos blockInfos = new BlockInfos(rlp);
        List<BlockInfo> list = blockInfos.getBlockInfoList();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (!list.get(i).isMainChain()) {
                // delete tx
                Block block = getBlockByHash(chainID, list.get(i).getHash());
                if (null != block) {
                    if (null != block.getTxHash()) {
                        db.delete(PrefixKey.txKey(chainID, block.getTxHash()));
                    }
                }
                // delete non-main chain block
                db.delete(PrefixKey.blockKey(chainID, list.get(i).getHash()));
                list.remove(i);
            }
        }
        // save main chain block info
        db.put(PrefixKey.blockInfoKey(chainID, number), blockInfos.getEncoded());
    }

    /**
     * get all blocks of a chain, whether it is a block on the main chain or not
     * @param chainID
     * @return
     * @throws Exception
     */
    @Override
    public Set<Block> getChainAllBlocks(byte[] chainID) throws Exception {
        Set<byte[]> blocks = db.retrieveKeysWithPrefix(PrefixKey.blockPrefix(chainID));
        if (null == blocks) {
            logger.info("ChainID[{}]: Cannot find any block", new String(chainID));
            return null;
        }
        Set<Block> set = new HashSet<>();
        for (byte[] rlp: blocks) {
            set.add(new Block(rlp));
        }
        return set;
    }

    /**
     * remove all blocks and info of a chain
     * @param chainID
     * @throws Exception
     */
    @Override
    public void removeChain(byte[] chainID) throws Exception {
        db.removeWithKeyPrefix(chainID);
    }

    /**
     * get fork point block between main chain and fork chain
     *
     * @param chainID chain ID
     * @param block block on chain
     * @return
     * @throws Exception
     */
    @Override
    public Block getForkPointBlock(byte[] chainID, Block block) throws Exception {

        // if on main chain
        byte[] infoRLP = db.get(PrefixKey.blockInfoKey(chainID, block.getBlockNum()));
        if (null != infoRLP) {
            BlockInfos blockInfos = new BlockInfos(infoRLP);
            List<BlockInfo> list = blockInfos.getBlockInfoList();

            for (BlockInfo blockInfo : list) {
                if (Arrays.equals(blockInfo.getHash(), block.getBlockHash()) && blockInfo.isMainChain()) {
                    // return itself
                    return block;
                }
            }
        } else {
            logger.info("ChainID[{}]: Cannot find block info with current block.", new String(chainID));
        }

        byte[] rlp = db.get(PrefixKey.blockKey(chainID, block.getPreviousBlockHash()));
        // if previous is null, return
        if (null == rlp) {
            logger.error("ChainID[{}]: Cannot find previous block, hash[{}]",
                    new String(chainID), Hex.toHexString(block.getPreviousBlockHash()));
            return null;
        }
        Block previousBlock = new Block(rlp);

        while (true) {
            infoRLP = db.get(PrefixKey.blockInfoKey(chainID, previousBlock.getBlockNum()));
            if (null == infoRLP) {
                logger.error("ChainID[{}]: Cannot find block info with this block number:{}",
                        new String(chainID), previousBlock.getBlockNum());
                return null;
            }
            BlockInfos blockInfos = new BlockInfos(infoRLP);
            List<BlockInfo> list = blockInfos.getBlockInfoList();
            boolean found = false;
            for (BlockInfo blockInfo : list) {
                if (Arrays.equals(blockInfo.getHash(), previousBlock.getBlockHash())) {
                    // found block
                    found = true;
                    if (blockInfo.isMainChain()) {
                        // if this block is on main chain, got it
                        return previousBlock;
                    } else {
                        // if this block is not on main chain, look ahead
                        rlp = db.get(PrefixKey.blockKey(chainID, previousBlock.getPreviousBlockHash()));
                        // if previous is null, return
                        if (null == rlp) {
                            logger.error("ChainID[{}]: Cannot find previous block, hash[{}].",
                                    new String(chainID), Hex.toHexString(previousBlock.getPreviousBlockHash()));
                            return null;
                        }
                        logger.info("ChainID[{}]: Seek previous block hash[{}].",
                                new String(chainID), Hex.toHexString(previousBlock.getPreviousBlockHash()));
                        previousBlock = new Block(rlp);
                        break;
                    }
                }
            }
            // if not found this block info in this height
            if (!found) {
                logger.error("ChainID[{}]: Cannot find block info, hash[{}]",
                        new String(chainID), Hex.toHexString(previousBlock.getBlockHash()));
                return null;
            }
        }
    }

    /**
     * get fork point block between chain 1 and chain 2
     *
     * @param chainID chain ID
     * @param chain1Block block on chain 1
     * @param chain2Block block on chain 2
     * @return fork point block or null if not found
     * @throws Exception
     */
    @Override
    public Block getForkPointBlock(byte[] chainID, Block chain1Block, Block chain2Block) throws Exception {

        long maxLevel = Math.max(chain1Block.getBlockNum(), chain2Block.getBlockNum());

        // 1. First ensure that you are one the save level
        long currentLevel = maxLevel;
        Block chain1Line = chain1Block;
        if (chain1Block.getBlockNum() > chain2Block.getBlockNum()) {

            while (currentLevel > chain2Block.getBlockNum()) {
                chain1Line = getBlockByHash(chainID, chain1Line.getPreviousBlockHash());
                if (chain1Line == null)
                    return null;
                --currentLevel;
            }
        }

        Block chain2Line = chain2Block;
        if (chain2Block.getBlockNum() > chain1Block.getBlockNum()) {

            while (currentLevel > chain1Block.getBlockNum()) {
                chain2Line = getBlockByHash(chainID, chain2Line.getPreviousBlockHash());
                if (chain2Line == null)
                    return null;
                --currentLevel;
            }
        }

        // 2. Loop back on each level until common block
        while (!Arrays.equals(chain2Line.getBlockHash(), chain1Line.getBlockHash())) {
            chain2Line = getBlockByHash(chainID, chain2Line.getPreviousBlockHash());
            chain1Line = getBlockByHash(chainID, chain1Line.getPreviousBlockHash());

            if (chain1Line == null || chain2Line == null)
                return null;
        }

        if (Arrays.equals(chain2Line.getBlockHash(), chain1Line.getBlockHash())) {
            return chain1Block;
        }

        return null;
    }

    /**
     * get fork info
     *
     * @param chainID chain ID
     * @param forkBlock  fork point block
     * @param bestBlock current chain best block
     * @param undoBlocks blocks to roll back from high to low
     * @param newBlocks  blocks to connect from high to low
     * @return
     */
    @Override
    public boolean getForkBlocksInfo(byte[] chainID, Block forkBlock, Block bestBlock, List<Block> undoBlocks, List<Block> newBlocks) throws Exception{

        long maxLevel = Math.max(bestBlock.getBlockNum(), forkBlock.getBlockNum());

        // 1. First ensure that you are one the save level
        long currentLevel = maxLevel;
        Block forkLine = forkBlock;
        if (forkBlock.getBlockNum() > bestBlock.getBlockNum()) {

            while (currentLevel > bestBlock.getBlockNum()) {
                newBlocks.add(forkLine);

                forkLine = getBlockByHash(chainID, forkLine.getPreviousBlockHash());
                if (forkLine == null)
                    return false;
                --currentLevel;
            }
        }

        Block bestLine = bestBlock;
        if (bestBlock.getBlockNum() > forkBlock.getBlockNum()) {

            while (currentLevel > forkBlock.getBlockNum()) {
                undoBlocks.add(bestLine);

                bestLine = getBlockByHash(chainID, bestLine.getPreviousBlockHash());
                --currentLevel;
            }
        }

        // 2. Loop back on each level until common block
        while (!Arrays.equals(bestLine.getBlockHash(), forkLine.getBlockHash())) {
            newBlocks.add(forkLine);
            undoBlocks.add(bestLine);

            bestLine = getBlockByHash(chainID, bestLine.getPreviousBlockHash());
            forkLine = getBlockByHash(chainID, forkLine.getPreviousBlockHash());

            if (forkLine == null)
                return false;

            --currentLevel;
        }

        return true;
    }

    /**
     * get fork info
     *
     * @param chainID             chain ID
     * @param forkBlockContainer  fork point block container
     * @param bestBlockContainer  current chain best block container
     * @param undoBlockContainers block containers to roll back from high to low
     * @param newBlockContainers  block containers to connect from high to low
     * @return true/false
     */
    @Override
    public boolean getForkBlockContainersInfo(byte[] chainID,
                                              BlockContainer forkBlockContainer,
                                              BlockContainer bestBlockContainer,
                                              List<BlockContainer> undoBlockContainers,
                                              List<BlockContainer> newBlockContainers) throws Exception {

        long maxLevel = Math.max(bestBlockContainer.getBlock().getBlockNum(),
                forkBlockContainer.getBlock().getBlockNum());

        // 1. First ensure that you are one the save level
        long currentLevel = maxLevel;
        BlockContainer forkLine = forkBlockContainer;
        if (forkBlockContainer.getBlock().getBlockNum() > bestBlockContainer.getBlock().getBlockNum()) {

            while (currentLevel > bestBlockContainer.getBlock().getBlockNum()) {
                newBlockContainers.add(forkLine);

                forkLine = getBlockContainerByHash(chainID, forkLine.getBlock().getPreviousBlockHash());
                if (forkLine == null)
                    return false;
                --currentLevel;
            }
        }

        BlockContainer bestLine = bestBlockContainer;
        if (bestBlockContainer.getBlock().getBlockNum() > forkBlockContainer.getBlock().getBlockNum()) {

            while (currentLevel > forkBlockContainer.getBlock().getBlockNum()) {
                undoBlockContainers.add(bestLine);

                bestLine = getBlockContainerByHash(chainID, bestLine.getBlock().getPreviousBlockHash());
                --currentLevel;
            }
        }

        // 2. Loop back on each level until common block
        while (!Arrays.equals(bestLine.getBlock().getBlockHash(), forkLine.getBlock().getBlockHash())) {
            newBlockContainers.add(forkLine);
            undoBlockContainers.add(bestLine);

            bestLine = getBlockContainerByHash(chainID, bestLine.getBlock().getPreviousBlockHash());
            forkLine = getBlockContainerByHash(chainID, forkLine.getBlock().getPreviousBlockHash());

            if (forkLine == null)
                return false;

            --currentLevel;
        }

        return true;
    }

    /**
     * re-branch blocks
     *
     * @param undoBlocks move to non-main chain
     * @param newBlocks  move to main chain
     */
    @Override
    public void reBranchBlocks(byte[] chainID, List<Block> undoBlocks, List<Block> newBlocks) throws Exception {
        if (undoBlocks != null) {
            for (Block block : undoBlocks) {
                saveBlockInfo(chainID, block, false);
            }
        }

        if (newBlocks != null) {
            for (Block block : newBlocks) {
                saveBlockInfo(chainID, block, true);
            }
        }
    }

    /**
     * re-branch blocks with block containers
     *
     * @param chainID             chain ID
     * @param undoBlockContainers move to non-main chain
     * @param newBlockContainers  move to main chain
     */
    @Override
    public void reBranchBlocksWithContainers(byte[] chainID, List<BlockContainer> undoBlockContainers, List<BlockContainer> newBlockContainers) throws Exception {
        if (undoBlockContainers != null) {
            for (BlockContainer blockContainer : undoBlockContainers) {
                saveBlockInfo(chainID, blockContainer.getBlock(), false);
            }
        }

        if (newBlockContainers != null) {
            for (BlockContainer blockContainer : newBlockContainers) {
                saveBlockInfo(chainID, blockContainer.getBlock(), true);
            }
        }
    }
}

