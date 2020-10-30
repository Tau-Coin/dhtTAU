package io.taucoin.db;

import io.taucoin.types.BlockContainer;
import io.taucoin.param.ChainParam;
import io.taucoin.types.Block;
import io.taucoin.types.HashList;
import io.taucoin.types.HorizontalItem;
import io.taucoin.types.Transaction;
import io.taucoin.types.TransactionFactory;
import io.taucoin.types.VerticalItem;

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
     * save tx in db
     * @param chainID chain ID
     * @param tx transaction
     */
    private void saveTransaction(byte[] chainID, Transaction tx) throws DBException {
        try {
            if (null != tx) {
                db.put(PrefixKey.txKey(chainID, tx.getTxID()), tx.getEncoded());
            }
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * get tx by hash
     * @param chainID chain ID
     * @param hash tx hash
     * @return transaction or null
     * @throws DBException database exception
     */
    @Override
    public Transaction getTransactionByHash(byte[] chainID, byte[] hash) throws DBException {
        if (null == hash) {
            logger.error("Chain ID[{}], tx hash is null", new String(chainID));
            return null;
        }

        byte[] encode;

        try {
            encode = db.get(PrefixKey.txKey(chainID, hash));
        } catch (Exception e) {
            logger.error("GetTransactionByHash:" + e.getMessage(), e);
            throw new DBException(e.getMessage());
        }

        if (null != encode) {
            return TransactionFactory.parseTransaction(encode);
        }

        logger.info("ChainID[{}]:Cannot find tx by hash:{}", new String(chainID), Hex.toHexString(hash));
        return null;
    }

    /**
     * save hash list item
     * @param chainID chain ID
     * @param hashList hash list
     * @throws DBException database exception
     */
    private void saveHashListItem(byte[] chainID, HashList hashList) throws DBException {
        try {
            if (null != hashList) {
                db.put(PrefixKey.hashListKey(chainID, hashList.getHash()), hashList.getEncoded());
            }
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * get horizontal item by hash
     * @param chainID chain ID
     * @param hash hash
     * @return horizontal item
     * @throws DBException database exception
     */
    public HorizontalItem getHorizontalItemByHash(byte[] chainID, byte[] hash) throws DBException {
        if (null == hash) {
            logger.error("Chain ID[{}], hash is null", new String(chainID));
            return null;
        }

        byte[] encode;
        try {
            encode = db.get(PrefixKey.hashListKey(chainID, hash));
        } catch (Exception e) {
            logger.error("getHorizontalItemByHash:" + e.getMessage(), e);
            throw new DBException(e.getMessage());
        }

        if (null != encode) {
            return new HorizontalItem(encode);
        }

        logger.info("ChainID[{}]:Cannot find horizontal item by hash:{}", new String(chainID), Hex.toHexString(hash));
        return null;
    }

    /**
     * get vertical item by hash
     * @param chainID chain ID
     * @param hash hash
     * @return vertical item
     * @throws DBException database exception
     */
    public VerticalItem getVerticalItemByHash(byte[] chainID, byte[] hash) throws DBException {
        if (null == hash) {
            logger.error("Chain ID[{}], hash is null", new String(chainID));
            return null;
        }

        byte[] encode;
        try {
            encode = db.get(PrefixKey.hashListKey(chainID, hash));
        } catch (Exception e) {
            logger.error("getVerticalItemByHash:" + e.getMessage(), e);
            throw new DBException(e.getMessage());
        }

        if (null != encode) {
            return new VerticalItem(encode);
        }

        logger.info("ChainID[{}]:Cannot find vertical item by hash:{}", new String(chainID), Hex.toHexString(hash));
        return null;
    }

    /**
     * get block by hash
     * @param chainID chain ID
     * @param hash block hash
     * @return block if found, null otherwise
     * @throws DBException database exception
     */
    @Override
    public Block getBlockByHash(byte[] chainID, byte[] hash) throws DBException {
        if (null == hash) {
            logger.error("Chain ID[{}], block hash is null", new String(chainID));
            return null;
        }

        byte[] blockEncode;
        try {
            blockEncode = db.get(PrefixKey.blockKey(chainID, hash));
        } catch (Exception e) {
            logger.error("GetBlockByHash:" + e.getMessage(), e);
            throw new DBException(e.getMessage());
        }

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
     * @throws DBException database exception
     */
    @Override
    public BlockContainer getBlockContainerByHash(byte[] chainID, byte[] hash) throws DBException {
        if (null == hash) {
            logger.error("Chain ID[{}], block container hash is null", new String(chainID));
            return null;
        }

        Block block = getBlockByHash(chainID, hash);
        if (null != block) {
            BlockContainer blockContainer = new BlockContainer(block);

            if (null != block.getHorizontalHash()) {
                HorizontalItem horizontalItem = getHorizontalItemByHash(chainID, block.getHorizontalHash());
                if (null != horizontalItem) {
                    blockContainer.setHorizontalItem(horizontalItem);

                    byte[] txHash = horizontalItem.getTxHash();
                    if (null != txHash) {
                        Transaction tx = getTransactionByHash(chainID, txHash);
                        if (null != tx) {
                            blockContainer.setTx(tx);
                        } else {
                            return null;
                        }
                    }
                } else {
                    logger.info("ChainID[{}]:Cannot find horizontal item by hash:{}",
                            new String(chainID), Hex.toHexString(block.getHorizontalHash()));
                    return null;
                }
            }

            if (null != block.getVerticalHash()) {
                VerticalItem verticalItem = getVerticalItemByHash(chainID, block.getVerticalHash());
                if (null != verticalItem) {
                    blockContainer.setVerticalItem(verticalItem);
                } else {
                    logger.info("ChainID[{}]:Cannot find vertical item by hash:{}",
                            new String(chainID), Hex.toHexString(block.getVerticalHash()));
                    return null;
                }
            }

            return blockContainer;
        }

        return null;
    }

    /**
     * get block info
     * @param chainID chain ID
     * @param number block number
     * @param hash block hash
     * @return block info or null if not found
     * @throws DBException database exception
     */
    private BlockInfo getBlockInfo(byte[] chainID, long number, byte[] hash) throws DBException {
        byte[] encode;
        try {
            encode = db.get(PrefixKey.blockInfoKey(chainID, number));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }

        if (null == encode) {
            logger.info("ChainID[{}]:There is no block in this height:{}", new String(chainID), number);
            return null;
        }
        BlockInfos blockInfos = new BlockInfos(encode);
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
     * @param chainID chain ID
     * @param hash block hash
     * @return block info or null if not found
     * @throws DBException database exception
     */
    @Override
    public BlockInfo getBlockInfoByHash(byte[] chainID, byte[] hash) throws DBException {
        if (null == hash) {
            logger.error("Chain ID[{}], block info hash is null", new String(chainID));
            return null;
        }

        byte[] encode;
        try {
            encode = db.get(PrefixKey.blockKey(chainID, hash));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }

        if (null != encode) {
            Block block = new Block(encode);
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
     * @return true if main chain, false otherwise
     * @throws DBException database exception
     */
    @Override
    public boolean isMainChainBlock(byte[] chainID, byte[] hash) throws DBException {
        if (null == hash) {
            logger.error("Chain ID[{}], block hash is null", new String(chainID));
            return false;
        }

        byte[] encode;
        try {
            encode = db.get(PrefixKey.blockKey(chainID, hash));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }

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
     * if a block is on chain
     *
     * @param chainID chain ID
     * @param hash    block hash
     * @return true if main chain, false otherwise
     * @throws DBException database exception
     */
    @Override
    public boolean isBlockOnChain(byte[] chainID, byte[] hash) throws DBException {
        if (null == hash) {
            logger.error("Chain ID[{}], block hash is null", new String(chainID));
            return false;
        }

        byte[] encode;
        try {
            encode = db.get(PrefixKey.blockKey(chainID, hash));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }

        if (null != encode) {
            Block block = new Block(encode);
            BlockInfo blockInfo = getBlockInfo(chainID, block.getBlockNum(), hash);
            if (null != blockInfo) {
                return true;
            }
        }

        logger.info("ChainID[{}]:Cannot find block info by hash:{}", new String(chainID), Hex.toHexString(hash));

        return false;
    }

    /**
     * get main chain block by number
     * @param chainID chain ID
     * @param number block number
     * @return block or null if not found
     * @throws DBException database exception
     */
    @Override
    public Block getMainChainBlockByNumber(byte[] chainID, long number) throws DBException {
        byte[] encode;
        try {
            encode = db.get(PrefixKey.blockInfoKey(chainID, number));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }

        if (null == encode) {
            logger.info("ChainID[{}]:There is no block in this height:{}", new String(chainID), number);
            return null;
        }

        BlockInfos blockInfos = new BlockInfos(encode);
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
     * @param chainID chain ID
     * @param number  block number
     * @return block container or null if not found
     * @throws DBException database exception
     */
    @Override
    public BlockContainer getMainChainBlockContainerByNumber(byte[] chainID, long number) throws DBException {
        Block block = getMainChainBlockByNumber(chainID, number);

        if (null == block) {
            return null;
        }

        BlockContainer blockContainer = new BlockContainer(block);

        if (null != block.getHorizontalHash()) {
            HorizontalItem horizontalItem = getHorizontalItemByHash(chainID, block.getHorizontalHash());
            if (null != horizontalItem) {
                blockContainer.setHorizontalItem(horizontalItem);

                byte[] txHash = horizontalItem.getTxHash();
                if (null != txHash) {
                    Transaction tx = getTransactionByHash(chainID, txHash);
                    if (null != tx) {
                        blockContainer.setTx(tx);
                    } else {
                        return null;
                    }
                }
            } else {
                logger.info("ChainID[{}]:Cannot find horizontal item by hash:{}",
                        new String(chainID), Hex.toHexString(block.getHorizontalHash()));
                return null;
            }
        }

        if (null != block.getVerticalHash()) {
            VerticalItem verticalItem = getVerticalItemByHash(chainID, block.getVerticalHash());
            if (null != verticalItem) {
                blockContainer.setVerticalItem(verticalItem);
            } else {
                logger.info("ChainID[{}]:Cannot find vertical item by hash:{}",
                        new String(chainID), Hex.toHexString(block.getVerticalHash()));
                return null;
            }
        }

        return blockContainer;
    }

    /**
     * get main chain block hash by number
     *
     * @param chainID chain ID
     * @param number  block number
     * @return block hash or null if not found
     * @throws DBException database exception
     */
    @Override
    public byte[] getMainChainBlockHashByNumber(byte[] chainID, long number) throws DBException {
        byte[] encode;
        try {
            encode = db.get(PrefixKey.blockInfoKey(chainID, number));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }

        if (null == encode) {
            logger.info("ChainID[{}]:There is no block in this height:{}",
                    new String(chainID), number);
            return null;
        }

        BlockInfos blockInfos = new BlockInfos(encode);
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
     * @throws DBException database exception
     */
    private void saveBlockInfo(byte[] chainID, Block block, boolean isMainChain) throws DBException {
        BlockInfos blockInfos;
        // if saved in the same height
        byte[] encode;
        try {
            encode = db.get(PrefixKey.blockInfoKey(chainID, block.getBlockNum()));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }

        if (null == encode) {
            blockInfos = new BlockInfos();
        } else {
            blockInfos = new BlockInfos(encode);
        }
        blockInfos.putBlock(block, isMainChain);

        try {
            db.put(PrefixKey.blockInfoKey(chainID, block.getBlockNum()), blockInfos.getEncoded());
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * save block
     *
     * @param chainID chain ID
     * @param block block to save
     * @param isMainChain if on main chain
     * @throws DBException database exception
     */
    @Override
    public void saveBlock(byte[] chainID, Block block, boolean isMainChain) throws DBException {
        // save block
        try {
            db.put(PrefixKey.blockKey(chainID, block.getBlockHash()), block.getEncoded());
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }

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
     * @param isMainChain if on main chain
     * @throws DBException database exception
     */
    @Override
    public void saveBlockContainer(byte[] chainID, BlockContainer blockContainer, boolean isMainChain) throws DBException {
        Block block = blockContainer.getBlock();

        // save block
        try {
            db.put(PrefixKey.blockKey(chainID, block.getBlockHash()), block.getEncoded());
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }

        saveHashListItem(chainID, blockContainer.getHorizontalItem());
        saveHashListItem(chainID, blockContainer.getVerticalItem());

        // save tx
        saveTransaction(chainID, blockContainer.getTx());

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
     * @throws DBException database exception
     */
    private void delForkChainBlockByNumber(byte[] chainID, long number) throws DBException {
        try {
            byte[] encode = db.get(PrefixKey.blockInfoKey(chainID, number));
            if (null == encode) {
                logger.info("ChainID[{}]: There is no block in this height:{}", new String(chainID), number);
                return;
            }

            BlockInfos blockInfos = new BlockInfos(encode);
            List<BlockInfo> list = blockInfos.getBlockInfoList();
            for (int i = list.size() - 1; i >= 0; i--) {
                if (!list.get(i).isMainChain()) {
                    // delete tx
                    Block block = getBlockByHash(chainID, list.get(i).getHash());
                    if (null != block) {
                        if (null != block.getVerticalHash()) {
                            db.delete(PrefixKey.hashListKey(chainID, block.getVerticalHash()));
                        }

                        if (null != block.getHorizontalHash()) {
                            HorizontalItem horizontalItem = getHorizontalItemByHash(chainID, block.getHorizontalHash());
                            if (null != horizontalItem) {
                                if (null != horizontalItem.getTxHash()) {
                                    db.delete(PrefixKey.txKey(chainID, horizontalItem.getTxHash()));
                                }
                                db.delete(PrefixKey.hashListKey(chainID, block.getHorizontalHash()));
                            }
                        }
                    }
                    // delete non-main chain block
                    db.delete(PrefixKey.blockKey(chainID, list.get(i).getHash()));
                    list.remove(i);
                }
            }

            // save or delete main chain block info
            byte[] infosEncode = blockInfos.getEncoded();
            if (null == infosEncode) {
                db.delete(PrefixKey.blockInfoKey(chainID, number));
            } else {
                db.put(PrefixKey.blockInfoKey(chainID, number), blockInfos.getEncoded());
            }
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * get all blocks of a chain, whether it is a block on the main chain or not
     * @param chainID chain ID
     * @return block set on the chain or empty set otherwise
     * @throws DBException database exception
     */
    @Override
    public Set<Block> getChainAllBlocks(byte[] chainID) throws DBException {
        Set<byte[]> blocks;

        try {
            blocks = db.retrieveKeysWithPrefix(PrefixKey.blockPrefix(chainID));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }

        Set<Block> set = new HashSet<>();

        if (null == blocks) {
            logger.info("ChainID[{}]: Cannot find any block", new String(chainID));
            return set;
        }


        for (byte[] encode: blocks) {
            set.add(new Block(encode));
        }

        return set;
    }

    /**
     * remove all chain info
     * @param chainID chain ID
     * @throws DBException database exception
     */
    @Override
    public void removeChainInfo(byte[] chainID) throws DBException {
        try {
            db.removeWithKeyPrefix(chainID);
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * remove block info of a chain
     * @param chainID chain ID
     * @throws DBException database exception
     */
    @Override
    public void removeChainBlockInfo(byte[] chainID) throws DBException {
        try {
            db.removeWithKeyPrefix(PrefixKey.blockInfoPrefix(chainID));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * get fork point block between main chain and fork chain
     *
     * @param chainID chain ID
     * @param block block on chain
     * @return fork block or null otherwise
     * @throws DBException database exception
     */
//    @Override
//    public Block getForkPointBlock(byte[] chainID, Block block) throws DBException {
//
//        // if on main chain
//        byte[] infoEncode;
//        try {
//            infoEncode = db.get(PrefixKey.blockInfoKey(chainID, block.getBlockNum()));
//        } catch (Exception e) {
//            throw new DBException(e.getMessage());
//        }
//
//        if (null != infoEncode) {
//            BlockInfos blockInfos = new BlockInfos(infoEncode);
//            List<BlockInfo> list = blockInfos.getBlockInfoList();
//
//            for (BlockInfo blockInfo : list) {
//                if (Arrays.equals(blockInfo.getHash(), block.getBlockHash()) && blockInfo.isMainChain()) {
//                    // return itself
//                    return block;
//                }
//            }
//        } else {
//            logger.info("ChainID[{}]: Cannot find block info with current block.", new String(chainID));
//        }
//
//        byte[] encode;
//        try {
//            encode = db.get(PrefixKey.blockKey(chainID, block.getPreviousBlockHash()));
//        } catch (Exception e) {
//            throw new DBException(e.getMessage());
//        }
//
//        // if previous is null, return
//        if (null == encode) {
//            logger.error("ChainID[{}]: Cannot find previous block, hash[{}]",
//                    new String(chainID), Hex.toHexString(block.getPreviousBlockHash()));
//            return null;
//        }
//
//        Block previousBlock = new Block(encode);
//        while (true) {
//            try {
//                infoEncode = db.get(PrefixKey.blockInfoKey(chainID, previousBlock.getBlockNum()));
//            } catch (Exception e) {
//                throw new DBException(e.getMessage());
//            }
//
//            if (null == infoEncode) {
//                logger.error("ChainID[{}]: Cannot find block info with this block number:{}",
//                        new String(chainID), previousBlock.getBlockNum());
//                return null;
//            }
//            BlockInfos blockInfos = new BlockInfos(infoEncode);
//            List<BlockInfo> list = blockInfos.getBlockInfoList();
//            boolean found = false;
//            for (BlockInfo blockInfo : list) {
//                if (Arrays.equals(blockInfo.getHash(), previousBlock.getBlockHash())) {
//                    // found block
//                    found = true;
//                    if (blockInfo.isMainChain()) {
//                        // if this block is on main chain, got it
//                        return previousBlock;
//                    } else {
//                        // if this block is not on main chain, look ahead
//                        try {
//                            encode = db.get(PrefixKey.blockKey(chainID, previousBlock.getPreviousBlockHash()));
//                        } catch (Exception e) {
//                            throw new DBException(e.getMessage());
//                        }
//
//                        // if previous is null, return
//                        if (null == encode) {
//                            logger.error("ChainID[{}]: Cannot find previous block, hash[{}].",
//                                    new String(chainID), Hex.toHexString(previousBlock.getPreviousBlockHash()));
//                            return null;
//                        }
//
//                        logger.info("ChainID[{}]: Seek previous block hash[{}].",
//                                new String(chainID), Hex.toHexString(previousBlock.getPreviousBlockHash()));
//                        previousBlock = new Block(encode);
//                        break;
//                    }
//                }
//            }
//
//            // if not found this block info in this height
//            if (!found) {
//                logger.error("ChainID[{}]: Cannot find block info, hash[{}]",
//                        new String(chainID), Hex.toHexString(previousBlock.getBlockHash()));
//                return null;
//            }
//        }
//    }

    /**
     * get fork point block between chain 1 and chain 2
     *
     * @param chainID chain ID
     * @param chain1Block block on chain 1
     * @param chain2Block block on chain 2
     * @return fork point block or null if not found
     * @throws DBException database exception
     */
//    @Override
//    public Block getForkPointBlock(byte[] chainID, Block chain1Block, Block chain2Block) throws DBException {
//
//        // 1. First ensure that you are one the save level
//        long currentLevel = Math.max(chain1Block.getBlockNum(), chain2Block.getBlockNum());
//
//        Block chain1Line = chain1Block;
//        if (chain1Block.getBlockNum() > chain2Block.getBlockNum()) {
//
//            while (currentLevel > chain2Block.getBlockNum()) {
//                chain1Line = getBlockByHash(chainID, chain1Line.getPreviousBlockHash());
//                if (chain1Line == null)
//                    return null;
//                --currentLevel;
//            }
//        }
//
//        Block chain2Line = chain2Block;
//        if (chain2Block.getBlockNum() > chain1Block.getBlockNum()) {
//
//            while (currentLevel > chain1Block.getBlockNum()) {
//                chain2Line = getBlockByHash(chainID, chain2Line.getPreviousBlockHash());
//                if (chain2Line == null)
//                    return null;
//                --currentLevel;
//            }
//        }
//
//        // 2. Loop back on each level until common block
//        while (!Arrays.equals(chain2Line.getBlockHash(), chain1Line.getBlockHash())) {
//            chain2Line = getBlockByHash(chainID, chain2Line.getPreviousBlockHash());
//            chain1Line = getBlockByHash(chainID, chain1Line.getPreviousBlockHash());
//
//            if (chain1Line == null || chain2Line == null)
//                return null;
//        }
//
//        if (Arrays.equals(chain2Line.getBlockHash(), chain1Line.getBlockHash())) {
//            return chain1Block;
//        }
//
//        return null;
//    }

    /**
     * get fork block info
     *
     * @param chainID chain ID
     * @param forkBlock  fork point block
     * @param bestBlock current chain best block
     * @param undoBlocks blocks to roll back from high to low
     * @param newBlocks  blocks to connect from high to low
     * @return true if find fork info, false otherwise
     * @throws DBException database exception
     */
//    @Override
//    public boolean getForkBlocksInfo(byte[] chainID, Block forkBlock, Block bestBlock,
//                                     List<Block> undoBlocks, List<Block> newBlocks) throws DBException {
//
//        // 1. First ensure that you are one the save level
//        long currentLevel = Math.max(bestBlock.getBlockNum(), forkBlock.getBlockNum());
//
//        Block forkLine = forkBlock;
//        if (forkBlock.getBlockNum() > bestBlock.getBlockNum()) {
//
//            while (currentLevel > bestBlock.getBlockNum()) {
//                newBlocks.add(forkLine);
//
//                forkLine = getBlockByHash(chainID, forkLine.getPreviousBlockHash());
//                if (forkLine == null)
//                    return false;
//                --currentLevel;
//            }
//        }
//
//        Block bestLine = bestBlock;
//        if (bestBlock.getBlockNum() > forkBlock.getBlockNum()) {
//
//            while (currentLevel > forkBlock.getBlockNum()) {
//                undoBlocks.add(bestLine);
//
//                bestLine = getBlockByHash(chainID, bestLine.getPreviousBlockHash());
//                --currentLevel;
//            }
//        }
//
//        // 2. Loop back on each level until common block
//        while (!Arrays.equals(bestLine.getBlockHash(), forkLine.getBlockHash())) {
//            newBlocks.add(forkLine);
//            undoBlocks.add(bestLine);
//
//            bestLine = getBlockByHash(chainID, bestLine.getPreviousBlockHash());
//            forkLine = getBlockByHash(chainID, forkLine.getPreviousBlockHash());
//
//            if (forkLine == null)
//                return false;
//
//            --currentLevel;
//        }
//
//        return true;
//    }

    /**
     * get fork block container info
     *
     * @param chainID             chain ID
     * @param forkBlockContainer  fork point block container
     * @param bestBlockContainer  current chain best block container
     * @param undoBlockContainers block containers to roll back from high to low
     * @param newBlockContainers  block containers to connect from high to low
     * @return true/false
     * @throws DBException database exception
     */
    @Override
    public boolean getForkBlockContainersInfo(byte[] chainID,
                                              BlockContainer forkBlockContainer,
                                              BlockContainer bestBlockContainer,
                                              List<BlockContainer> undoBlockContainers,
                                              List<BlockContainer> newBlockContainers) throws DBException {

        // 1. First ensure that you are one the save level
        long currentLevel = Math.max(bestBlockContainer.getBlock().getBlockNum(),
                forkBlockContainer.getBlock().getBlockNum());
        
        BlockContainer forkLine = forkBlockContainer;
        if (forkBlockContainer.getBlock().getBlockNum() > bestBlockContainer.getBlock().getBlockNum()) {

            while (currentLevel > bestBlockContainer.getBlock().getBlockNum()) {
                newBlockContainers.add(forkLine);

                forkLine = getBlockContainerByHash(chainID, forkLine.getVerticalItem().getPreviousHash());
                if (forkLine == null)
                    return false;
                --currentLevel;
            }
        }

        BlockContainer bestLine = bestBlockContainer;
        if (bestBlockContainer.getBlock().getBlockNum() > forkBlockContainer.getBlock().getBlockNum()) {

            while (currentLevel > forkBlockContainer.getBlock().getBlockNum()) {
                undoBlockContainers.add(bestLine);

                bestLine = getBlockContainerByHash(chainID, bestLine.getVerticalItem().getPreviousHash());
                --currentLevel;
            }
        }

        // 2. Loop back on each level until common block
        while (!Arrays.equals(bestLine.getBlock().getBlockHash(), forkLine.getBlock().getBlockHash())) {
            if (0 == forkLine.getBlock().getBlockNum()) {
                return false;
            }

            newBlockContainers.add(forkLine);
            undoBlockContainers.add(bestLine);

            bestLine = getBlockContainerByHash(chainID, bestLine.getVerticalItem().getPreviousHash());
            forkLine = getBlockContainerByHash(chainID, forkLine.getVerticalItem().getPreviousHash());

            if (forkLine == null)
                return false;

            --currentLevel;
        }

        return true;
    }

    /**
     * re-branch blocks
     *
     * @param chainID chain ID
     * @param undoBlocks move to non-main chain
     * @param newBlocks  move to main chain
     * @throws DBException database exception
     */
//    @Override
//    public void reBranchBlocks(byte[] chainID, List<Block> undoBlocks, List<Block> newBlocks) throws DBException {
//        if (undoBlocks != null) {
//            for (Block block : undoBlocks) {
//                saveBlockInfo(chainID, block, false);
//            }
//        }
//
//        if (newBlocks != null) {
//            for (Block block : newBlocks) {
//                saveBlockInfo(chainID, block, true);
//            }
//        }
//    }

    /**
     * re-branch blocks with block containers
     *
     * @param chainID             chain ID
     * @param undoBlockContainers move to non-main chain
     * @param newBlockContainers  move to main chain
     * @throws DBException database exception
     */
    @Override
    public void reBranchBlocksWithContainers(byte[] chainID,
                                             List<BlockContainer> undoBlockContainers,
                                             List<BlockContainer> newBlockContainers) throws DBException {
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

