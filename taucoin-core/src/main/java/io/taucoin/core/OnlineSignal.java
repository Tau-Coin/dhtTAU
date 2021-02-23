package io.taucoin.core;

import java.math.BigInteger;

public class OnlineSignal {
    BigInteger timestamp;
    Bloom senderBloomFilter = new Bloom();
    Bloom receiverBloomFilter = new Bloom();
    Bloom friendListBloomFilter = new Bloom();
    byte[] chattingFriend;
    BigInteger chattingTime;

    public OnlineSignal(BigInteger timestamp, Bloom senderBloomFilter, Bloom receiverBloomFilter, Bloom friendListBloomFilter, byte[] chattingFriend, BigInteger chattingTime) {
        this.timestamp = timestamp;
        this.senderBloomFilter = senderBloomFilter;
        this.receiverBloomFilter = receiverBloomFilter;
        this.friendListBloomFilter = friendListBloomFilter;
        this.chattingFriend = chattingFriend;
        this.chattingTime = chattingTime;
    }

    public BigInteger getTimestamp() {
        return timestamp;
    }

    public Bloom getSenderBloomFilter() {
        return senderBloomFilter;
    }

    public Bloom getReceiverBloomFilter() {
        return receiverBloomFilter;
    }

    public Bloom getFriendListBloomFilter() {
        return friendListBloomFilter;
    }

    public byte[] getChattingFriend() {
        return chattingFriend;
    }

    public BigInteger getChattingTime() {
        return chattingTime;
    }
}
