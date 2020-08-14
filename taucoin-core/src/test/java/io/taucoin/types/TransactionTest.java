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

import io.taucoin.genesis.GenesisItem;
import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.ByteUtil;

import java.math.BigInteger;
import java.util.HashMap;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionTest {
    private static final Logger log = LoggerFactory.getLogger("txTest");
    private static final long version = 1;
    private static final byte[] chainid = "TAUcoin#300#3938383036366633393364383365393338373434".getBytes();
    private static final long timestamp = 1597071234L;
    private static final long txFee = 800000000000000L;
    private static final byte[] sender = ByteUtil.toByte("ae20f5f96e89b8945a1194749456f74357864d5902ee8a5c19c3e75d0cef91ea");
    private static final long nonce = 8000L;
    private static final byte[] signature = ByteUtil.toByte("281f3c2fe309683c74762f965f38bd8f8910d8dbeca1da904d6821e8101075776243379a4efdfdc8c10ae34be767a825f770e6a62b5430c030f179b74057e747");

    @Test
    public void createGenesisMsg() {

        long txType = ChainParam.TxType.GMsgType.ordinal();

        BigInteger balance = new BigInteger("fffff", 16);
        BigInteger power = new BigInteger("ff", 16);

        GenesisItem gItem= new GenesisItem(balance, power);
        ByteArrayWrapper account = new ByteArrayWrapper(ByteUtil.toByte("ae20f5f96e89b8945a1194749456f74357864d5902ee8a5c19c3e75d0cef91ea"));
        HashMap<ByteArrayWrapper, GenesisItem> gMsg = new HashMap();
        gMsg.put(account, gItem);

        Transaction tx = new Transaction(version, chainid, timestamp, txFee, txType,
                    sender, nonce, gMsg, null, null, 0, signature);

        System.out.println(tx.getVersion());
        byte[] bencoded= tx.getEncoded();
        String str = new String(bencoded);
        System.out.println(str);
        System.out.println(str.length());
        System.out.println(ByteUtil.toHexString(tx.getTxID()));
        boolean ret1 = tx.isTxParamValidate();

        System.out.println("param validate ?: "+ret1);
    }

    @Test
    public void createForumNoteTx() {
        long txType = ChainParam.TxType.FNoteType.ordinal();
        byte[] forumNote = "Hello, Taucoin".getBytes();

        Transaction tx = new Transaction(version, chainid, timestamp, txFee, txType,
                    sender, nonce, null, forumNote, null, 0, signature);

        System.out.println(tx.getVersion());
        byte[] bencoded= tx.getEncoded();
        String str = new String(bencoded);
        System.out.println(str);
        System.out.println(str.length());
        System.out.println(tx.getForumNote());
        System.out.println(ByteUtil.toHexString(tx.getTxID()));
        boolean ret1 = tx.isTxParamValidate();
        System.out.println("param validate ?: "+ret1);
    }

    @Test
    public void createWiringCoinsTx() {
        long txType = ChainParam.TxType.WCoinsType.ordinal();
        byte[] receiver = ByteUtil.toByte("fe20f5f96e89b8945a1194749456f74357864d5902ee8a5c19c3e75d0cef91ea");
        long amount = 600000000000000L;

        Transaction tx = new Transaction(version, chainid, timestamp, txFee, txType,
                    sender, nonce, null, null, receiver, amount, signature);

        System.out.println(tx.getVersion());
        byte[] bencoded= tx.getEncoded();
        String str = new String(bencoded);
        System.out.println(str);
        System.out.println(str.length());
        System.out.println(ByteUtil.toHexString(tx.getTxID()));
        boolean ret1 = tx.isTxParamValidate();
        System.out.println("param validate ?: "+ret1);
    }

}
