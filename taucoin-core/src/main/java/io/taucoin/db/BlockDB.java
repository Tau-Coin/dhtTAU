package io.taucoin.db;

import io.taucoin.types.Block;

import java.util.List;

public class BlockDB implements BlockStore{

    private KeyValueDataBase db;

    public BlockDB(KeyValueDataBase db) {
        this.db = db;
    }

    /**
     * open db
     * @param path database path which can be accessed
     * @throws Exception
     */
    public void open(String path) throws Exception {
        db.open(path);
    }

    /**
     * close db
     */
    public void close() {
        db.close();
    }

    /**
     * get block by hash
     * @param chainID
     * @param hash
     * @return
     * @throws Exception
     */
    @Override
    public Block getBlockByHash(byte[] chainID, byte[] hash) throws Exception {
        byte[] rlp = db.get(PrefixKey.blockKey(chainID, hash));
        if (null != rlp) {
            return new Block(rlp);
        }
        return null;
    }

    /**
     * save block info in db
     * @param block
     * @param isMainChain
     * @throws Exception
     */
    public void saveBlockInfo(Block block, boolean isMainChain) throws Exception {
        BlockInfos blockInfos;
        // if saved in the same height
        byte[] rlp = db.get(PrefixKey.blockInfoKey(block.getChainID(), block.getBlockNum()));
        if (null == rlp) {
            blockInfos = new BlockInfos();
        } else {
            blockInfos = new BlockInfos(rlp);
        }
        blockInfos.putBlock(block, isMainChain);

        db.put(PrefixKey.blockInfoKey(block.getChainID(), block.getBlockNum()), blockInfos.getEncoded());
    }

    /**
     * save block
     * @param block
     * @throws Exception
     */
    @Override
    public void saveBlock(Block block, boolean isMainChain) throws Exception {
        // save block
        db.put(PrefixKey.blockKey(block.getChainID(), block.getBlockHash()), block.getEncoded());
        // save block info
        saveBlockInfo(block, isMainChain);
        // TODO: delete blocks out of 3 * mutable range
    }

    /**
     * delete non-main chain block and block info
     * @param chainID
     * @param number
     * @throws Exception
     */
    public void delNonChainBlockByNumber(byte[] chainID, long number) throws Exception {
        byte[] rlp = db.get(PrefixKey.blockInfoKey(chainID, number));
        if (null == rlp) {
            return;
        }
        BlockInfos blockInfos = new BlockInfos(rlp);
        List<BlockInfo> list = blockInfos.getBlockInfoList();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (!list.get(i).isMainChain()) {
                // delete non-main chain block
                db.delete(PrefixKey.blockKey(chainID, list.get(i).getHash()));
                list.remove(i);
            }
        }
        // save main chain block info
        db.put(PrefixKey.blockInfoKey(chainID, number), blockInfos.getEncoded());
    }

    /**
     * remove all blocks and info of a chain
     * @param chainID
     * @throws Exception
     */
    @Override
    public void removeChain(byte[] chainID) throws Exception {
        db.removeWithKeyPrefix(chainID);
    }
}

