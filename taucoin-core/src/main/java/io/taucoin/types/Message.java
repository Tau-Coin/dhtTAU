package io.taucoin.types;

import java.math.BigInteger;

import io.taucoin.util.HashUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class Message {
    private MessageVersion version;
    private byte[] timestamp;
    private byte[] previousMsgDAGRoot;
    private byte[] friendLatestMessageRoot;
    private MessageType type;
    private byte[] contentLink;

    private byte[] hash;
    private byte[] encode;
    private boolean parsed = false;

    public static Message CreateTextMessage(byte[] timestamp, byte[] previousMsgDAGRoot, byte[] friendLatestMessageRoot, byte[] contentLink) {
        return new Message(MessageVersion.VERSION1, timestamp, previousMsgDAGRoot, friendLatestMessageRoot, MessageType.TEXT, contentLink);
    }

    public static Message CreatePictureMessage(byte[] timestamp, byte[] previousMsgDAGRoot, byte[] friendLatestMessageRoot, byte[] contentLink) {
        return new Message(MessageVersion.VERSION1, timestamp, previousMsgDAGRoot, friendLatestMessageRoot, MessageType.PICTURE, contentLink);
    }

    private Message(MessageVersion version, byte[] timestamp, byte[] previousMsgDAGRoot, byte[] friendLatestMessageRoot, MessageType type, byte[] contentLink) {
        this.version = version;
        this.timestamp = timestamp;
        this.previousMsgDAGRoot = previousMsgDAGRoot;
        this.friendLatestMessageRoot = friendLatestMessageRoot;
        this.type = type;
        this.contentLink = contentLink;

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

    public byte[] getTimestamp() {
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

    public MessageType getType() {
        if (!this.parsed) {
            parseRLP();
        }

        return type;
    }

    public byte[] getContentLink() {
        if (!this.parsed) {
            parseRLP();
        }

        return contentLink;
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

        byte[] versionByte = messageList.get(0).getRLPData();
        int versionNum = null == versionByte ? 0: new BigInteger(1, versionByte).intValue();
        if (versionNum >= MessageVersion.MAX_VERSION.ordinal()) {
            this.version = MessageVersion.MAX_VERSION;
        } else {
            this.version = MessageVersion.values()[versionNum];
        }

        this.timestamp = messageList.get(1).getRLPData();

        this.previousMsgDAGRoot = messageList.get(2).getRLPData();

        this.friendLatestMessageRoot = messageList.get(3).getRLPData();

        byte[] typeByte = messageList.get(4).getRLPData();
        int typeNum = null == typeByte ? 0: new BigInteger(1, typeByte).intValue();
        if (typeNum >= MessageType.UNKNOWN.ordinal()) {
            this.type = MessageType.UNKNOWN;
        } else {
            this.type = MessageType.values()[typeNum];
        }

        this.contentLink = messageList.get(5).getRLPData();

        this.parsed = true;
    }

    public byte[] getEncoded() {
        if (null == this.encode) {
            byte[] version = RLP.encodeBigInteger(BigInteger.valueOf(this.version.ordinal()));
            byte[] timestamp = RLP.encodeElement(this.timestamp);
            byte[] previousMsgDAGRoot = RLP.encodeElement(this.previousMsgDAGRoot);
            byte[] friendLatestMessageRoot = RLP.encodeElement(this.friendLatestMessageRoot);
            byte[] type = RLP.encodeBigInteger(BigInteger.valueOf(this.type.ordinal()));
            byte[] contentLink = RLP.encodeElement(this.contentLink);

            this.encode = RLP.encodeList(version, timestamp, previousMsgDAGRoot, friendLatestMessageRoot, type, contentLink);
        }

        return this.encode;
    }
}
