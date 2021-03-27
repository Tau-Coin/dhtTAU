package io.taucoin.types;

import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

import io.taucoin.core.FriendInfoList;
import io.taucoin.core.MutableDataWrapper;

public class MutableDataWrapperTest {

//    @Test
//    public void testFriendListCap() {
//        byte[] deviceID = new byte[4];
//        List<byte[]> list = new ArrayList<>();
//        byte[] pubKey = Hex.decode("2a62868271f3d3455e4b1ea0c1f96263732d0347349f9daa3247107ce1b2b2f9");
//        for (int i = 0; i < 29; i++) {
//            list.add(pubKey);
//        }
//
//        FriendInfoList friendInfoList = new FriendInfoList(deviceID, list);
//        MutableDataWrapper mutableDataWrapper =
//                new MutableDataWrapper(MutableDataType.FRIEND_LIST, friendInfoList.getEncoded());
//        int length = mutableDataWrapper.getEncoded().length;
//        // 972 < DHT_ITEM_LIMIT_SIZE = 996
//        System.out.println(length);
//    }
//
//    @Test
//    public void testEmptyWrapper() {
//        MutableDataWrapper mutableDataWrapper = new MutableDataWrapper(MutableDataType.MESSAGE_LIST, new byte[2]);
//        System.out.println(mutableDataWrapper.getEncoded().length);
//    }
}
