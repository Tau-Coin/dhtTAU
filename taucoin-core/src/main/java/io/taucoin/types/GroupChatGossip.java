package io.taucoin.types;

import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.taucoin.util.HashUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class GroupChatGossip {
    private byte[] bestBlockHash;
    private byte[] bestTxHash;
    private List<GossipElement> gossipList = new CopyOnWriteArrayList<>();

    private byte[] hash;
    private byte[] encode;
    private boolean parsed = false;

    public GroupChatGossip(byte[] bestBlockHash, byte[] bestTxHash, List<GossipElement> gossipList) {
        this.bestBlockHash = bestBlockHash;
        this.bestTxHash = bestTxHash;
        this.gossipList = gossipList;

        this.parsed = true;
    }

    public GroupChatGossip(byte[] encode) {
        this.encode = encode;
    }

    public byte[] getBestBlockHash() {
        if (!parsed) {
            parseRLP();
        }

        return bestBlockHash;
    }

    public byte[] getBestTxHash() {
        if (!parsed) {
            parseRLP();
        }

        return bestTxHash;
    }

    public List<GossipElement> getGossipList() {
        if (!parsed) {
            parseRLP();
        }

        return gossipList;
    }

    public byte[] getHash(){
        if(null == this.hash) {
            this.hash = HashUtil.bencodeHash(this.getEncoded());
        }

        return this.hash;
    }

    /**
     * get encoded hash list
     * @return encode
     */
    public byte[] getEncoded(){
        if (null == this.encode) {
            byte[] bestBlockHash = RLP.encodeElement(this.bestBlockHash);
            byte[] bestTxHash = RLP.encodeElement(this.bestTxHash);
            byte[] gossipListEncoded = getGossipListEncoded();

            this.encode = RLP.encodeList(bestBlockHash, bestTxHash, gossipListEncoded);
        }

        return encode;
    }

    public byte[] getGossipListEncoded() {
        byte[][] gossipListEncoded = new byte[this.gossipList.size()][];
        int i = 0;
        for (GossipElement gossipElement : this.gossipList) {
            gossipListEncoded[i] = gossipElement.getEncoded();
            ++i;
        }
        return RLP.encodeList(gossipListEncoded);
    }


    private void parseList(RLPList list) {
        for (int i = 0; i < list.size(); i++) {
            byte[] encode = list.get(i).getRLPData();
            this.gossipList.add(new GossipElement(encode));
        }
    }

    /**
     * parse rlp encode
     */
    private void parseRLP() {
        RLPList params = RLP.decode2(this.encode);
        RLPList list = (RLPList) params.get(0);

        this.bestBlockHash = list.get(0).getRLPData();
        this.bestTxHash = list.get(1).getRLPData();

        parseList((RLPList)list.get(2));

        this.parsed = true;
    }

    @Override
    public String toString() {
        List<String> list = new ArrayList<>();

        byte[] bestBlockHash = getBestBlockHash();
        if (null != bestBlockHash) {
            list.add("Best block hash:" + Hex.toHexString(bestBlockHash));
        }
        byte[] bestTxHash = getBestTxHash();
        if (null != bestTxHash) {
            list.add("Best tx hash:" + Hex.toHexString(bestTxHash));
        }

        List<GossipElement> gossipList = getGossipList();
        if (null != gossipList) {
            for (GossipElement gossipElement: gossipList) {
                list.add(gossipElement.toString());
            }
        }

        return "GroupChatGossip{" + list + '}';
    }
}
