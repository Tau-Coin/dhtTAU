package io.taucoin.types;

import com.frostwire.jlibtorrent.Entry;

import java.util.ArrayList;
import java.util.List;

import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteUtil;

public class HashList {
    List<byte[]> hashList;

    public HashList(List<byte[]> hashList) {
        this.hashList = hashList;
    }

    public HashList(byte[] encode) {
        Entry entry = Entry.bdecode(encode);
        List<Entry> entryList = entry.list();

        this.hashList = new ArrayList<>();
        if (null != entryList) {
            for (Entry entryItem: entryList) {
                this.hashList.add(ByteUtil.longArrayToBytes(ByteUtil.stringToLongArrayList(entryItem.toString()), ChainParam.HashLength));
            }
        }
    }

    public List<byte[]> getHashList() {
        return this.hashList;
    }

    public byte[] getEncoded(){
        List list = new ArrayList();
        if (null != this.hashList) {
            for (byte[] hash: this.hashList) {
                list.add(ByteUtil.unAlignByteArrayToSignLongArray(hash, ChainParam.HashLongArrayLength));
            }
        }
        Entry entry = Entry.fromList(list);

        return entry.bencode();
    }
}
