package io.taucoin.repository;

import java.math.BigInteger;

/**
 * 朋友昵称信息类
 * 用于多设备昵称同步
 */
public class NicknameBean {
    private byte[] friendPk;        // 朋友公钥
    private byte[] nickname;        // 朋友昵称
    private BigInteger timestamp;   // 昵称被修改的时间戳

    public byte[] getFriendPk() {
        return friendPk;
    }

    public void setFriendPk(byte[] friendPk) {
        this.friendPk = friendPk;
    }

    public byte[] getNickname() {
        return nickname;
    }

    public void setNickname(byte[] nickname) {
        this.nickname = nickname;
    }

    public BigInteger getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(BigInteger timestamp) {
        this.timestamp = timestamp;
    }
}
