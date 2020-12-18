package io.taucoin.types;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.taucoin.util.HashUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

// gossip list是gossip mutable item的具体内容
public class GossipList {
    private List<GossipItem> gossipList = new CopyOnWriteArrayList<>();

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

    private void parseList(RLPList list) {
        for (int i = 0; i < list.size(); i++) {
            byte[] encode = list.get(i).getRLPData();
            this.gossipList.add(new GossipItem(encode));
        }
    }

    /**
     * parse rlp encode
     */
    private void parseRLP() {
        RLPList params = RLP.decode2(this.rlpEncoded);
        RLPList list = (RLPList) params.get(0);

        parseList(list);

        this.parsed = true;
    }

    public byte[] getGossipListEncoded() {
        byte[][] gossipListEncoded = new byte[this.gossipList.size()][];
        int i = 0;
        for (GossipItem gossipItem : this.gossipList) {
            gossipListEncoded[i] = gossipItem.getEncoded();
            ++i;
        }
        return RLP.encodeList(gossipListEncoded);
    }

    /**
     * get encoded hash list
     * @return encode
     */
    public byte[] getEncoded(){
        if (null == rlpEncoded) {
            this.rlpEncoded = getGossipListEncoded();
        }

        return rlpEncoded;
    }

    @Override
    public String toString() {
        List<String> list = new ArrayList<>();

        List<GossipItem> gossipList = getGossipList();
        if (null != gossipList) {
            for (GossipItem gossipItem: gossipList) {
                list.add(gossipItem.toString());
            }
        }

        return "GossipList{" + list + '}';
    }
}
