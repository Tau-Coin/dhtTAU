package io.taucoin.types;

import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class GossipItemTest {

//    @Test
//    public void testCodec1() {
//        byte[] sender = Hex.decode("2a62868271f3d3455e4b1ea0c1f96263732d0347349f9daa3247107ce1b2b2f9");
//        byte[] receiver = Hex.decode("3e87c35d2079858d88dcb113edadaf1b339fcd4f74c539faa9a9bd59e787f124");
//        BigInteger timeStamp = BigInteger.valueOf(System.currentTimeMillis() / 1000);
//        byte[] messageRoot = Hex.decode("2eac92b256b6960eefa5b105fe7ab1322b796245");
//        byte[] confirmationRoot = Hex.decode("3eac92b256b6960eefa5b105fe7ab1322b796245");
//
//        GossipItem gossipItem = new GossipItem(sender, receiver, timeStamp, messageRoot, confirmationRoot);
//
//        byte[] encode = gossipItem.getEncoded();
//
//        System.out.print(gossipItem.toString());
//
//        GossipItem gossipItem1 = new GossipItem(encode);
//        System.out.print(gossipItem1.toString());
//        System.out.print(encode.length); // 120
//
//    }
//
//    @Test
//    public void testCodec2() {
//        byte[] sender = Hex.decode("2a62868271f3d3455e4b1ea0c1f96263732d0347349f9daa3247107ce1b2b2f9");
//        byte[] receiver = Hex.decode("3e87c35d2079858d88dcb113edadaf1b339fcd4f74c539faa9a9bd59e787f124");
//        BigInteger timeStamp = BigInteger.valueOf(System.currentTimeMillis() / 1000);
//        byte[] messageRoot = Hex.decode("2eac92b256b6960eefa5b105fe7ab1322b796245");
//        byte[] confirmationRoot = null;
//
//        GossipItem gossipItem = new GossipItem(sender, receiver, timeStamp, messageRoot, confirmationRoot);
//
//        byte[] encode = gossipItem.getEncoded();
//
//        System.out.print(gossipItem.toString());
//
//        GossipItem gossipItem1 = new GossipItem(encode);
//        System.out.print(gossipItem1.toString());
//
//    }

//    @Test
//    public void testGossipListCodec() {
//        List<GossipItem> list = new ArrayList<>();
//
//        byte[] sender = Hex.decode("2a628682");
//        byte[] receiver = Hex.decode("3e87c35d");
//        BigInteger timeStamp = BigInteger.valueOf(System.currentTimeMillis() / 1000);
//
//        GossipItem gossipItem = new GossipItem(sender, receiver, timeStamp);
//
//        for (int i = 0; i < 58; i++) {
//            list.add(gossipItem);
//        }
//
//        byte[] deviceID = Hex.decode("df66086cba1c1d916fcdbc0d7b9752d9");
//        byte[] friend = Hex.decode("2a62868271f3d3455e4b1ea0c1f96263732d0347349f9daa3247107ce1b2b2f9");
//        List<byte[]> friendList = new ArrayList<>();
//        friendList.add(friend);
//
//        GossipMutableData gossip = new GossipMutableData(deviceID, friendList, list);
//
//        System.out.print(gossip.toString());
//        System.out.print("\n");
//
//        byte[] encode = gossip.getEncoded();
//        System.out.print(encode.length); // 990 bytes
//        System.out.print("\n");
//
//        GossipMutableData gossip1 = new GossipMutableData(encode);
//        System.out.print(gossip1.toString());
//    }
}
