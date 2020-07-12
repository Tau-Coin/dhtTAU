package io.taucoin.core;

import io.taucoin.db.Repository;
import io.taucoin.types.Block;

public class TaucoinStateProcessor implements StateProcessor {
    /**
     * process block
     *
     * @param block      to be processed
     * @param repository : state db
     * @return
     */
    @Override
    public boolean process(Block block, Repository repository) {
        return false;
    }
}
