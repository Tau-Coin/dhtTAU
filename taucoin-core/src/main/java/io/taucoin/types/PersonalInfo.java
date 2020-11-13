package io.taucoin.types;

import java.util.List;

import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class PersonalInfo {
    private byte[] userName;
    private byte[] iconRoot;
    private byte[] peerListRoot;

    private byte[] encode;
    private boolean parsed = false;

    public PersonalInfo(byte[] userName, byte[] iconRoot, byte[] peerListRoot) {
        this.userName = userName;
        this.iconRoot = iconRoot;
        this.peerListRoot = peerListRoot;

        this.parsed = true;
    }

    public PersonalInfo(byte[] encode) {
        this.encode = encode;
    }

    public byte[] getUserName() {
        if (!this.parsed) {
            parseRLP();
        }

        return userName;
    }

    public void setUserName(byte[] userName) {
        this.userName = userName;
        this.parsed = false;
    }

    public byte[] getIconRoot() {
        if (!this.parsed) {
            parseRLP();
        }

        return iconRoot;
    }

    public void setIconRoot(byte[] iconRoot) {
        this.iconRoot = iconRoot;
        this.parsed = false;
    }

    public byte[] getPeerListRoot() {
        if (!this.parsed) {
            parseRLP();
        }

        return peerListRoot;
    }

    public void setPeerListRoot(byte[] peerListRoot) {
        this.peerListRoot = peerListRoot;
        this.parsed = false;
    }

    private void parseRLP() {
        RLPList params = RLP.decode2(this.encode);
        RLPList personalInfo = (RLPList) params.get(0);

        this.userName = personalInfo.get(0).getRLPData();
        this.iconRoot = personalInfo.get(1).getRLPData();
        this.peerListRoot = personalInfo.get(2).getRLPData();

        this.parsed = true;
    }

    public byte[] getEncoded() {
        if (null == this.encode) {
            byte[] userName = RLP.encodeElement(this.userName);
            byte[] iconRoot = RLP.encodeElement(this.iconRoot);
            byte[] peerListRoot = RLP.encodeElement(this.peerListRoot);

            this.encode = RLP.encodeList(userName, iconRoot, peerListRoot);
        }

        return this.encode;
    }
}
