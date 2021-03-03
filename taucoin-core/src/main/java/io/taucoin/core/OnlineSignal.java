package io.taucoin.core;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.taucoin.types.GossipItem;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class OnlineSignal {
    Bloom senderBloomFilter = new Bloom();
    Bloom receiverBloomFilter = new Bloom();
    Bloom friendListBloomFilter = new Bloom();
    byte[] chattingFriend; // 当前正在交谈的朋友
    BigInteger chattingTime; // 正在交谈的时间
    private List<GossipItem> gossipItemList = new CopyOnWriteArrayList<>(); // 打听到的信息

    private byte[] rlpEncoded; // 编码数据
    private boolean parsed = false; // 解析标志

    public OnlineSignal(Bloom senderBloomFilter, Bloom receiverBloomFilter, Bloom friendListBloomFilter,
                        byte[] chattingFriend, BigInteger chattingTime, List<GossipItem> gossipItemList) {
        this.senderBloomFilter = senderBloomFilter;
        this.receiverBloomFilter = receiverBloomFilter;
        this.friendListBloomFilter = friendListBloomFilter;
        this.chattingFriend = chattingFriend;
        this.chattingTime = chattingTime;
        this.gossipItemList = gossipItemList;

        this.parsed = true;
    }

    public OnlineSignal(byte[] rlpEncoded) {
        this.rlpEncoded = rlpEncoded;
    }

    public Bloom getSenderBloomFilter() {
        if (!parsed) {
            parseRLP();
        }

        return senderBloomFilter;
    }

    public Bloom getReceiverBloomFilter() {
        if (!parsed) {
            parseRLP();
        }

        return receiverBloomFilter;
    }

    public Bloom getFriendListBloomFilter() {
        if (!parsed) {
            parseRLP();
        }

        return friendListBloomFilter;
    }

    public byte[] getChattingFriend() {
        if (!parsed) {
            parseRLP();
        }

        return chattingFriend;
    }

    public BigInteger getChattingTime() {
        if (!parsed) {
            parseRLP();
        }

        return chattingTime;
    }

    public List<GossipItem> getGossipItemList() {
        if (!parsed) {
            parseRLP();
        }

        return gossipItemList;
    }

    /**
     * parse rlp encode
     */
    private void parseRLP() {
        RLPList params = RLP.decode2(this.rlpEncoded);
        RLPList list = (RLPList) params.get(0);

        this.senderBloomFilter = new Bloom(list.get(0).getRLPData());
        this.receiverBloomFilter = new Bloom(list.get(1).getRLPData());
        this.friendListBloomFilter = new Bloom(list.get(2).getRLPData());
        this.chattingFriend = list.get(3).getRLPData();

        byte[] chattingTimeBytes = list.get(4).getRLPData();
        this.chattingTime = (null == chattingTimeBytes) ? BigInteger.ZERO: new BigInteger(1, chattingTimeBytes);

        parseGossipList((RLPList) list.get(5));

        this.parsed = true;
    }

    private void parseGossipList(RLPList list) {
        for (int i = 0; i < list.size(); i++) {
            byte[] encode = list.get(i).getRLPData();
            this.gossipItemList.add(new GossipItem(encode));
        }
    }

    public byte[] getGossipListEncoded() {
        byte[][] gossipListEncoded = new byte[this.gossipItemList.size()][];
        int i = 0;
        for (GossipItem gossipItem : this.gossipItemList) {
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
            byte[] senderBloomFilter = RLP.encodeElement(this.senderBloomFilter.getData());
            byte[] receiverBloomFilter = RLP.encodeElement(this.receiverBloomFilter.getData());
            byte[] friendListBloomFilter = RLP.encodeElement(this.friendListBloomFilter.getData());
            byte[] chattingFriend = RLP.encodeElement(this.chattingFriend);
            byte[] chattingTime = RLP.encodeBigInteger(this.chattingTime);
            byte[] gossipListEncoded = getGossipListEncoded();

            this.rlpEncoded = RLP.encodeList(senderBloomFilter, receiverBloomFilter,
                    friendListBloomFilter, chattingFriend, chattingTime, gossipListEncoded);
        }

        return rlpEncoded;
    }
}
