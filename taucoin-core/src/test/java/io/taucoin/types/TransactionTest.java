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
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;

public class TransactionTest {
    private static final Logger log = LoggerFactory.getLogger("transactionTest");
    private static final String txdata = "f104afeea0c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c88000000003b9aca0083627579";
    private static final byte[] chainid = "TAUcoin#300#3938383036366633393364383365393338373434".getBytes();
    private static final byte[] sender = ByteUtil.toByte("c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c");
    private static final byte[] signature = ByteUtil.toByte("281f3c2fe309683c74762f965f38bd8f8910d8dbeca1da904d6821e8101075776243379a4efdfdc8c10ae34be767a825f770e6a62b5430c030f179b74057e747");
    private static final String transaction = "f8e201b4544155636f696e233330302333393338333833303336333636363333333933333634333833333635333933333338333733343334880000000008f0d18084ffffffc4a0c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c880000000000000000f104afeea0c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c88000000003b9aca0083627579b840281f3c2fe309683c74762f965f38bd8f8910d8dbeca1da904d6821e8101075776243379a4efdfdc8c10ae34be767a825f770e6a62b5430c030f179b74057e747";
    private static final byte version = 1;

    @Test
    public void createTransaction(){
        TxData txData = new TxData(ByteUtil.toByte(txdata));
        Transaction tx = new Transaction(version,chainid,150000000,-60,sender,0,txData,signature);
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
    }
}
