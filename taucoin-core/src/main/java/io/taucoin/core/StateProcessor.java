package io.taucoin.core;

import io.taucoin.db.Repository;
import io.taucoin.types.Block;

public interface StateProcessor {
    /**
     * process block
     * @param block to be processed
     * @param repository: state db
     * @return
     */
    boolean process(Block block, Repository repository);
}
