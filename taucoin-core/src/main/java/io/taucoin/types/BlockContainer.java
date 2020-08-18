package io.taucoin.types;

public class BlockContainer {
    private Block block;
    private Transaction tx;

    public BlockContainer(Block block) {
        this.block = block;
    }

    public BlockContainer(Block block, Transaction tx) {
        this.block = block;
        this.tx = tx;
    }

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public Transaction getTx() {
        return tx;
    }

    public void setTx(Transaction tx) {
        this.tx = tx;
    }
}
