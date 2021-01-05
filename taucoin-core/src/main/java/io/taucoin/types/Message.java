package io.taucoin.types;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

import io.taucoin.util.ByteUtil;
import io.taucoin.util.HashUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class Message {
    private MessageVersion version; // 标识消息版本
    private BigInteger timestamp;
    private byte[] previousMsgDAGRoot; // 对应horizontal概念
    private byte[] friendLatestMessageRoot; // 对应horizontal概念
    // 利用skip list概念来提升访问效率和处理访问失败的备份方案，本质上是vertical的概念
    private byte[] skipMessageRoot; // 指向前一个完整意义的消息
    private MessageType type;  // 可以标识消息类型
    private byte[] content; // 消息体

    private byte[] hash; // gossip hash 入口
    private byte[] encode;
    private boolean parsed = false;

    public static Message CreateTextMessage(BigInteger timestamp, byte[] previousMsgDAGRoot,
                                            byte[] friendLatestMessageRoot, byte[] skipMessageRoot, byte[] content) {
        return new Message(MessageVersion.VERSION1, timestamp, previousMsgDAGRoot,
                friendLatestMessageRoot, skipMessageRoot, MessageType.TEXT, content);
    }

//    public static Message CreatePictureMessage(BigInteger timestamp, byte[] previousMsgDAGRoot, byte[] friendLatestMessageRoot, byte[] contentLink) {
//        return new Message(MessageVersion.VERSION1, timestamp, previousMsgDAGRoot, friendLatestMessageRoot, MessageType.PICTURE, contentLink);
//    }

    public Message(MessageVersion version, BigInteger timestamp, byte[] previousMsgDAGRoot,
                   byte[] friendLatestMessageRoot, byte[] skipMessageRoot, MessageType type, byte[] content) {
        this.version = version;
        this.timestamp = timestamp;
        this.previousMsgDAGRoot = previousMsgDAGRoot;
        this.friendLatestMessageRoot = friendLatestMessageRoot;
        this.skipMessageRoot = skipMessageRoot;
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

    public byte[] getPreviousMsgDAGRoot() {
        if (!this.parsed) {
            parseRLP();
        }

        return previousMsgDAGRoot;
    }

    public byte[] getFriendLatestMessageRoot() {
        if (!this.parsed) {
            parseRLP();
        }

        return friendLatestMessageRoot;
    }

    public byte[] getSkipMessageRoot() {
        if (!this.parsed) {
            parseRLP();
        }

        return skipMessageRoot;
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

        this.previousMsgDAGRoot = messageList.get(2).getRLPData();

        this.friendLatestMessageRoot = messageList.get(3).getRLPData();

        this.skipMessageRoot = messageList.get(4).getRLPData();

        byte[] typeBytes = messageList.get(5).getRLPData();
        int typeNum = null == typeBytes ? 0: new BigInteger(1, typeBytes).intValue();
        if (typeNum >= MessageType.UNKNOWN.ordinal()) {
            this.type = MessageType.UNKNOWN;
        } else {
            this.type = MessageType.values()[typeNum];
        }

        this.content = messageList.get(6).getRLPData();

        this.parsed = true;
    }

    public byte[] getEncoded() {
        if (null == this.encode) {
            byte[] version = RLP.encodeBigInteger(BigInteger.valueOf(this.version.ordinal()));
            byte[] timestamp = RLP.encodeBigInteger(this.timestamp);
            byte[] previousMsgDAGRoot = RLP.encodeElement(this.previousMsgDAGRoot);
            byte[] friendLatestMessageRoot = RLP.encodeElement(this.friendLatestMessageRoot);
            byte[] skipMessageRoot = RLP.encodeElement(this.skipMessageRoot);
            byte[] type = RLP.encodeBigInteger(BigInteger.valueOf(this.type.ordinal()));
            byte[] content = RLP.encodeElement(this.content);

            this.encode = RLP.encodeList(version, timestamp, previousMsgDAGRoot, friendLatestMessageRoot, skipMessageRoot, type, content);
        }

        return this.encode;
    }

    @Override
    public String toString() {
        MessageVersion version = getVersion();
        BigInteger timestamp = getTimestamp();
        byte[] previousRoot = getPreviousMsgDAGRoot();
        byte[] friendRoot = getFriendLatestMessageRoot();
        byte[] skipRoot = getSkipMessageRoot();
        MessageType type = getType();
        byte[] content = getContent();
        byte[] hash = getHash();

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("GossipItem{");

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
        if (null != previousRoot) {
            stringBuilder.append("previousMsgRoot=");
            stringBuilder.append(Hex.toHexString(previousRoot));
        }
        if (null != friendRoot) {
            stringBuilder.append("friendMsgRoot=");
            stringBuilder.append(Hex.toHexString(friendRoot));
        }
        if (null != skipRoot) {
            stringBuilder.append("skipMsgRoot=");
            stringBuilder.append(Hex.toHexString(skipRoot));
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
