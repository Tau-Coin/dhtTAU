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
    private byte[] messageRoot;
    private byte[] confirmationRoot = null;
    private byte[] demandHash = null;
    private GossipStatus gossipStatus = GossipStatus.UNKNOWN;

    private byte[] hash;
    private byte[] encode;
    private boolean parsed = false;

    public GossipItem(byte[] sender, byte[] receiver, BigInteger timestamp, byte[] messageRoot, byte[] confirmationRoot, byte[] demandHash) {
        this.sender = sender;
        this.receiver = receiver;
        this.timestamp = timestamp;
        this.messageRoot = messageRoot;
        this.confirmationRoot = confirmationRoot;
        this.demandHash = demandHash;

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

    public GossipItem(byte[] sender, byte[] receiver, BigInteger timestamp, byte[] messageRoot, byte[] confirmationRoot, byte[] demandHash, GossipStatus gossipStatus) {
        this.sender = sender;
        this.receiver = receiver;
        this.timestamp = timestamp;
        this.messageRoot = messageRoot;
        this.confirmationRoot = confirmationRoot;
        this.demandHash = demandHash;
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

    public byte[] getDemandHash() {
        if (!this.parsed) {
            parseRLP();
        }

        return demandHash;
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
        this.demandHash = messageList.get(5).getRLPData();

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
            byte[] demandHash = RLP.encodeElement(this.demandHash);
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
        byte[] demandHash = getDemandHash();
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
