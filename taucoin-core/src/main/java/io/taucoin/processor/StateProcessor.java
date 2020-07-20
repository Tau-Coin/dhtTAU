package io.taucoin.processor;

import io.taucoin.db.StateDB;
import io.taucoin.types.Block;

public interface StateProcessor {
    /**
     * process block
     * @param block to be processed
     * @param stateDB: state db
     * @return
     */
    boolean process(Block block, StateDB stateDB);

    /**
     * roll back a block
     * @param block
     * @param stateDB
     * @return
     */
    boolean rollback(Block block, StateDB stateDB);
}
