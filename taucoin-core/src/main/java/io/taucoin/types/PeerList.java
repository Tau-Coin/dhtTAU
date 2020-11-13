package io.taucoin.types;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.taucoin.util.HashUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class PeerList {
    private byte[] previousPeerListRoot;
    private List<byte[]> peerList = new CopyOnWriteArrayList<>();

    private byte[] hash;

    private byte[] encode;
    private boolean parsed = false;

    public PeerList(byte[] previousPeerListRoot, List<byte[]> peerList) {
        this.previousPeerListRoot = previousPeerListRoot;
        this.peerList = peerList;

        this.parsed = true;
    }

    public PeerList(byte[] encode) {
        this.encode = encode;
    }

    public byte[] getPreviousPeerListRoot() {
        if (!this.parsed) {
            parseRLP();
        }

        return previousPeerListRoot;
    }


    public List<byte[]> getPeerList() {
        if (!this.parsed) {
            parseRLP();
        }

        return peerList;
    }

    public byte[] getHash() {
        if (null == this.hash) {
            HashUtil.bencodeHash(getEncoded());
        }

        return this.hash;
    }

    private void parseList(RLPList list) {
        for (int i = 0; i < list.size(); i++) {
            this.peerList.add(list.get(i).getRLPData());
        }
    }

    private void parseRLP() {
        RLPList params = RLP.decode2(this.encode);
        RLPList list = (RLPList) params.get(0);

        this.previousPeerListRoot = list.get(0).getRLPData();
        if (2 == list.size()) {
            parseList((RLPList) list.get(1));
        }

        this.parsed = true;
    }

    public byte[] getPeerListEncoded() {
        byte[][] peerListEncoded = new byte[this.peerList.size()][];
        int i = 0;
        for (byte[] peer : this.peerList) {
            peerListEncoded[i] = RLP.encodeElement(peer);
            ++i;
        }
        return RLP.encodeList(peerListEncoded);
    }

    public byte[] getEncoded() {
        if (null == this.encode) {
            byte[] previousRoot = RLP.encodeElement(this.previousPeerListRoot);
            byte[] peerListEncode = getPeerListEncoded();

            this.encode = RLP.encodeList(previousRoot, peerListEncode);
        }

        return this.encode;
    }

}
