package io.taucoin.db;

import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

import java.math.BigInteger;

public class BlockInfo {
    // block hash
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

        byte[] mainChainBytes = blockInfo.get(1).getRLPData();
        this.mainChain = ByteUtil.byteArrayToInt(mainChainBytes);
    }

    public BlockInfo(byte[] hash, boolean isMainChain) {
        this.hash = hash;
        if (isMainChain) {
            this.mainChain = 0;
        } else {
            this.mainChain = 1;
        }
    }

    /**
     * get block hash
     * @return
     */
    public byte[] getHash() {
        return this.hash;
    }

    /**
     * set block hash
     * @param hash
     */
    public void setHash(byte[] hash) {
        this.hash = hash;
    }

    /**
     * if main chain or not
     * @return
     */
    public boolean isMainChain() {
        return 0 == mainChain;
    }

    /**
     * set if main chain
     * @param isMainChain
     */
    public void setMainChain(boolean isMainChain) {
        if (isMainChain) {
            this.mainChain = 0;
        } else {
            this.mainChain = 1;
        }
    }

    /**
     * get rlp encode
     * @return
     */
    public byte[] getEncoded() {
        byte[] hash = RLP.encodeElement(this.hash);
        byte[] mainChain = RLP.encodeBigInteger(BigInteger.valueOf(this.mainChain));
        return RLP.encodeList(hash, mainChain);
    }
}

