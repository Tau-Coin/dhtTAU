package io.taucoin.torrent.publishing.ui.constant;

import java.util.List;

/**
 * QR内容
 */
public class QRContent {
    private String publicKey;
    private String nickName;
    private List<String> friendPks;

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public void setFriendPks(List<String> friendPks) {
        this.friendPks = friendPks;
    }
}