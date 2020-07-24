package io.taucoin.core;

import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;
import org.spongycastle.util.encoders.Hex;

public class MutableItemValue {
    byte[] hash;
    byte[] peer;

    public MutableItemValue(byte[] hash, byte[] peer) {
        this.hash = hash;
        this.peer = peer;
    }

    public MutableItemValue(byte[] rlp) {
        RLPList decodedTxList = RLP.decode2(rlp);
        RLPList item = (RLPList) decodedTxList.get(0);
        this.hash = item.get(0).getRLPData();
        this.peer = item.get(1).getRLPData();
    }

    public byte[] getHash() {
        return hash;
    }

    public void setHash(byte[] hash) {
        this.hash = hash;
    }

    public byte[] getPeer() {
        return peer;
    }

    public void setPeer(byte[] peer) {
        this.peer = peer;
    }

    public byte[] getEncoded() {
        byte[] hash = RLP.encodeElement(this.hash);
        byte[] peer = RLP.encodeElement(this.peer);
        return RLP.encodeList(hash, peer);
    }

    @Override
    public String toString() {
        return "MutableItemValue{" +
                "hash=" + Hex.toHexString(hash) +
                ", peer=" + Hex.toHexString(peer) +
                '}';
    }
}
