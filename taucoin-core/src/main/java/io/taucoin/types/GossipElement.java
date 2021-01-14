package io.taucoin.types;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;

import io.taucoin.util.HashUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class GossipElement {
    private byte[] sender;
    private byte[] msgRoot;
    private BigInteger timestamp;

    private byte[] hash;
    private byte[] encode; // 缓存编码
    private boolean parsed = false; // 是否解码标志

    public GossipElement(byte[] sender, byte[] msgRoot) {
        this.sender = sender;
        this.msgRoot = msgRoot;

        this.parsed = true;
    }

    public GossipElement(byte[] encode) {
        this.encode = encode;
    }

    public byte[] getSender() {
        if (!this.parsed) {
            parseRLP();
        }

        return sender;
    }

    public byte[] getMsgRoot() {
        if (!this.parsed) {
            parseRLP();
        }

        return msgRoot;
    }

    public BigInteger getTimestamp() {
        if (!this.parsed) {
            parseRLP();
        }

        return timestamp;
    }

    private void parseRLP() {
        RLPList params = RLP.decode2(this.encode);
        RLPList list = (RLPList) params.get(0);

        this.sender = list.get(0).getRLPData();
        this.msgRoot = list.get(1).getRLPData();
        byte[] timeBytes = list.get(2).getRLPData();
        this.timestamp = (null == timeBytes) ? BigInteger.ZERO: new BigInteger(1, timeBytes);

        this.parsed = true;
    }

    public byte[] getEncoded() {
        if (null == this.encode) {
            byte[] sender = RLP.encodeElement(this.sender);
            byte[] msgRoot = RLP.encodeElement(this.msgRoot);
            byte[] timestamp = RLP.encodeBigInteger(this.timestamp);

            this.encode = RLP.encodeList(sender, msgRoot, timestamp);
        }

        return this.encode;
    }

    public byte[] getHash() {
        if (null == this.hash) {
            this.hash = HashUtil.bencodeHash(getEncoded());
        }

        return this.hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GossipElement that = (GossipElement) o;
        return Arrays.equals(this.getHash(), that.getHash());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getHash());
    }

    @Override
    public String toString() {
        byte[] sender = getSender();
        byte[] msgRoot = getMsgRoot();
        BigInteger timestamp = getTimestamp();

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("GossipElement{");

        if (null != sender) {
            stringBuilder.append("sender=");
            stringBuilder.append(Hex.toHexString(sender));
        }

        if (null != msgRoot) {
            stringBuilder.append(", msgRoot=");
            stringBuilder.append(Hex.toHexString(msgRoot));
        }

        if (null != timestamp) {
            stringBuilder.append(", timestamp=");
            stringBuilder.append(timestamp);
        }

        stringBuilder.append("}");

        return stringBuilder.toString();
    }

}
