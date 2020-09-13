package io.taucoin.db;

import io.taucoin.types.BlockContainer;
import io.taucoin.types.Block;
import io.taucoin.types.Transaction;

import java.util.List;
import java.util.Set;

public interface BlockStore {

    /**
     * Open database.
     *
     * @param path database path which can be accessed
     * @throws DBException database exception
     */
    void open(String path) throws DBException;

    /**
     * Close database.
     */
    void close();

    /**
     * get tx by hash
     * @param chainID chain ID
     * @param hash tx hash
     * @return transaction or null
     * @throws DBException database exception
     */
    Transaction getTransactionByHash(byte[] chainID, byte[] hash) throws DBException;

    /**
     * get block by hash
     * @param chainID chain ID
     * @param hash block hash
     * @return block if found, null otherwise
     * @throws DBException database exception
     */
    Block getBlockByHash(byte[] chainID, byte[] hash) throws DBException;

    /**
     * get block container by hash
     * @param chainID chain ID
     * @param hash block hash
     * @return block container
     * @throws DBException database exception
     */
    BlockContainer getBlockContainerByHash(byte[] chainID, byte[] hash) throws DBException;

    /**
     * get block info by hash
     * @param chainID chain ID
     * @param hash block hash
     * @return block info or null if not found
     * @throws DBException database exception
     */
    BlockInfo getBlockInfoByHash(byte[] chainID, byte[] hash) throws DBException;

    /**
     * if a block hash is main chain block hash
     * @param chainID chain ID
     * @param hash block hash
     * @return true if main chain, false otherwise
     * @throws DBException  database exception
     */
    boolean isMainChainBlock(byte[] chainID, byte[] hash) throws DBException;

    /**
     * if a block is on chain
     * @param chainID chain ID
     * @param hash block hash
     * @return true if main chain, false otherwise
     * @throws DBException  database exception
     */
    boolean isBlockOnChain(byte[] chainID, byte[] hash) throws DBException;

    /**
     * get main chain block by number
     * @param chainID chain id
     * @param number block number
     * @return  block or null if not found
     * @throws DBException database exception
     */
    Block getMainChainBlockByNumber(byte[] chainID, long number) throws DBException;

    /**
     * get main chain block container by number
     * @param chainID chain id
     * @param number block number
     * @return block container or null if not found
     * @throws DBException database exception
     */
    BlockContainer getMainChainBlockContainerByNumber(byte[] chainID, long number) throws DBException;

    /**
     * get main chain block hash by number
     * @param chainID chain ID
     * @param number block number
     * @return block hash or null if not found
     * @throws DBException database exception
     */
    byte[] getMainChainBlockHashByNumber(byte[] chainID, long number) throws DBException;

    /**
     * save block
     * @param chainID chain ID
     * @param block block to save
     * @param isMainChain if on main chain
     * @throws DBException database exception
     */
    void saveBlock(byte[] chainID, Block block, boolean isMainChain) throws DBException;

    /**
     * save block container
     * @param chainID chain ID
     * @param blockContainer block container to save
     * @param isMainChain if on main chain
     * @throws DBException database exception
     */
    void saveBlockContainer(byte[] chainID, BlockContainer blockContainer, boolean isMainChain) throws DBException;

    /**
     * get all blocks of a chain, whether it is a block on the main chain or not
     * @param chainID chain ID
     * @return block set on the chain or empty set otherwise
     * @throws DBException database exception
     */
    Set<Block> getChainAllBlocks(byte[] chainID) throws DBException;

    /**
     * remove all chain info
     * @param chainID chain ID
     * @throws DBException database exception
     */
    void removeChainInfo(byte[] chainID) throws DBException;

    /**
     * remove block info of a chain
     * @param chainID chain ID
     * @throws DBException database exception
     */
    void removeChainBlockInfo(byte[] chainID) throws DBException;

    /**
     * get fork point block between main chain and fork chain
     * @param chainID chain ID
     * @param block block on chain
     * @return fork block or null otherwise
     * @throws DBException database exception
     */
    Block getForkPointBlock(byte[] chainID, Block block) throws DBException;

    /**
     * get fork point block between chain 1 and chain 2
     * @param chainID chain ID
     * @param chain1Block block on chain 1
     * @param chain2Block block on chain 2
     * @return fork point block or null if not found
     * @throws DBException database exception
     */
    Block getForkPointBlock(byte[] chainID, Block chain1Block, Block chain2Block) throws DBException;

    /**
     * get fork block info
     * @param chainID chain ID
     * @param forkBlock fork point block
     * @param bestBlock current chain best block
     * @param undoBlocks blocks to roll back from high to low
     * @param newBlocks blocks to connect from high to low
     * @return true if find fork info, false otherwise
     * @throws DBException database exception
     */
    boolean getForkBlocksInfo(byte[] chainID,
                              Block forkBlock,
                              Block bestBlock,
                              List<Block> undoBlocks,
                              List<Block> newBlocks) throws DBException;

    /**
     * get fork block container info
     * @param chainID chain ID
     * @param forkBlockContainer fork point block container
     * @param bestBlockContainer current chain best block container
     * @param undoBlockContainers block containers to roll back from high to low
     * @param newBlockContainers block containers to connect from high to low
     * @return true/false
     * @throws DBException database exception
     */
    boolean getForkBlockContainersInfo(byte[] chainID,
                                       BlockContainer forkBlockContainer,
                                       BlockContainer bestBlockContainer,
                                       List<BlockContainer> undoBlockContainers,
                                       List<BlockContainer> newBlockContainers) throws DBException;

    /**
     * re-branch blocks
     * @param chainID chain ID
     * @param undoBlocks move to non-main chain
     * @param newBlocks move to main chain
     * @throws DBException database exception
     */
    void reBranchBlocks(byte[] chainID, List<Block> undoBlocks, List<Block> newBlocks) throws DBException;

    /**
     * re-branch blocks with block containers
     * @param chainID chain ID
     * @param undoBlockContainers move to non-main chain
     * @param newBlockContainers move to main chain
     * @throws DBException database exception
     */
    void reBranchBlocksWithContainers(byte[] chainID,
                                 List<BlockContainer> undoBlockContainers,
                                 List<BlockContainer> newBlockContainers) throws DBException;
}
