package io.taucoin.types;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;

import io.taucoin.util.CryptoUtil;
import io.taucoin.util.HashUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class Message {
    private MessageVersion version; // 标识消息版本
    private BigInteger timestamp;
    private byte[] sender;
    private byte[] receiver;
    private byte[] logicMsgHash; // 用于确认区分逻辑消息，带时间戳（可能连发两次）
    private BigInteger nonce; // 局部nonce，用于标识切分消息的顺序
    private MessageType type;  // 可以标识消息类型
    private byte[] encryptedContent; // 加密消息体

    private byte[] rawContent; // 加密之前的原始
    private byte[] hash; // gossip hash 入口
    private byte[] sha1Hash; // 用于bloom，因为bencodeHash前面一部分字节完全一样不随机
    private byte[] encode; // 缓存编码
    private boolean parsed = false; // 是否解码标志

    public static Message createTextMessage(BigInteger timestamp, byte[] sender, byte[] receiver,
                                            byte[] logicMsgHash, BigInteger nonce, byte[] rawContent) {
        return new Message(MessageVersion.VERSION1, timestamp, sender, receiver, logicMsgHash, nonce, MessageType.TEXT, rawContent);
    }

    public static Message createPictureMessage(BigInteger timestamp, byte[] sender, byte[] receiver,
                                               byte[] logicMsgHash, BigInteger nonce, byte[] rawContent) {
        return new Message(MessageVersion.VERSION1, timestamp, sender, receiver, logicMsgHash, nonce, MessageType.PICTURE, rawContent);
    }

    public Message(MessageVersion version, BigInteger timestamp, byte[] sender, byte[] receiver,
                   byte[] logicMsgHash, BigInteger nonce, MessageType type, byte[] rawContent) {
        this.version = version;
        this.timestamp = timestamp;
        this.sender = sender;
        this.receiver = receiver;
        this.logicMsgHash = logicMsgHash;
        this.nonce = nonce;
        this.type = type;
        this.rawContent = rawContent;

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

    public byte[] getLogicMsgHash() {
        if (!this.parsed) {
            parseRLP();
        }

        return logicMsgHash;
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

    public byte[] getEncryptedContent() {
        if (!this.parsed) {
            parseRLP();
        }

        return encryptedContent;
    }

    public void setEncryptedContent(byte[] encryptedContent) {
        this.encryptedContent = encryptedContent;
    }

    public byte[] getRawContent() {
        return this.rawContent;
    }

    /**
     * 对消息内容进行加密
     * @param key 加解密秘钥
     * @throws Exception data exception
     */
    public void encrypt(byte[] key) throws Exception {
        if (null != this.rawContent) {
            this.encryptedContent = CryptoUtil.encrypt(this.rawContent, key);
        }
    }

    /**
     * 对消息内容解密
     * @param key 加解密秘钥
     * @throws Exception data exception
     */
    public byte[] decrypt(byte[] key) throws Exception {
        if (null == this.rawContent) {
            if (!this.parsed) {
                parseRLP();
            }

            this.rawContent = CryptoUtil.decrypt(this.encryptedContent, key);
        }

        return this.rawContent;
    }

    public byte[] getHash() {
        if (null == this.hash) {
            this.hash = HashUtil.bencodeHash(getEncoded());
        }

        return this.hash;
    }

    public byte[] getSha1Hash() {
        if (null == this.sha1Hash) {
            this.sha1Hash = HashUtil.sha1hash(getEncoded());
        }

        return this.sha1Hash;
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

        this.sender = messageList.get(2).getRLPData();

        this.receiver = messageList.get(3).getRLPData();

        this.logicMsgHash = messageList.get(4).getRLPData();

        byte[] nonceBytes = messageList.get(5).getRLPData();
        this.nonce = (null == nonceBytes) ? BigInteger.ZERO: new BigInteger(1, nonceBytes);

        byte[] typeBytes = messageList.get(6).getRLPData();
        int typeNum = null == typeBytes ? 0: new BigInteger(1, typeBytes).intValue();
        if (typeNum >= MessageType.UNKNOWN.ordinal()) {
            this.type = MessageType.UNKNOWN;
        } else {
            this.type = MessageType.values()[typeNum];
        }

        this.encryptedContent = messageList.get(7).getRLPData();

        this.parsed = true;
    }

    public byte[] getEncoded() {
        if (null == this.encode) {
            byte[] version = RLP.encodeBigInteger(BigInteger.valueOf(this.version.ordinal()));
            byte[] timestamp = RLP.encodeBigInteger(this.timestamp);
            byte[] sender = RLP.encodeElement(this.sender);
            byte[] receiver = RLP.encodeElement(this.receiver);
            byte[] previousHash = RLP.encodeElement(this.logicMsgHash);
            byte[] nonce = RLP.encodeBigInteger(this.nonce);
            byte[] type = RLP.encodeBigInteger(BigInteger.valueOf(this.type.ordinal()));
            byte[] encryptedContent = RLP.encodeElement(this.encryptedContent);

            this.encode = RLP.encodeList(version, timestamp, sender, receiver, previousHash, nonce, type, encryptedContent);
        }

        return this.encode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Arrays.equals(getHash(), message.getHash());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getHash());
    }

    @Override
    public String toString() {
        MessageVersion version = getVersion();
        BigInteger timestamp = getTimestamp();
        byte[] logicMsgHash = getLogicMsgHash();
        BigInteger nonce = getNonce();
        byte[] sender = getSender();
        byte[] receiver = getReceiver();
        MessageType type = getType();
//        byte[] content = getContent();
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
        if (null != sender) {
            stringBuilder.append(", sender=");
            stringBuilder.append(Hex.toHexString(sender));
        }
        if (null != receiver) {
            stringBuilder.append(", receiver=");
            stringBuilder.append(Hex.toHexString(receiver));
        }
        if (null != logicMsgHash) {
            stringBuilder.append("logicMsgHash=");
            stringBuilder.append(Hex.toHexString(logicMsgHash));
        }
        if (null != nonce) {
            stringBuilder.append(", nonce=");
            stringBuilder.append(nonce);
        }
        if (null != type) {
            stringBuilder.append(", type=");
            stringBuilder.append(type);
        }
//        if (null != content) {
//            stringBuilder.append(", content=");
//            stringBuilder.append(new String(content));
//        }

        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
