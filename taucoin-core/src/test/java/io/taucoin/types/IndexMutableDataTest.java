package io.taucoin.types;

import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class IndexMutableDataTest {

    @Test
    public void testGossipListCodec() {
        byte[] deviceID = Hex.decode("df66086cba1c1d916fcdbc0d7b9752d9");

        List<byte[]> list = new ArrayList<>();
        byte[] hash = Hex.decode("b52d6aef0afdc33c610708532d135ed8ac3f5ebc");


        for (int i = 0; i < 46; i++) {
            list.add(hash);
        }

        IndexMutableData index = new IndexMutableData(deviceID, list);

        System.out.print(index.toString());
        System.out.print("\n");

        byte[] encode = index.getEncoded();
        System.out.print(encode.length); // 994 bytes
        System.out.print("\n");

        IndexMutableData index1 = new IndexMutableData(encode);
        System.out.print(index1.toString());
    }
}
