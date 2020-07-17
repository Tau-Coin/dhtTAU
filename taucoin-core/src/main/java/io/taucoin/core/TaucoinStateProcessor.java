package io.taucoin.core;

import io.taucoin.db.StateDB;
import io.taucoin.types.Block;

public class TaucoinStateProcessor implements StateProcessor {
    /**
     * process block
     *
     * @param block      to be processed
     * @param stateDB : state db
     * @return
     */
    @Override
    public boolean process(Block block, StateDB stateDB) {
        return false;
    }


    /**
     * roll back a block
     *
     * @param block
     * @param stateDB
     * @return
     */
    @Override
    public boolean rollback(Block block, StateDB stateDB) {
        return false;
    }
}
