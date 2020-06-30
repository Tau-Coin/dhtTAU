package io.taucoin.db;

import io.taucoin.types.Block;

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
     * save block
     * @param block
     * @throws Exception
     */
    void saveBlock(Block block, boolean isMainChain) throws Exception;

    /**
     * remove all blocks and info of a chain
     * @param chainID
     * @throws Exception
     */
    void removeChain(byte[] chainID) throws Exception;
}
