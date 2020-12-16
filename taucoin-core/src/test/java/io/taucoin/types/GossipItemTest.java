package io.taucoin.types;

import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

import io.taucoin.util.ByteUtil;

public class GossipItemTest {

    @Test
    public void testCodec1() {
        byte[] sender = Hex.decode("2a62868271f3d3455e4b1ea0c1f96263732d0347349f9daa3247107ce1b2b2f9");
        byte[] receiver = Hex.decode("3e87c35d2079858d88dcb113edadaf1b339fcd4f74c539faa9a9bd59e787f124");
        BigInteger timeStamp = BigInteger.valueOf(System.currentTimeMillis() / 1000);
        byte[] messageRoot = Hex.decode("2eac92b256b6960eefa5b105fe7ab1322b796245");
        byte[] confirmationRoot = Hex.decode("3eac92b256b6960eefa5b105fe7ab1322b796245");

        GossipItem gossipItem = new GossipItem(sender, receiver, timeStamp, messageRoot, confirmationRoot);

        byte[] encode = gossipItem.getEncoded();

        System.out.print(gossipItem.toString());

        GossipItem gossipItem1 = new GossipItem(encode);
        System.out.print(gossipItem1.toString());
        System.out.print(encode.length); // 120

    }

    @Test
    public void testCodec2() {
        byte[] sender = Hex.decode("2a62868271f3d3455e4b1ea0c1f96263732d0347349f9daa3247107ce1b2b2f9");
        byte[] receiver = Hex.decode("3e87c35d2079858d88dcb113edadaf1b339fcd4f74c539faa9a9bd59e787f124");
        BigInteger timeStamp = BigInteger.valueOf(System.currentTimeMillis() / 1000);
        byte[] messageRoot = Hex.decode("2eac92b256b6960eefa5b105fe7ab1322b796245");
        byte[] confirmationRoot = null;

        GossipItem gossipItem = new GossipItem(sender, receiver, timeStamp, messageRoot, confirmationRoot);

        byte[] encode = gossipItem.getEncoded();

        System.out.print(gossipItem.toString());

        GossipItem gossipItem1 = new GossipItem(encode);
        System.out.print(gossipItem1.toString());

    }
}
