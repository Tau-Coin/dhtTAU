package io.taucoin.types;

import io.taucoin.util.HashUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class GossipItem {
    private byte[] sender;
    private byte[] receiver;
    private byte[] timestamp;
    private byte[] messageRoot;

    private byte[] hash;
    private byte[] encode;
    private boolean parsed = false;

    public GossipItem(byte[] sender, byte[] receiver, byte[] timestamp, byte[] messageRoot) {
        this.sender = sender;
        this.receiver = receiver;
        this.timestamp = timestamp;
        this.messageRoot = messageRoot;
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

    public byte[] getTimestamp() {
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
        this.timestamp = messageList.get(2).getRLPData();
        this.messageRoot = messageList.get(3).getRLPData();

        this.parsed = true;
    }

    public byte[] getEncoded() {
        if (null == this.encode) {
            byte[] sender = RLP.encodeElement(this.sender);
            byte[] receiver = RLP.encodeElement(this.receiver);
            byte[] timestamp = RLP.encodeElement(this.timestamp);
            byte[] messageRoot = RLP.encodeElement(this.messageRoot);

            this.encode = RLP.encodeList(sender, receiver, timestamp, messageRoot);
        }

        return this.encode;
    }
}
