package io.taucoin.types;

public class BlockContainer {
    private Block block;
    private Transaction tx;

    public BlockContainer() {
    }

    public BlockContainer(Block block) {
        this.block = block;
    }

    public BlockContainer(Block block, Transaction tx) {
        this.block = block;
        this.tx = tx;
    }

    public static BlockContainer with(BlockContainer blockContainer) {
        return new BlockContainer(blockContainer.block, blockContainer.tx);
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

    public boolean isEmpty() {
        return null == block;
    }

    public void copy(BlockContainer blockContainer) {
        this.block = blockContainer.block;
        this.tx = blockContainer.tx;
    }
}
