package io.taucoin.processor;

import io.taucoin.types.BlockContainer;
import io.taucoin.core.ImportResult;
import io.taucoin.db.StateDB;

public interface StateProcessor {
    /**
     * forward process block, when sync new block
     * @param blockContainer block to be processed
     * @param stateDB: state db
     * @return import result
     */
    ImportResult forwardProcess(BlockContainer blockContainer, StateDB stateDB);

    /**
     * backward process block, when sync old block
     * @param blockContainer block to be processed
     * @param stateDB state db
     * @return import result
     */
    ImportResult backwardProcess(BlockContainer blockContainer, StateDB stateDB);

    /**
     * sync genesis block
     * @param blockContainer genesis block
     * @param stateDB state db
     * @return import result
     */
    ImportResult backwardProcessGenesisBlock(BlockContainer blockContainer, StateDB stateDB);

    /**
     * roll back a block
     * @param blockContainer block container
     * @param stateDB state db
     * @return true if succeed, false otherwise
     */
    boolean rollback(BlockContainer blockContainer, StateDB stateDB);
}
