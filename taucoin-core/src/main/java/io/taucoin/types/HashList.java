package io.taucoin.types;

import java.util.ArrayList;
import java.util.List;

import io.taucoin.util.HashUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class HashList {
    private List<byte[]> hashList;

    private byte[] hash;
    private byte[] rlpEncoded;

    private boolean parsed = false;

    public HashList(List<byte[]> hashList) {
        this.hashList = hashList;
        this.parsed = true;
    }

    public HashList(byte[] encode) {
        this.rlpEncoded = encode;
    }

    /**
     * get hash list
     * @return hash list
     */
    public List<byte[]> getHashList() {
        if (!parsed) {
            parseRLP();
        }

        return this.hashList;
    }

    /**
     * parse rlp encode
     */
    private void parseRLP() {
        RLPList params = RLP.decode2(this.rlpEncoded);
        RLPList list = (RLPList) params.get(0);

        this.hashList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            byte[] hashByte = list.get(i).getRLPData();
            if (null != hashByte) {
                this.hashList.add(hashByte);
            }
        }

        this.parsed = true;
    }

    /**
     * get encoded hash list
     * @return encode
     */
    public byte[] getEncoded(){
        if (null == rlpEncoded) {
            if (null != this.hashList && this.hashList.size() > 1) {
                byte[][] encodeList = new byte[this.hashList.size()][];

                int i = 0;
                for (byte[] hash : this.hashList) {
                    encodeList[i] = RLP.encodeElement(hash);
                    i++;
                }

                rlpEncoded = RLP.encodeList(encodeList);
            }
        }

        return rlpEncoded;
    }

    /**
     * get hash list item hash
     * @return hash
     */
    public byte[] getHash(){
        if(null != this.hash) {
            this.hash = HashUtil.sha1hash(this.getEncoded());
        }

        return this.hash;
    }


}
