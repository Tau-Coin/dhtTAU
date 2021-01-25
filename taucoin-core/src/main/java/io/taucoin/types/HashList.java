package io.taucoin.types;

import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.taucoin.util.HashUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class HashList {
    private List<byte[]> hashList = new CopyOnWriteArrayList<>();

    private byte[] hash;
    private byte[] rlpEncoded;

    private boolean parsed = false;

    public HashList(List<byte[]> hashList) {
        this.hashList = hashList;
        this.parsed = true;
    }

    public HashList(byte[] encode) {
        this.rlpEncoded = encode;
    }

    /**
     * create a hash list with one hash
     * @param hash hash
     * @return hash list
     */
    public static HashList with(byte[] hash) {
        List<byte[]> list = new ArrayList<>();
        list.add(hash);

        return new HashList(list);
    }

    /**
     * get hash list
     * @return hash list
     */
    public List<byte[]> getHashList() {
        if (!parsed) {
            parseRLP();
        }

        return this.hashList;
    }

    /**
     * get first hash
     * @return first hash
     */
    public byte[] getFirstHash() {
        List<byte[]> list = getHashList();
        if (null != list && !list.isEmpty()) {
            return list.get(0);
        }

        return null;
    }

    /**
     * parse rlp encode
     */
    private void parseRLP() {
        RLPList params = RLP.decode2(this.rlpEncoded);
        RLPList list = (RLPList) params.get(0);

        this.hashList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            byte[] hashByte = list.get(i).getRLPData();
            this.hashList.add(hashByte);
        }

        this.parsed = true;
    }

    /**
     * get encoded hash list
     * @return encode
     */
    public byte[] getEncoded(){
        if (null == rlpEncoded) {
            if (null != this.hashList && !this.hashList.isEmpty()) {
                byte[][] encodeList = new byte[this.hashList.size()][];

                int i = 0;
                for (byte[] hash : this.hashList) {
                    encodeList[i] = RLP.encodeElement(hash);
                    i++;
                }

                rlpEncoded = RLP.encodeList(encodeList);
            }
        }

        return rlpEncoded;
    }

    /**
     * get hash list item hash
     * @return hash
     */
    public byte[] getHash(){
        if(null == this.hash) {
            this.hash = HashUtil.bencodeHash(this.getEncoded());
        }

        return this.hash;
    }

    @Override
    public String toString() {
        List<String> list = new ArrayList<>();
        list.add("Hash:" + Hex.toHexString(getHash()));
        List<byte[]> hashList = getHashList();
        if (null != hashList) {
            int i = 0;
            for (byte[] hash: hashList) {
                list.add("[" + i + "]: " + Hex.toHexString(hash));
                i++;
            }
        }

        return "HashList{" + list + '}';
    }
}
