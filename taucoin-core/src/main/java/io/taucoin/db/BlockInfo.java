package io.taucoin.db;

import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BlockInfo blockInfo = (BlockInfo) o;

        if (mainChain != blockInfo.mainChain) return false;
        return Arrays.equals(hash, blockInfo.hash);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(hash);
        result = 31 * result + mainChain;
        return result;
    }

    @Override
    public String toString() {
        return "BlockInfo{" +
                "hash=" + Hex.toHexString(hash) +
                ", mainChain=" + (0 == mainChain) +
                '}';
    }
}

