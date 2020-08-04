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
package io.taucoin.core.utypes;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.taucoin.utypes.Transaction;
import io.taucoin.utypes.TxData;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;

public class TransactionTest {
    private static final Logger log = LoggerFactory.getLogger("transactionTest");
    private static final String txdata = "6c313a34313a306c32303a2d3132353137383335303231383738363138313131393a3834393036303333373134393331363339343431393a3736323235343632303233333338353734353431393a2d39353439343033333133363731343231333032303a2d3536303334353433303237373433363230353331373a34333536393234393936363531393637336565";
    private static final byte[] chainid = "TAUcoin#300#3938383036366633393364383365393338373434".getBytes();
    private static final byte[] sender = ByteUtil.toByte("c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c");
    private static final byte[] signature = ByteUtil.toByte("281f3c2fe309683c74762f965f38bd8f8910d8dbeca1da904d6821e8101075776243379a4efdfdc8c10ae34be767a825f770e6a62b5430c030f179b74057e747");
    private static final String transaction = "6c313a3135323a544155636f696e233330302333393338333833303336333636363333333933333634333833333635333933333338333733343334393a313530303030303030333a3730306c32303a2d3432313237303335393733333535373931383032303a2d3538353234323932373236333232333535373632303a2d3530383833343339373636383531343534303931393a3638333330383536313934383230343730333665313a306c313a34313a306c32303a2d3132353137383335303231383738363138313131393a3834393036303333373134393331363339343431393a3736323235343632303233333338353734353431393a2d39353439343033333133363731343231333032303a2d3536303334353433303237373433363230353331373a343335363932343939363635313936373365656c31393a3238393130393536363231363533323137383831393a3833393139343732373835343036353139313932303a2d3835373031313136353138303635383632323431393a3535373737343534313930383430303236373931393a3730383035363431373530343633313133363832303a2d3435333635363337353934373739373039303731393a2d36313637333935343735383338363035343431393a333532363733333831313137333238373735316565";
    private static final byte version = 1;

    @Test
    public void createTransaction(){
        TxData txData = new TxData(ByteUtil.toByte(txdata));
        Transaction tx = new Transaction(version,chainid,150000000,700,sender,0,txData,signature);
        log.debug(ByteUtil.toHexString(tx.getEncoded()));
        log.debug("size of tx: "+transaction.length()/2);
        log.debug("hash is: "+ ByteUtil.toHexString(tx.getTxID()));
        byte[] hashbyte = RLP.encodeElement(ByteUtil.toByte("f8e201b4544155636f696e233330302333393338"));
        Assert.assertArrayEquals(hashbyte,ByteUtil.toByte("94f8e201b4544155636f696e233330302333393338"));
        log.debug("encode size is: "+hashbyte.length);
    }

    @Test
    public void decodeTransaction(){
        Transaction tx = new Transaction(ByteUtil.toByte(transaction));
        log.debug("fee: "+tx.getTxFee());
        String chainid = new String(tx.getChainID());
        log.debug("chainid: "+ chainid);
        log.debug("amount: "+tx.getTxData().getAmount());
        log.debug("sender: "+ByteUtil.toHexString(tx.getSenderPubkey()));
        log.debug("receiver: "+ByteUtil.toHexString(tx.getTxData().getReceiver()));
    }
}
