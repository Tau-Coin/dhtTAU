package io.taucoin.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

// TODO::考虑加密
public class FriendInfoList {
    private byte[] deviceID;
    private List<FriendInfo> friendInfoList = new CopyOnWriteArrayList<>();

    private byte[] rlpEncoded; // 编码数据
    private boolean parsed = false; // 解析标志

    public FriendInfoList(byte[] deviceID, List<FriendInfo> friendInfoList) {
        this.deviceID = deviceID;
        this.friendInfoList = friendInfoList;

        this.parsed = true;
    }

    public FriendInfoList(byte[] rlpEncoded) {
        this.rlpEncoded = rlpEncoded;
    }

    public byte[] getDeviceID() {
        if (!parsed) {
            parseRLP();
        }

        return deviceID;
    }

    public List<FriendInfo> getFriendInfoList() {
        if (!parsed) {
            parseRLP();
        }

        return friendInfoList;
    }

    private void parseFriendList(RLPList list) {
        this.friendInfoList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            this.friendInfoList.add(new FriendInfo(list.get(i).getRLPData()));
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
        if (null != this.friendInfoList) {
            byte[][] encodeList = new byte[this.friendInfoList.size()][];

            int i = 0;
            for (FriendInfo friendInfo : this.friendInfoList) {
                encodeList[i] = RLP.encodeElement(friendInfo.getEncoded());
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
