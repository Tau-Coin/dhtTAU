package io.taucoin.types;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;

import io.taucoin.util.HashUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

// 单条gossip记录，大概能放16条记录在一个mutable item中
public class GossipItem {
    private byte[] sender; // 4个字节,提取的public key的前缀
    private byte[] receiver; // 4个字节,提取的public key的前缀
    // 可能需要拉长
    private BigInteger timestamp; // 4个字节
    // 我的最新消息的root
    private byte[] messageRoot;  // 20个字节
    // 我看到的对方的root
    private byte[] confirmationRoot = null; // 20个字节
    // 我的需求，是immutable data的hash, demand功能仅限于immutable data，
    // 对于mutable data由时钟频率提供数据，不需要demand
    private byte[] demandImmutableDataHash = null; // 20个字节
    // 当前的聊天状态,可以发现对方是否处于写状态
    private GossipStatus gossipStatus = GossipStatus.UNKNOWN; // 1个字节

    private byte[] hash;
    private byte[] encode; // 缓存编码
    private boolean parsed = false; // 是否解码标志

    // 主要用于转发的gossip item
    public GossipItem(byte[] sender, byte[] receiver, BigInteger timestamp) {
        this.sender = sender;
        this.receiver = receiver;
        this.timestamp = timestamp;

        this.parsed = true;
    }

    public GossipItem(byte[] sender, byte[] receiver, BigInteger timestamp, byte[] messageRoot,
                      byte[] confirmationRoot, byte[] demandImmutableDataHash) {
        this.sender = sender;
        this.receiver = receiver;
        this.timestamp = timestamp;
        this.messageRoot = messageRoot;
        this.confirmationRoot = confirmationRoot;
        this.demandImmutableDataHash = demandImmutableDataHash;

        this.parsed = true;
    }

    public GossipItem(byte[] sender, byte[] receiver, BigInteger timestamp, byte[] messageRoot, byte[] confirmationRoot) {
        this.sender = sender;
        this.receiver = receiver;
        this.timestamp = timestamp;
        this.messageRoot = messageRoot;
        this.confirmationRoot = confirmationRoot;

        this.parsed = true;
    }

    public GossipItem(byte[] sender, byte[] receiver, BigInteger timestamp, byte[] messageRoot,
                      byte[] confirmationRoot, byte[] demandImmutableDataHash, GossipStatus gossipStatus) {
        this.sender = sender;
        this.receiver = receiver;
        this.timestamp = timestamp;
        this.messageRoot = messageRoot;
        this.confirmationRoot = confirmationRoot;
        this.demandImmutableDataHash = demandImmutableDataHash;
        this.gossipStatus = gossipStatus;

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

    public byte[] getDemandImmutableDataHash() {
        if (!this.parsed) {
            parseRLP();
        }

        return demandImmutableDataHash;
    }

    public GossipStatus getGossipStatus() {
        if (!this.parsed) {
            parseRLP();
        }

        return gossipStatus;
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

        this.messageRoot = messageList.get(3).getRLPData();
        this.confirmationRoot = messageList.get(4).getRLPData();
        this.demandImmutableDataHash = messageList.get(5).getRLPData();

        byte[] statusBytes = messageList.get(6).getRLPData();
        int statusNum = null == statusBytes ? 0: new BigInteger(1, statusBytes).intValue();
        if (statusNum >= GossipStatus.UNKNOWN.ordinal()) {
            this.gossipStatus = GossipStatus.UNKNOWN;
        } else {
            this.gossipStatus = GossipStatus.values()[statusNum];
        }

        this.parsed = true;
    }

    public byte[] getEncoded() {
        if (null == this.encode) {
            byte[] sender = RLP.encodeElement(this.sender);
            byte[] receiver = RLP.encodeElement(this.receiver);
            byte[] timestamp = RLP.encodeBigInteger(this.timestamp);
            byte[] messageRoot = RLP.encodeElement(this.messageRoot);
            byte[] confirmationRoot = RLP.encodeElement(this.confirmationRoot);
            byte[] demandHash = RLP.encodeElement(this.demandImmutableDataHash);
            byte[] gossipStatus = RLP.encodeBigInteger(BigInteger.valueOf(this.gossipStatus.ordinal()));

            this.encode = RLP.encodeList(sender, receiver, timestamp, messageRoot,
                    confirmationRoot, demandHash, gossipStatus);
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
        byte[] sender = getSender();
        byte[] receiver = getReceiver();
        BigInteger timestamp = getTimestamp();
        byte[] messageRoot = getMessageRoot();
        byte[] confirmationRoot = getConfirmationRoot();
        byte[] demandHash = getDemandImmutableDataHash();
        GossipStatus gossipStatus = getGossipStatus();

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("GossipItem{");

        if (null != sender) {
            stringBuilder.append("sender=");
            stringBuilder.append(Hex.toHexString(sender));
        }

        if (null != receiver) {
            stringBuilder.append(", receiver=");
            stringBuilder.append(Hex.toHexString(receiver));
        }

        if (null != timestamp) {
            stringBuilder.append(", timestamp=");
            stringBuilder.append(timestamp);
        }

        if (null != messageRoot) {
            stringBuilder.append(", messageRoot=");
            stringBuilder.append(Hex.toHexString(messageRoot));
        }

        if (null != confirmationRoot) {
            stringBuilder.append(", confirmationRoot=");
            stringBuilder.append(Hex.toHexString(confirmationRoot));
        }

        if (null != demandHash) {
            stringBuilder.append(", demandHash=");
            stringBuilder.append(Hex.toHexString(demandHash));
        }

        if (null != gossipStatus) {
            stringBuilder.append(", gossipStatus=");
            stringBuilder.append(gossipStatus);
        }

        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
