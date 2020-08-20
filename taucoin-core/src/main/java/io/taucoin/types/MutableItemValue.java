package io.taucoin.types;

import com.frostwire.jlibtorrent.Entry;

import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

public class MutableItemValue {
    private static final Logger logger = LoggerFactory.getLogger("MutableItemValue");

    // content including tx/message hash, 原本哈希是20个字节，这里用3个long存储，最后一个long是补齐的，高位补齐
    ArrayList<Long> hash;
    // hash link or pubKey for optimization, 正好32个字节，4个long存储
    ArrayList<Long> peer;

    public MutableItemValue(byte[] hash, byte[] peer) {
        logger.debug("+++++++++++++++++++++++++++++++++++++++++++++");
        logger.debug("Hash: " + Hex.toHexString(hash));
        logger.debug("Peer: " + Hex.toHexString(peer));
        logger.debug("+++++++++++++++++++++++++++++++++++++++++++++");
        this.hash = ByteUtil.unAlignByteArrayToSignLongArray(hash, ChainParam.HashLongArrayLength);
        this.peer = ByteUtil.byteArrayToSignLongArray(peer, ChainParam.PubkeyLongArrayLength);
    }

    public MutableItemValue(byte[] encode) {
        logger.debug("------------encode:{}", new String(encode));
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
        if (null == this.hash) {
            logger.debug("++++++++++++++++++++++:hash is null");
            list.add(new ArrayList<Long>());
        } else {
            list.add(this.hash);
        }
        if (null == this.peer) {
            logger.debug("++++++++++++++++++++++:peer is null");
            list.add(new ArrayList<Long>());
        } else {
            list.add(this.peer);
        }
        Entry entry = Entry.fromList(list);

        byte[] encode = entry.bencode();
        logger.debug("------------------encode:{}", Hex.toHexString(encode));
        return encode;
    }

    @Override
    public String toString() {
        return "MutableItemValue{" +
                "hash=" + Hex.toHexString(getHash()) +
                ", peer=" + Hex.toHexString(getPeer()) +
                '}';
    }
}
