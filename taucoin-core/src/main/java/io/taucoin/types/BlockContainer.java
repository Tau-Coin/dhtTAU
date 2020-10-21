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

}
