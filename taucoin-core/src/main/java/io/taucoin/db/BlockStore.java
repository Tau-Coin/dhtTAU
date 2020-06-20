package io.taucoin.db;

import io.taucoin.types.Block;

public interface BlockStore {

    Block getBlockByHash(byte[] chainID, byte[] hash);

    void saveBlock(Block block);

    void removeChainAllBlocks(byte[] chainID);
}
