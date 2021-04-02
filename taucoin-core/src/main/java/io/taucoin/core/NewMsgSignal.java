package io.taucoin.core;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.taucoin.types.GossipItem;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class NewMsgSignal {
    byte[] hashPrefixArray;
    Bloom friendListBloomFilter = null;
    // TODO：：推荐策略重新考虑
    byte[] chattingFriend; // 当前正在交谈的朋友
    BigInteger chattingTime; // 正在交谈的时间
    private List<GossipItem> gossipItemList = new CopyOnWriteArrayList<>(); // 打听到的信息

    private byte[] rlpEncoded; // 编码数据
    private boolean parsed = false; // 解析标志

    public NewMsgSignal(byte[] hashPrefixArray, byte[] chattingFriend,
                        BigInteger chattingTime, List<GossipItem> gossipItemList) {
        this.hashPrefixArray = hashPrefixArray;
        this.chattingFriend = chattingFriend;
        this.chattingTime = chattingTime;
        this.gossipItemList = gossipItemList;

        this.parsed = true;
    }

    public NewMsgSignal(byte[] hashPrefixArray, Bloom friendListBloomFilter,
                        byte[] chattingFriend, BigInteger chattingTime, List<GossipItem> gossipItemList) {
        this.hashPrefixArray = hashPrefixArray;
        this.friendListBloomFilter = friendListBloomFilter;
        this.chattingFriend = chattingFriend;
        this.chattingTime = chattingTime;
        this.gossipItemList = gossipItemList;

        this.parsed = true;
    }

    public NewMsgSignal(byte[] rlpEncoded) {
        this.rlpEncoded = rlpEncoded;
    }

    public byte[] getHashPrefixArray() {
        if (!parsed) {
            parseRLP();
        }

        return hashPrefixArray;
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

        this.hashPrefixArray = list.get(0).getRLPData();

        byte[] friendBloomBytes = list.get(1).getRLPData();
        this.friendListBloomFilter = (null == friendBloomBytes) ? new Bloom(): new Bloom(friendBloomBytes);

        this.chattingFriend = list.get(2).getRLPData();

        byte[] chattingTimeBytes = list.get(3).getRLPData();
        this.chattingTime = (null == chattingTimeBytes) ? BigInteger.ZERO: new BigInteger(1, chattingTimeBytes);

        parseGossipList((RLPList) list.get(4));

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
            byte[] hashPrefixArray = RLP.encodeElement(this.hashPrefixArray);
            byte[] friendListBloomFilter = RLP.encodeElement(null);
            if (null != this.friendListBloomFilter) {
                friendListBloomFilter = RLP.encodeElement(this.friendListBloomFilter.getData());
            }
            byte[] chattingFriend = RLP.encodeElement(this.chattingFriend);
            byte[] chattingTime = RLP.encodeBigInteger(this.chattingTime);
            byte[] gossipListEncoded = getGossipListEncoded();

            this.rlpEncoded = RLP.encodeList(hashPrefixArray, friendListBloomFilter,
                    chattingFriend, chattingTime, gossipListEncoded);
        }

        return rlpEncoded;
    }

    @Override
    public String toString() {
        byte[] hashPrefixArray = getHashPrefixArray();
        Bloom friendListBloomFilter = getFriendListBloomFilter();
        BigInteger chattingTime = getChattingTime();
        byte[] chattingFriend = getChattingFriend();

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("OnlineSignal{");
        if (null != hashPrefixArray) {
            stringBuilder.append("hash prefix array=");
            stringBuilder.append(Hex.toHexString(hashPrefixArray));
        }
        if (null != friendListBloomFilter) {
            stringBuilder.append(", friend list bloom filter=");
            stringBuilder.append(friendListBloomFilter);
        }
        if (null != chattingTime) {
            stringBuilder.append(", chatting time=");
            stringBuilder.append(chattingTime);
        }
        if (null != chattingFriend) {
            stringBuilder.append(", chatting friend=");
            stringBuilder.append(Hex.toHexString(chattingFriend));
        }

        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
