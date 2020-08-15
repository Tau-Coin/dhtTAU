package io.taucoin.core;

import com.frostwire.jlibtorrent.Entry;

import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

public class MutableItemValue {
    // content including tx/message hash
    ArrayList<Long> hash;
    // hash link or pubKey for optimization
    ArrayList<Long> peer;

    public MutableItemValue(byte[] hash, byte[] peer) {
        this.hash = ByteUtil.unAlignByteArrayToSignLongArray(hash, ChainParam.HashLongArrayLength);
        this.peer = ByteUtil.byteArrayToSignLongArray(peer, ChainParam.PubkeyLongArrayLength);
    }

    public MutableItemValue(byte[] encode) {
        Entry entry = Entry.bdecode(encode);
        List<Entry> entryList = entry.list();

        this.hash = ByteUtil.stringToLongArrayList(entryList.get(0).toString());
        this.peer = ByteUtil.stringToLongArrayList(entryList.get(1).toString());
    }

    public byte[] getHash() {
        byte[] longByte0 = ByteUtil.longToBytes(this.hash.get(0));
        byte[] longByte1 = ByteUtil.longToBytes(this.hash.get(1));
        byte[] longByte2 = ByteUtil.keep4bytesOfLong(this.hash.get(2));
        byte[] hash = new byte[ChainParam.HashLength];
        System.arraycopy(longByte0, 0, hash, 0, 8);
        System.arraycopy(longByte1, 0, hash, 8, 8);
        System.arraycopy(longByte2, 0, hash, 16, 4);
        return hash;
    }

    public byte[] getPeer() {
        byte[] longByte0 = ByteUtil.longToBytes(this.peer.get(0));
        byte[] longByte1 = ByteUtil.longToBytes(this.peer.get(1));
        byte[] longByte2 = ByteUtil.longToBytes(this.peer.get(2));
        byte[] longByte3 = ByteUtil.longToBytes(this.peer.get(3));

        byte[] pubKey = new byte[ChainParam.PubkeyLength];
        System.arraycopy(longByte0, 0, pubKey, 0, 8);
        System.arraycopy(longByte1, 0, pubKey, 8, 8);
        System.arraycopy(longByte2, 0, pubKey, 16, 8);
        System.arraycopy(longByte3, 0, pubKey, 24, 8);

        return pubKey;
    }

    public byte[] getEncoded(){
        List list = new ArrayList();
        list.add(this.hash);
        list.add(this.peer);
        Entry entry = Entry.fromList(list);

        return entry.bencode();
    }

    @Override
    public String toString() {
        return "MutableItemValue{" +
                "hash=" + Hex.toHexString(getHash()) +
                ", peer=" + Hex.toHexString(getPeer()) +
                '}';
    }
}
