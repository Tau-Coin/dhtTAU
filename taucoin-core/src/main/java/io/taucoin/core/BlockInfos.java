package io.taucoin.core;

import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class BlockInfos {
    private List<BlockInfo> blockInfoList = new ArrayList<>();

    public List<BlockInfo> getBlockInfoList() {
        return this.blockInfoList;
    }

    public static class BlockInfo {
        byte[] hash;

        /*
            0: main chain
            others, default 1: non-main chain
         */
        int mainChain;

        private BlockInfo() {
        }

        public BlockInfo(byte[] rlp) {
            RLPList decodedBlockInfoList = RLP.decode2(rlp);
            RLPList blockInfo = (RLPList) decodedBlockInfoList.get(0);

            this.hash = blockInfo.get(0).getRLPData();

            byte[] mainChaineBytes = blockInfo.get(1).getRLPData();
            this.mainChain = ByteUtil.byteArrayToInt(mainChaineBytes);
        }

        public BlockInfo(byte[] hash, boolean mainChain) {
            this.hash = hash;
            if (mainChain) {
                this.mainChain = 0;
            } else {
                this.mainChain = 1;
            }
        }

        public byte[] getHash() {
            return hash;
        }

        public void setHash(byte[] hash) {
            this.hash = hash;
        }

        public boolean isMainChain() {
            return 0 == mainChain;
        }

        public void setMainChain() {
            this.mainChain = 0;
        }

        public byte[] getEncoded() {
            byte[] hash = RLP.encodeElement(this.hash);
            byte[] mainChain = RLP.encodeBigInteger(BigInteger.valueOf(this.mainChain));
            return RLP.encodeList(hash, mainChain);
        }
    }
}
