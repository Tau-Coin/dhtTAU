package io.taucoin.processor;

import io.taucoin.core.ImportResult;
import io.taucoin.db.StateDB;
import io.taucoin.types.Block;

public interface StateProcessor {
    /**
     * forward process block, when sync new block
     * @param block block to be processed
     * @param stateDB: state db
     * @return
     */
    ImportResult forwardProcess(Block block, StateDB stateDB);

    /**
     * backward process block, when sync old block
     * @param block block to be processed
     * @param stateDB state db
     * @return
     */
    boolean backwardProcess(Block block, StateDB stateDB);

    /**
     * roll back a block
     * @param block
     * @param stateDB
     * @return
     */
    boolean rollback(Block block, StateDB stateDB);
}
