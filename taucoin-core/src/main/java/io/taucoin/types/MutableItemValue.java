package io.taucoin.types;

import com.frostwire.jlibtorrent.Entry;

import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

public class MutableItemValue {
    // content including tx/message hash, 原本哈希是20个字节，这里用3个long存储，最后一个long是补齐的，高位补齐
    ArrayList<Long> hash;
    // hash link or pubKey for optimization, 正好32个字节，4个long存储
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
        return ByteUtil.longArrayToBytes(this.hash, ChainParam.HashLength);
    }

    public byte[] getPeer() {
        return ByteUtil.longArrayToBytes(this.peer, ChainParam.PubkeyLength);
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
