package io.taucoin.torrent.publishing.ui.constant;

/**
 * QR内容
 */
public class KeyQRContent extends QRContent{
    private String seed;

    public String getSeed() {
        return seed;
    }

    public void setSeed(String seed) {
        this.seed = seed;
    }
}