package io.taucoin.torrent.publishing.core.model.data;

import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class MsgBlock {
    private byte[] verticalHash;   // 代表后面的MsgBlock的Hash
    private byte[] horizontalHash; // 如果是文本代表1个切片Hash，如果是图片代表40个切片Hash

    private byte[] rlpEncoded;
    private boolean isParsed = false;

    public byte[] getHorizontalHash() {
        if (!isParsed) {
            parseEncodedBytes();
        }
        return horizontalHash;
    }

    public MsgBlock() {
    }

    public MsgBlock(byte[] encoded) {
        this.rlpEncoded = encoded;
        isParsed = false;
    }

    public void setHorizontalHash(byte[] horizontalHash) {
        isParsed = true;
        rlpEncoded = null;
        this.horizontalHash = horizontalHash;
    }

    public byte[] getVerticalHash() {
        if (!isParsed) {
            parseEncodedBytes();
        }
        return verticalHash;
    }

    public void setVerticalHash(byte[] verticalHash) {
        isParsed = true;
        rlpEncoded = null;
        this.verticalHash = verticalHash;
    }

    public boolean isHaveVerticalHash() {
        if (!isParsed) {
            parseEncodedBytes();
        }
        return verticalHash != null;
    }

    public boolean isHaveHorizontalHash() {
        if (!isParsed) {
            parseEncodedBytes();
        }
        return horizontalHash != null;
    }

    /**
     * get encoded msg block
     * @return encode
     */
    public byte[] getEncoded(){
        if (null == rlpEncoded) {
            byte[] verticalHash = RLP.encodeElement(this.verticalHash);
            byte[] horizontalHash = RLP.encodeElement(this.horizontalHash);
            rlpEncoded = RLP.encodeList(verticalHash, horizontalHash);
        }
        return rlpEncoded;
    }

    /**
    * parse msg block bytes field to flat block field.
    */
    private void parseEncodedBytes(){
        if (isParsed || null == this.rlpEncoded) {
            return;
        } else {
            RLPList blockList = RLP.decode2(this.rlpEncoded);
            RLPList block = (RLPList) blockList.get(0);

            this.verticalHash = block.get(0).getRLPData();
            this.horizontalHash = block.get(1).getRLPData();
        }
        isParsed = true;
    }
}