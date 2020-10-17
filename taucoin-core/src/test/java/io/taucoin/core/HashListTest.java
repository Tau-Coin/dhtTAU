package io.taucoin.core;

import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

import io.taucoin.types.HashList;

public class HashListTest {

    private List<byte[]> makeHashList() {
        List<byte[]> list = new ArrayList<>();
        byte[] hash1 = Hex.decode("9738450c31228d0e4b8c29e4677515e30c2e64e1");
        list.add(hash1);
        byte[] hash2 = Hex.decode("9738450c31228d0e4b8c29e4677515e30c2e64e2");
        list.add(hash2);
        byte[] hash3 = Hex.decode("9738450c31228d0e4b8c29e4677515e30c2e64e3");
        list.add(hash3);
        byte[] hash4 = Hex.decode("9738450c31228d0e4b8c29e4677515e30c2e64e4");
        list.add(hash4);
        byte[] hash5 = Hex.decode("9738450c31228d0e4b8c29e4677515e30c2e64e5");
        list.add(hash5);

        return list;
    }

    @Test
    public void testGetHashList() {
        HashList hashList = new HashList(makeHashList());
        List<byte[]> list = hashList.getHashList();
        for (byte[] hash: list) {
            System.out.println(Hex.toHexString(hash));
        }
    }
}
