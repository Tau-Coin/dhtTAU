package io.taucoin.processor;

import io.taucoin.core.BlockContainer;
import io.taucoin.core.ImportResult;
import io.taucoin.db.StateDB;
import io.taucoin.types.Block;

public interface StateProcessor {
    /**
     * forward process block, when sync new block
     * @param blockContainer block to be processed
     * @param stateDB: state db
     * @return
     */
    ImportResult forwardProcess(BlockContainer blockContainer, StateDB stateDB);

    /**
     * backward process block, when sync old block
     * @param blockContainer block to be processed
     * @param stateDB state db
     * @return
     */
    boolean backwardProcess(BlockContainer blockContainer, StateDB stateDB);

    /**
     * sync genesis block
     * @param blockContainer genesis block
     * @param stateDB state db
     * @return
     */
    boolean backwardProcessGenesisBlock(BlockContainer blockContainer, StateDB stateDB);

    /**
     * roll back a block
     * @param blockContainer
     * @param stateDB
     * @return
     */
    boolean rollback(BlockContainer blockContainer, StateDB stateDB);
}
