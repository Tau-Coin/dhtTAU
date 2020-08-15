package io.taucoin.core;

import javax.annotation.Nonnull;

import io.taucoin.types.Block;
import io.taucoin.types.Transaction;

public class BlockContainer {
    private Block block;
    private Transaction tx;

    public BlockContainer(@Nonnull Block block) {
        this.block = block;
    }

    public BlockContainer(@Nonnull Block block, Transaction tx) {
        this.block = block;
        this.tx = tx;
    }

    public Block getBlock() {
        return block;
    }

    public Transaction getTx() {
        return tx;
    }

    public void setTx(Transaction tx) {
        this.tx = tx;
    }
}
