package io.taucoin.db;

import io.taucoin.types.Block;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockInfos {
    private static final Logger logger = LoggerFactory.getLogger("BlockInfos");

    private List<BlockInfo> blockInfoList;

    public BlockInfos() {
    }

    public BlockInfos(byte[] rlp) {
        if (null != rlp) {
            this.blockInfoList = new ArrayList<>();
            RLPList list = (RLPList) RLP.decode2(rlp).get(0);
            for (int i = 0; i < list.size(); i++) {
                BlockInfo blockInfo = new BlockInfo(list.get(i).getRLPData());
                this.blockInfoList.add(blockInfo);
            }
        }
    }

    /**
     * get block info list
     * @return
     */
    public List<BlockInfo> getBlockInfoList() {
        return this.blockInfoList;
    }

    /**
     * put a block into block info list
     * @param block
     * @param isMainChain
     */
    public void putBlock(Block block, boolean isMainChain) {
        if (null == blockInfoList) {
            blockInfoList = new ArrayList<>();
        }
        boolean found = false;
        for (BlockInfo blockInfo: blockInfoList) {
            if (Arrays.equals(blockInfo.getHash(), block.getBlockHash())) {
                // if found, overwrite it with new status
                blockInfo.setMainChain(isMainChain);
                found = true;
            }
        }
        // if not found, add a new one
        if (!found) {
            BlockInfo blockInfo = new BlockInfo(block.getBlockHash(), isMainChain);
            blockInfoList.add(blockInfo);
        }
    }

    /**
     * get rlp encode
     * @return encode
     */
    public byte[] getEncoded() {
        if (null == blockInfoList) {
            logger.error("Block Info is null");
            return null;
        }

        if (blockInfoList.isEmpty()) {
            logger.error("Block Info is empty");
            return null;
        }

        byte[][] blockInfosEncoded = new byte[blockInfoList.size()][];
        int i = 0;
        for (BlockInfo blockInfo: blockInfoList) {
            blockInfosEncoded[i] = blockInfo.getEncoded();
            i++;
        }
        return RLP.encodeList(blockInfosEncoded);
    }
}

