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

        this.parsed = true;
    }

    public byte[] getEncoded() {
        if (null == this.encode) {
            byte[] sender = RLP.encodeElement(this.sender);
            byte[] receiver = RLP.encodeElement(this.receiver);
            byte[] timestamp = RLP.encodeBigInteger(this.timestamp);

            this.encode = RLP.encodeList(sender, receiver, timestamp);
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

        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
