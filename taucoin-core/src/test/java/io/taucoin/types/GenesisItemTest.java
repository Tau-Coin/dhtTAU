/**
Copyright 2020 taucoin developer

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
(the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
OR OTHER DEALINGS IN THE SOFTWARE.
*/
package io.taucoin.types;

import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Pair;

import io.taucoin.genesis.GenesisItem;
import io.taucoin.util.RLP;

import java.math.BigInteger;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

public class GenesisItemTest {
    private  static final byte[] seed;
    private  static final byte[] sender;

    static {
        seed = Ed25519.createSeed();
        Pair<byte[], byte[]> keys = Ed25519.createKeypair(seed);
        sender = keys.first;
    }

    @Test
    public void createGenesisItem(){
        BigInteger abalance = BigInteger.valueOf(10000000L);
        BigInteger anonce = BigInteger.valueOf(100000000L);
        GenesisItem item = new GenesisItem(sender, abalance, anonce);

        System.out.println("Account: " + Hex.toHexString(item.getAccount()));
        System.out.println("Balance: " + item.getBalance());
        System.out.println("Power: " + item.getPower());

        System.out.println("GenesisItem String: "+ Hex.toHexString(item.getEncoded()));

        GenesisItem value = new GenesisItem(item.getEncoded());
        System.out.println("Account: " + Hex.toHexString(value.getAccount()));
        System.out.println("Balance: " + value.getBalance());
        System.out.println("Power: " + value.getPower());

    }

}
