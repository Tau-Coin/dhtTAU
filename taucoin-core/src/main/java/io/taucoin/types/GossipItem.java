package io.taucoin.types;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;

import io.taucoin.util.HashUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class GossipItem {
    private byte[] sender;
    private byte[] receiver;
    // 可能需要拉长
    private BigInteger timestamp;
    private GossipType gossipType;
    private byte[] messageRoot;
    private byte[] confirmationRoot;

    private byte[] hash;
    private byte[] encode;
    private boolean parsed = false;

    public GossipItem(byte[] sender, byte[] receiver, BigInteger timestamp, GossipType gossipType, byte[] messageRoot, byte[] confirmationRoot) {
        this.sender = sender;
        this.receiver = receiver;
        this.timestamp = timestamp;
        this.gossipType = gossipType;
        this.messageRoot = messageRoot;
        this.confirmationRoot = confirmationRoot;

        this.parsed = true;
    }

    public GossipItem(byte[] encode) {
        this.encode = encode;
    }

    public byte[] getSender() {
        if (!this.parsed) {
            parseRLP();
        }

        return sender;
    }

    public byte[] getReceiver() {
        if (!this.parsed) {
            parseRLP();
        }

        return receiver;
    }

    public BigInteger getTimestamp() {
        if (!this.parsed) {
            parseRLP();
        }

        return timestamp;
    }

    public GossipType getGossipType() {
        if (!this.parsed) {
            parseRLP();
        }

        return gossipType;
    }

    public byte[] getMessageRoot() {
        if (!this.parsed) {
            parseRLP();
        }

        return messageRoot;
    }

    public byte[] getConfirmationRoot() {
        if (!this.parsed) {
            parseRLP();
        }

        return confirmationRoot;
    }

    public byte[] getHash() {
        if (null == this.hash) {
            this.hash = HashUtil.bencodeHash(getEncoded());
        }

        return this.hash;
    }

    private void parseRLP() {
        RLPList params = RLP.decode2(this.encode);
        RLPList messageList = (RLPList) params.get(0);

        this.sender = messageList.get(0).getRLPData();
        this.receiver = messageList.get(1).getRLPData();
        byte[] timeBytes = messageList.get(2).getRLPData();
        this.timestamp = (null == timeBytes) ? BigInteger.ZERO: new BigInteger(1, timeBytes);
        byte[] typeBytes = messageList.get(3).getRLPData();
        int typeNum = null == typeBytes ? 0: new BigInteger(1, typeBytes).intValue();
        if (typeNum >= GossipType.UNKNOWN.ordinal()) {
            this.gossipType = GossipType.UNKNOWN;
        } else {
            this.gossipType = GossipType.values()[typeNum];
        }
        this.messageRoot = messageList.get(4).getRLPData();
        this.confirmationRoot = messageList.get(5).getRLPData();

        this.parsed = true;
    }

    public byte[] getEncoded() {
        if (null == this.encode) {
            byte[] sender = RLP.encodeElement(this.sender);
            byte[] receiver = RLP.encodeElement(this.receiver);
            byte[] timestamp = RLP.encodeBigInteger(this.timestamp);
            byte[] gossipType = RLP.encodeBigInteger(BigInteger.valueOf(this.gossipType.ordinal()));
            byte[] messageRoot = RLP.encodeElement(this.messageRoot);
            byte[] confirmationRoot = RLP.encodeElement(this.confirmationRoot);

            this.encode = RLP.encodeList(sender, receiver, timestamp, gossipType, messageRoot, confirmationRoot);
        }

        return this.encode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GossipItem that = (GossipItem) o;
        return Arrays.equals(this.getHash(), that.getHash());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getHash());
    }

    @Override
    public String toString() {
        byte[] confirmationRoot = getConfirmationRoot();

        if (null != confirmationRoot) {
            return "GossipItem{" +
                    "sender=" + Hex.toHexString(getSender()) +
                    ", receiver=" + Hex.toHexString(getReceiver()) +
                    ", timestamp=" + getTimestamp() +
                    ", gossipType=" + getGossipType() +
                    ", messageRoot=" + Hex.toHexString(getMessageRoot()) +
                    ", confirmationRoot=" + Hex.toHexString(confirmationRoot) +
                    '}';
        } else {
            return "GossipItem{" +
                    "sender=" + Hex.toHexString(getSender()) +
                    ", receiver=" + Hex.toHexString(getReceiver()) +
                    ", timestamp=" + getTimestamp() +
                    ", gossipType=" + getGossipType() +
                    ", messageRoot=" + Hex.toHexString(getMessageRoot()) +
                    '}';
        }
    }
}
