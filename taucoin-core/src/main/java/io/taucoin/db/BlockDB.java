package io.taucoin.db;

import io.taucoin.types.Block;

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
     * save block
     * @param block
     * @throws Exception
     */
    @Override
    public void saveBlock(Block block) throws Exception {
        db.put(PrefixKey.blockKey(block.getChainID(), block.getBlockHash()), block.getEncoded());
    }

    /**
     * remove all blocks of a chain
     * @param chainID
     * @throws Exception
     */
    @Override
    public void removeChainAllBlocks(byte[] chainID) throws Exception {
        db.removeWithKeyPrefix(chainID);
    }
}
