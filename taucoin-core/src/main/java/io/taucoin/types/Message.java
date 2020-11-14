package io.taucoin.types;

import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class Message {
    private long version;
    private long timestamp;
    private byte[] previousMsgDAGRoot;
    private long type;
    private byte[] contentLink;

    private byte[] hash;
    private byte[] encode;
    private boolean parsed = false;

    public Message(byte[] encode) {
        this.encode = encode;
    }

    public long getVersion() {
        return version;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getPreviousMsgDAGRoot() {
        return previousMsgDAGRoot;
    }

    public long getType() {
        return type;
    }

    public byte[] getContentLink() {
        return contentLink;
    }

    public byte[] getHash() {
        return hash;
    }

    private void parseRLP() {
        RLPList params = RLP.decode2(this.encode);
        RLPList messageList = (RLPList) params.get(0);

        this.previousMsgDAGRoot = messageList.get(2).getRLPData();

        this.parsed = true;
    }

    public byte[] getEncoded() {
        if (null == this.encode) {
        }

        return this.encode;
    }
}
