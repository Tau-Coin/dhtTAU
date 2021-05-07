package io.taucoin.core;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;

import io.taucoin.util.HashUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class OnlineSignal {
    private byte[] deviceID;
    byte[] confirmationHash = null;
    byte[] hashPrefixArray = null;
    FriendInfo friendInfo = null;
    BigInteger timestamp;

    private byte[] hash;
    private byte[] rlpEncoded; // 编码数据
    private boolean parsed = false; // 解析标志

    public OnlineSignal(byte[] deviceID, byte[] confirmationHash, byte[] hashPrefixArray, FriendInfo friendInfo, BigInteger timestamp) {
        this.deviceID = deviceID;
        this.confirmationHash = confirmationHash;
        this.hashPrefixArray = hashPrefixArray;
        this.friendInfo = friendInfo;
        this.timestamp = timestamp;

        this.parsed = true;
    }

    public OnlineSignal(byte[] rlpEncoded) {
        this.rlpEncoded = rlpEncoded;
    }

    public byte[] getDeviceID() {
        if (!parsed) {
            parseRLP();
        }

        return deviceID;
    }

    public byte[] getConfirmationHash() {
        if (!parsed) {
            parseRLP();
        }

        return confirmationHash;
    }

    public byte[] getHashPrefixArray() {
        if (!parsed) {
            parseRLP();
        }

        return hashPrefixArray;
    }

    public FriendInfo getFriendInfo() {
        if (!parsed) {
            parseRLP();
        }

        return friendInfo;
    }

    public BigInteger getTimestamp() {
        if (!parsed) {
            parseRLP();
        }

        return timestamp;
    }

    public byte[] getHash() {
        if (null == this.hash) {
            this.hash = HashUtil.bencodeHash(getEncoded());
        }

        return this.hash;
    }

    /**
     * parse rlp encode
     */
    private void parseRLP() {
        RLPList params = RLP.decode2(this.rlpEncoded);
        RLPList list = (RLPList) params.get(0);

        this.deviceID = list.get(0).getRLPData();

        this.confirmationHash = list.get(1).getRLPData();

        this.hashPrefixArray = list.get(2).getRLPData();

        byte[] friendInfoBytes = list.get(3).getRLPData();
        if (null != friendInfoBytes) {
            this.friendInfo = new FriendInfo(friendInfoBytes);
        }

        byte[] timeBytes = list.get(4).getRLPData();
        this.timestamp = (null == timeBytes) ? BigInteger.ZERO: new BigInteger(1, timeBytes);

        this.parsed = true;
    }

    /**
     * get encoded hash list
     * @return encode
     */
    public byte[] getEncoded(){
        if (null == rlpEncoded) {
            byte[] deviceID = RLP.encodeElement(this.deviceID);
            byte[] confirmationHash = RLP.encodeElement(this.confirmationHash);
            byte[] hashPrefixArray = RLP.encodeElement(this.hashPrefixArray);
            byte[] friendInfo = RLP.encodeElement(null);
            if (null != this.friendInfo) {
                friendInfo = RLP.encodeElement(this.friendInfo.getEncoded());
            }
            byte[] timestamp = RLP.encodeBigInteger(this.timestamp);

            this.rlpEncoded = RLP.encodeList(deviceID, confirmationHash, hashPrefixArray, friendInfo, timestamp);
        }

        return rlpEncoded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OnlineSignal onlineSignal = (OnlineSignal) o;
        return Arrays.equals(getHash(), onlineSignal.getHash());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getHash());
    }

    @Override
    public String toString() {
        byte[] deviceID = getDeviceID();
        byte[] confirmationHash = getConfirmationHash();
        byte[] hashPrefixArray = getHashPrefixArray();
        FriendInfo friendInfo = getFriendInfo();
        BigInteger time = getTimestamp();

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("OnlineSignal{");
        if (null != deviceID) {
            stringBuilder.append("device id=");
            stringBuilder.append(Hex.toHexString(deviceID));
        }

        if (null != confirmationHash) {
            stringBuilder.append(", confirmation hash=");
            stringBuilder.append(Hex.toHexString(confirmationHash));
        }
        if (null != hashPrefixArray) {
            stringBuilder.append(", hash prefix array=");
            stringBuilder.append(Hex.toHexString(hashPrefixArray));
        }
        if (null != friendInfo) {
            stringBuilder.append(", friend info=");
            stringBuilder.append(friendInfo.toString());
        }
        if (null != time) {
            stringBuilder.append(", time=");
            stringBuilder.append(time);
        }

        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
