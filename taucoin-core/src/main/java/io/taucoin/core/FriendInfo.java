package io.taucoin.core;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;

import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class FriendInfo {
    byte[] pubKey;
    byte[] nickname;
    BigInteger timestamp;

    private byte[] rlpEncoded; // 编码数据
    private boolean parsed = false; // 解析标志

    public FriendInfo(byte[] pubKey, byte[] nickname, BigInteger timestamp) {
        this.pubKey = pubKey;
        this.nickname = nickname;
        this.timestamp = timestamp;

        this.parsed = true;
    }

    public FriendInfo(byte[] rlpEncoded) {
        this.rlpEncoded = rlpEncoded;
    }

    public byte[] getPubKey() {
        if (!parsed) {
            parseRLP();
        }

        return pubKey;
    }

    public byte[] getNickname() {
        if (!parsed) {
            parseRLP();
        }

        return nickname;
    }

    public BigInteger getTimestamp() {
        if (!parsed) {
            parseRLP();
        }

        return timestamp;
    }

    /**
     * parse rlp encode
     */
    private void parseRLP() {
        RLPList params = RLP.decode2(this.rlpEncoded);
        RLPList list = (RLPList) params.get(0);

        this.pubKey = list.get(0).getRLPData();

        this.nickname = list.get(1).getRLPData();

        byte[] timeBytes = list.get(2).getRLPData();
        this.timestamp = (null == timeBytes) ? BigInteger.ZERO: new BigInteger(1, timeBytes);

        this.parsed = true;
    }

    /**
     * get encoded hash list
     * @return encode
     */
    public byte[] getEncoded(){
        if (null == rlpEncoded) {
            byte[] pubKey = RLP.encodeElement(this.pubKey);
            byte[] nickname = RLP.encodeElement(this.nickname);
            byte[] timestamp = RLP.encodeBigInteger(this.timestamp);

            this.rlpEncoded = RLP.encodeList(pubKey, nickname, timestamp);
        }

        return rlpEncoded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FriendInfo that = (FriendInfo) o;
        return Arrays.equals(pubKey, that.pubKey);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(pubKey);
    }

    @Override
    public String toString() {

        byte[] pubKey = getPubKey();
        byte[] nickname = getNickname();
        BigInteger timestamp = getTimestamp();

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("{");
        if (null != pubKey) {
            stringBuilder.append("public key=");
            stringBuilder.append(Hex.toHexString(pubKey));
        }
        if (null != nickname) {
            stringBuilder.append(", nickname=");
            stringBuilder.append(new String(nickname));
        }
        if (null != timestamp) {
            stringBuilder.append(", timestamp=");
            stringBuilder.append(timestamp);
        }

        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
