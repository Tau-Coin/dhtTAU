package io.taucoin.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.taucoin.types.GossipItem;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

// TODO::考虑加密
public class FriendList {
    private byte[] deviceID;
    // 可以放29个朋友的公钥
    private List<byte[]> friendList = new CopyOnWriteArrayList<>();

    private byte[] rlpEncoded; // 编码数据
    private boolean parsed = false; // 解析标志

    public FriendList(byte[] deviceID, List<byte[]> friendList) {
        this.deviceID = deviceID;
        this.friendList = friendList;

        this.parsed = true;
    }

    public FriendList(byte[] rlpEncoded) {
        this.rlpEncoded = rlpEncoded;
    }

    public byte[] getDeviceID() {
        if (!parsed) {
            parseRLP();
        }

        return deviceID;
    }

    public List<byte[]> getFriendList() {
        if (!parsed) {
            parseRLP();
        }

        return friendList;
    }

    private void parseFriendList(RLPList list) {
        this.friendList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            this.friendList.add(list.get(i).getRLPData());
        }
    }

    /**
     * parse rlp encode
     */
    private void parseRLP() {
        RLPList params = RLP.decode2(this.rlpEncoded);
        RLPList list = (RLPList) params.get(0);

        this.deviceID = list.get(0).getRLPData();

        parseFriendList((RLPList) list.get(1));

        this.parsed = true;
    }

    public byte[] getFriendListEncoded() {
        if (null != this.friendList) {
            byte[][] encodeList = new byte[this.friendList.size()][];

            int i = 0;
            for (byte[] friend : this.friendList) {
                encodeList[i] = RLP.encodeElement(friend);
                i++;
            }

            return RLP.encodeList(encodeList);
        }

        return null;
    }

    /**
     * get encoded
     * @return encode
     */
    public byte[] getEncoded(){
        if (null == rlpEncoded) {
            byte[] deviceID = RLP.encodeElement(this.deviceID);
            byte[] friendListEncode = getFriendListEncoded();

            this.rlpEncoded = RLP.encodeList(deviceID, friendListEncode);
        }

        return rlpEncoded;
    }
}
