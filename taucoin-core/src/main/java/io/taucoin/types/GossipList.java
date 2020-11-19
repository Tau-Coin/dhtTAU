package io.taucoin.types;

import java.util.ArrayList;
import java.util.List;

import io.taucoin.util.HashUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class GossipList {
    private List<GossipItem> gossipList;

    private byte[] hash;
    private byte[] rlpEncoded;

    private boolean parsed = false;

    public GossipList(List<GossipItem> gossipList) {
        this.gossipList = gossipList;
        this.parsed = true;
    }

    public GossipList(byte[] encode) {
        this.rlpEncoded = encode;
    }

    public List<GossipItem> getGossipList() {
        if (!parsed) {
            parseRLP();
        }

        return gossipList;
    }

    /**
     * get hash list item hash
     * @return hash
     */
    public byte[] getHash(){
        if(null == this.hash) {
            this.hash = HashUtil.bencodeHash(this.getEncoded());
        }

        return this.hash;
    }

    /**
     * parse rlp encode
     */
    private void parseRLP() {
        RLPList params = RLP.decode2(this.rlpEncoded);
        RLPList list = (RLPList) params.get(0);

        this.gossipList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            byte[] encode = list.get(i).getRLPData();
            this.gossipList.add(new GossipItem(encode));
        }

        this.parsed = true;
    }

    /**
     * get encoded hash list
     * @return encode
     */
    public byte[] getEncoded(){
        if (null == rlpEncoded) {
            if (null != this.gossipList && !this.gossipList.isEmpty()) {
                byte[][] encodeList = new byte[this.gossipList.size()][];

                int i = 0;
                for (GossipItem gossipItem : this.gossipList) {
                    encodeList[i] = RLP.encodeElement(gossipItem.getEncoded());
                    i++;
                }

                rlpEncoded = RLP.encodeList(encodeList);
            }
        }

        return rlpEncoded;
    }

}
