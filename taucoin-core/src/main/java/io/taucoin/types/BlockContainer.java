package io.taucoin.types;

public class BlockContainer {
    private Block block;
    private VerticalItem verticalItem;
    private HorizontalItem horizontalItem;
    private Transaction tx;

    public BlockContainer() {
    }

    public BlockContainer(Block block) {
        this.block = block;
    }

    // TODO:: del
    public BlockContainer(Block block, Transaction tx) {
        this.block = block;
        this.tx = tx;
    }

    public BlockContainer(Block block, VerticalItem verticalItem) {
        this.block = block;
        this.verticalItem = verticalItem;
    }

    public BlockContainer(Block block, VerticalItem verticalItem, HorizontalItem horizontalItem, Transaction tx) {
        this.block = block;
        this.verticalItem = verticalItem;
        this.horizontalItem = horizontalItem;
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

    public VerticalItem getVerticalItem() {
        return verticalItem;
    }

    public void setVerticalItem(VerticalItem verticalItem) {
        this.verticalItem = verticalItem;
    }

    public HorizontalItem getHorizontalItem() {
        return horizontalItem;
    }

    public void setHorizontalItem(HorizontalItem horizontalItem) {
        this.horizontalItem = horizontalItem;
    }

    public boolean isEmpty() {
        return null == block;
    }

    public void copy(BlockContainer blockContainer) {
        this.block = blockContainer.block;
        this.tx = blockContainer.tx;
    }
}
