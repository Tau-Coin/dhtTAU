package io.taucoin.types;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

// index频道数据结构,即XY的通信历史同步数据
public class IndexMutableData {
    private byte[] deviceID; // 设备ID
    private BigInteger timestamp; // 发布index数据时的时间戳
    private List<byte[]> hashList = new CopyOnWriteArrayList<>(); // index hash数据列表，目前可放46个hash

    // TODO:: 长期历史数据的同步策略还需研究

    // 辅助数据
    private byte[] rlpEncoded; // 编码数据
    private boolean parsed = false; // 解析标志

    public IndexMutableData(byte[] deviceID, List<byte[]> hashList) {
        this(deviceID, BigInteger.valueOf(System.currentTimeMillis() / 1000), hashList);
    }

    public IndexMutableData(byte[] deviceID, BigInteger timestamp, List<byte[]> hashList) {
        this.deviceID = deviceID;
        this.timestamp = timestamp;
        this.hashList = hashList;

        this.parsed = true;
    }

    public IndexMutableData(byte[] encode) {
        this.rlpEncoded = encode;
    }

    public byte[] getDeviceID() {
        if (!parsed) {
            parseRLP();
        }

        return deviceID;
    }

    public BigInteger getTimestamp() {
        if (!parsed) {
            parseRLP();
        }

        return timestamp;
    }

    public List<byte[]> getHashList() {
        if (!parsed) {
            parseRLP();
        }

        return hashList;
    }

    /**
     * parse rlp encode
     */
    private void parseRLP() {
        RLPList params = RLP.decode2(this.rlpEncoded);
        RLPList list = (RLPList) params.get(0);

        this.deviceID = list.get(0).getRLPData();

        byte[] timeBytes = list.get(1).getRLPData();
        this.timestamp = (null == timeBytes) ? BigInteger.ZERO: new BigInteger(1, timeBytes);

        parseHashList((RLPList) list.get(2));

        this.parsed = true;
    }

    private void parseHashList(RLPList list) {
        for (int i = 0; i < list.size(); i++) {
            byte[] encode = list.get(i).getRLPData();
            this.hashList.add(encode);
        }
    }

    public byte[] getHashListEncoded() {
        byte[][] hashListEncoded = new byte[this.hashList.size()][];
        int i = 0;
        for (byte[] hash : this.hashList) {
            hashListEncoded[i] = RLP.encodeElement(hash);
            ++i;
        }
        return RLP.encodeList(hashListEncoded);
    }

    /**
     * get encoded hash list
     * @return encode
     */
    public byte[] getEncoded(){
        if (null == rlpEncoded) {
            byte[] deviceID = RLP.encodeElement(this.deviceID);
            byte[] timestamp = RLP.encodeBigInteger(this.timestamp);
            byte[] hashListEncoded = getHashListEncoded();

            this.rlpEncoded = RLP.encodeList(deviceID, timestamp, hashListEncoded);
        }

        return rlpEncoded;
    }

    @Override
    public String toString() {
        List<String> list = new ArrayList<>();

        byte[] deviceID = getDeviceID();
        if (null != deviceID) {
            list.add("deviceID: " + Hex.toHexString(deviceID));
        }

        BigInteger timeStamp = getTimestamp();

        List<byte[]> hashList = getHashList();
        if (null != hashList) {
            for (byte[] hash: hashList) {
                list.add(Hex.toHexString(hash));
            }
        }

        return "Index{timestamp: " + timeStamp + ", "  + list + '}';
    }

}
