package io.taucoin.db;

import io.taucoin.types.Block;

import java.util.List;
import java.util.Set;

public interface BlockStore {

    /**
     * Open database.
     *
     * @param path database path which can be accessed
     * @throws Exception
     */
    void open(String path) throws Exception;

    /**
     * Close database.
     */
    void close();

    /**
     * get block by hash
     * @param chainID
     * @param hash
     * @return
     * @throws Exception
     */
    Block getBlockByHash(byte[] chainID, byte[] hash) throws Exception;

    /**
     * get block info by hash
     * @param chainID
     * @param hash
     * @return
     * @throws Exception
     */
    BlockInfo getBlockInfoByHash(byte[] chainID, byte[] hash) throws Exception;

    /**
     * get main chain block by number
     * @param chainID
     * @param number
     * @return
     * @throws Exception
     */
    Block getMainChainBlockByNumber(byte[] chainID, long number) throws Exception;

    /**
     * get main chain block hash by number
     * @param chainID chain ID
     * @param number block number
     * @return block hash
     * @throws Exception
     */
    byte[] getMainChainBlockHashByNumber(byte[] chainID, long number) throws Exception;

    /**
     * save block
     * @param block
     * @throws Exception
     */
    void saveBlock(Block block, boolean isMainChain) throws Exception;

    /**
     * get all blocks of a chain, whether it is a block on the main chain or not
     * @param chainID
     * @return
     * @throws Exception
     */
    Set<Block> getChainAllBlocks(byte[] chainID) throws Exception;

    /**
     * remove all blocks and info of a chain
     * @param chainID
     * @throws Exception
     */
    void removeChain(byte[] chainID) throws Exception;

    /**
     * get fork point block which on main chain
     * @param block
     * @return
     * @throws Exception
     */
    Block getForkPointBlock(Block block) throws Exception;

    /**
     * get fork info
     * @param forkBlock fork point block
     * @param undoBlocks blocks to roll back
     * @param newBlocks blocks to connect
     * @return
     */
    boolean getForkBlocksInfo(Block forkBlock, List<Block> undoBlocks, List<Block> newBlocks);

    /**
     * re-branch blocks
     * @param undoBlocks move to non-main chain
     * @param newBlocks move to main chain
     */
    void reBranchBlocks(List<Block> undoBlocks, List<Block> newBlocks);
}
