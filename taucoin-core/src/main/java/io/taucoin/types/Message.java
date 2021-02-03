package io.taucoin.types;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

import io.taucoin.util.HashUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class Message {
    private MessageVersion version; // 标识消息版本
    private BigInteger timestamp;
    private byte[] previousHash; // 用于确认切分消息的顺序
    private BigInteger nonce; // 局部nonce，用于标识切分消息的顺序
    private MessageType type;  // 可以标识消息类型
    private byte[] content; // 消息体

    private byte[] hash; // gossip hash 入口
    private byte[] encode; // 缓存编码
    private boolean parsed = false; // 是否解码标志

    public static Message createTextMessage(BigInteger timestamp, byte[] previousHash, BigInteger nonce, byte[] content) {
        return new Message(MessageVersion.VERSION1, timestamp, previousHash, nonce, MessageType.TEXT, content);
    }

    public static Message createPictureMessage(BigInteger timestamp, byte[] previousHash, BigInteger nonce, byte[] content) {
        return new Message(MessageVersion.VERSION1, timestamp, previousHash, nonce, MessageType.PICTURE, content);
    }

    public Message(MessageVersion version, BigInteger timestamp, byte[] previousHash, BigInteger nonce, MessageType type, byte[] content) {
        this.version = version;
        this.timestamp = timestamp;
        this.previousHash = previousHash;
        this.nonce = nonce;
        this.type = type;
        this.content = content;

        this.parsed = true;
    }

    public Message(byte[] encode) {
        this.encode = encode;
    }

    public MessageVersion getVersion() {
        if (!this.parsed) {
            parseRLP();
        }

        return version;
    }

    public BigInteger getTimestamp() {
        if (!this.parsed) {
            parseRLP();
        }

        return timestamp;
    }

    public byte[] getPreviousHash() {
        if (!this.parsed) {
            parseRLP();
        }

        return previousHash;
    }

    public BigInteger getNonce() {
        if (!this.parsed) {
            parseRLP();
        }

        return nonce;
    }

    public MessageType getType() {
        if (!this.parsed) {
            parseRLP();
        }

        return type;
    }

    public byte[] getContent() {
        if (!this.parsed) {
            parseRLP();
        }

        return content;
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

        byte[] versionBytes = messageList.get(0).getRLPData();
        int versionNum = null == versionBytes ? 0: new BigInteger(1, versionBytes).intValue();
        if (versionNum >= MessageVersion.MAX_VERSION.ordinal()) {
            this.version = MessageVersion.MAX_VERSION;
        } else {
            this.version = MessageVersion.values()[versionNum];
        }

        byte[] timeBytes = messageList.get(1).getRLPData();
        this.timestamp = (null == timeBytes) ? BigInteger.ZERO: new BigInteger(1, timeBytes);

        this.previousHash = messageList.get(2).getRLPData();

        byte[] nonceBytes = messageList.get(3).getRLPData();
        this.nonce = (null == nonceBytes) ? BigInteger.ZERO: new BigInteger(1, nonceBytes);

        byte[] typeBytes = messageList.get(4).getRLPData();
        int typeNum = null == typeBytes ? 0: new BigInteger(1, typeBytes).intValue();
        if (typeNum >= MessageType.UNKNOWN.ordinal()) {
            this.type = MessageType.UNKNOWN;
        } else {
            this.type = MessageType.values()[typeNum];
        }

        this.content = messageList.get(5).getRLPData();

        this.parsed = true;
    }

    public byte[] getEncoded() {
        if (null == this.encode) {
            byte[] version = RLP.encodeBigInteger(BigInteger.valueOf(this.version.ordinal()));
            byte[] timestamp = RLP.encodeBigInteger(this.timestamp);
            byte[] previousHash = RLP.encodeElement(this.previousHash);
            byte[] nonce = RLP.encodeBigInteger(this.nonce);
            byte[] type = RLP.encodeBigInteger(BigInteger.valueOf(this.type.ordinal()));
            byte[] content = RLP.encodeElement(this.content);

            this.encode = RLP.encodeList(version, timestamp, previousHash, nonce, type, content);
        }

        return this.encode;
    }

    @Override
    public String toString() {
        MessageVersion version = getVersion();
        BigInteger timestamp = getTimestamp();
        byte[] previousHash = getPreviousHash();
        BigInteger nonce = getNonce();
        MessageType type = getType();
        byte[] content = getContent();
        byte[] hash = getHash();

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Message{");
        if (null != hash) {
            stringBuilder.append("hash=");
            stringBuilder.append(Hex.toHexString(hash));
        }
        if (null != version) {
            stringBuilder.append(", version=");
            stringBuilder.append(version);
        }
        if (null != timestamp) {
            stringBuilder.append(", timestamp=");
            stringBuilder.append(timestamp);
        }
        if (null != previousHash) {
            stringBuilder.append("previous hash=");
            stringBuilder.append(Hex.toHexString(previousHash));
        }
        if (null != nonce) {
            stringBuilder.append(", nonce=");
            stringBuilder.append(nonce);
        }
        if (null != type) {
            stringBuilder.append(", type=");
            stringBuilder.append(type);
        }
        if (null != content) {
            stringBuilder.append(", content=");
            stringBuilder.append(new String(content));
        }

        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
