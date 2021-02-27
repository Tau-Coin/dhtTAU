package io.taucoin.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class FriendList {
    private List<byte[]> friendList = new CopyOnWriteArrayList<>();

    private byte[] rlpEncoded; // 编码数据
    private boolean parsed = false; // 解析标志

    public FriendList(List<byte[]> friendList) {
        this.friendList = friendList;

        this.parsed = true;
    }

    public FriendList(byte[] rlpEncoded) {
        this.rlpEncoded = rlpEncoded;
    }

    public List<byte[]> getFriendList() {
        if (!parsed) {
            parseRLP();
        }

        return friendList;
    }

    /**
     * parse rlp encode
     */
    private void parseRLP() {
        RLPList params = RLP.decode2(this.rlpEncoded);
        RLPList list = (RLPList) params.get(0);

        this.friendList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            this.friendList.add(list.get(i).getRLPData());
        }

        this.parsed = true;
    }

    /**
     * get encoded
     * @return encode
     */
    public byte[] getEncoded(){
        if (null == rlpEncoded) {
            if (null != this.friendList && !this.friendList.isEmpty()) {
                byte[][] encodeList = new byte[this.friendList.size()][];

                int i = 0;
                for (byte[] friend : this.friendList) {
                    encodeList[i] = RLP.encodeElement(friend);
                    i++;
                }

                rlpEncoded = RLP.encodeList(encodeList);
            }
        }

        return rlpEncoded;
    }
}
