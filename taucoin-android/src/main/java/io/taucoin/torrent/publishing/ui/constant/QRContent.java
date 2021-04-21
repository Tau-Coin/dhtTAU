package io.taucoin.torrent.publishing.ui.constant;

/**
 * QR内容
 */
public class QRContent {
    private String publicKey;
    private String nickName;

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
}