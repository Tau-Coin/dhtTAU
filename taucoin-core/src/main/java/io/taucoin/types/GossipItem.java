package io.taucoin.types;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;

import io.taucoin.util.HashUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

// 单条gossip记录，大概能放58条记录在一个mutable item中
public class GossipItem {
    private byte[] sender; // 4个字节,提取的public key的前缀
    private BigInteger timestamp; // 4个字节，通过时间戳的更新来标记自己是否有新信息
    // TODO::可以通过多加一个type来扩展信号
    // TODO:: 在对方设备休眠时候的信号变化

    private byte[] hash;
    private byte[] encode; // 缓存编码
    private boolean parsed = false; // 是否解码标志

    // 主要用于转发的gossip item
    public GossipItem(byte[] sender, BigInteger timestamp) {
        this.sender = sender;
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
        byte[] timeBytes = messageList.get(1).getRLPData();
        this.timestamp = (null == timeBytes) ? BigInteger.ZERO: new BigInteger(1, timeBytes);

        this.parsed = true;
    }

    public byte[] getEncoded() {
        if (null == this.encode) {
            byte[] sender = RLP.encodeElement(this.sender);
            byte[] timestamp = RLP.encodeBigInteger(this.timestamp);

            this.encode = RLP.encodeList(sender, timestamp);
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
        BigInteger timestamp = getTimestamp();

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("GossipItem{");

        if (null != sender) {
            stringBuilder.append("sender=");
            stringBuilder.append(Hex.toHexString(sender));
        }

        if (null != timestamp) {
            stringBuilder.append(", timestamp=");
            stringBuilder.append(timestamp);
        }

        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
