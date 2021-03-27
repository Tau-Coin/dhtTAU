package io.taucoin.types;

import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

import io.taucoin.core.FriendInfoList;

public class FriendInfoListTest {

//    @Test
//    public void testCodec1() {
//        byte[] deviceID = new byte[4];
//        byte[] pubKey1 = Hex.decode("2a62868271f3d3455e4b1ea0c1f96263732d0347349f9daa3247107ce1b2b2f9");
//        byte[] pubKey2 = Hex.decode("3e87c35d2079858d88dcb113edadaf1b339fcd4f74c539faa9a9bd59e787f124");
//        List<byte[]> list = new ArrayList<>();
//        list.add(pubKey1);
//        list.add(pubKey2);
//
//        FriendInfoList friendInfoList = new FriendInfoList(deviceID, list);
//        for (byte[] pubKey: list) {
//            System.out.println(Hex.toHexString(pubKey));
//        }
//
//        byte[] encode = friendInfoList.getEncoded();
//        FriendInfoList friendInfoList1 = new FriendInfoList(encode);
//        for (byte[] pubKey: friendInfoList1.getFriendInfoList()) {
//            System.out.println(Hex.toHexString(pubKey));
//        }
//    }
}
