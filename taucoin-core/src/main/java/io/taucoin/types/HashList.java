package io.taucoin.types;

import java.util.ArrayList;
import java.util.List;

import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class HashList {
    private List<byte[]> hashList;
    private byte[] rlpEncoded;

    public HashList(List<byte[]> hashList) {
        this.hashList = hashList;
    }

    public HashList(byte[] encode) {
        this.rlpEncoded = encode;
    }

    public List<byte[]> getHashList() {
        return this.hashList;
    }

    private void parseRLP() {
        RLPList params = RLP.decode2(this.rlpEncoded);
        RLPList list = (RLPList) params.get(0);
        this.hashList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            this.hashList.add(list.get(i).getRLPData());
        }
    }

    public byte[] getEncoded(){
        if (null != this.hashList && this.hashList.size() > 1) {
            byte[][] encodeList = new byte[this.hashList.size()][];

            int i = 0;
            for (byte[] hash: this.hashList) {
                encodeList[i] = RLP.encodeElement(hash);
                i++;
            }

            return RLP.encodeList(encodeList);
        }

        return null;
    }
}
