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
package io.taucoin.utypes;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.taucoin.utypes.MsgType;
import io.taucoin.utypes.Note;
import io.taucoin.utypes.TxData;
import io.taucoin.utypes.WireTransaction;
import io.taucoin.util.ByteUtil;


public class TxDataTest {
/*    private static final Logger log = LoggerFactory.getLogger("txDataTest");
    private static final String reference = "c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c";
    private static final byte[] chainid = "TAUcoin#300#3938383036366633393364383365393338373434".getBytes();
    private static final byte[] minerpubkey = ByteUtil.toByte("6569a52dd12c3f03ee6dc413ab33795a6597f1671659137bc8c9624abbb05c4a");
    private static final String description = "tau community";
    private static final String txdata = "6c313a34313a306c32303a2d3132353137383335303231383738363138313131393a3834393036303333373134393331363339343431393a3736323235343632303233333338353734353431393a2d39353439343033333133363731343231333032303a2d3536303334353433303237373433363230353331373a34333536393234393936363531393637336565";
    private static final String bootnodetx = "f85803b855f853b4544155636f696e2333303023333933383338333033363336363633333339333336343338333336353339333333383337333433348a746175636f696e2e696f923139322e3136382e322e3131383a33303333";
    @Test
    public void createTxData(){
        Note note = new Note("hello world....");
        TxData txData0 = new TxData(MsgType.RegularForum,note.getTxCode());
        String str0 = ByteUtil.toHexString(txData0.getEncoded());
        log.debug(str0);

//        Comment comment = new Comment(ByteUtil.toByte(reference),"hello copy!");
//        TxData txData1 = new TxData(MsgType.ForumComment,comment.getEncode());
//        String str1 = ByteUtil.toHexString(txData1.getEncoded());
//        log.debug(str1);

//        CommunityAnnouncement cua = new CommunityAnnouncement(chainid,minerpubkey,description);
//        TxData txData2 = new TxData(MsgType.CommunityAnnouncement,cua.getEncode());
//        String str2 = ByteUtil.toHexString(txData2.getEncoded());
//        log.debug(str2);

//        String[] network = new String[]{"taucoin.io","192.168.2.118:3033"};
//        DHTbootstrapNodeAnnouncement dbn = new DHTbootstrapNodeAnnouncement(chainid,network);
//        TxData txData3 = new TxData(MsgType.DHTBootStrapNodeAnnouncement,dbn.getEncode());
//        String str3 = ByteUtil.toHexString(txData3.getEncoded());
//        log.debug("bootnode: "+str3);

        WireTransaction wtx = new WireTransaction(ByteUtil.toByte("c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c"),1000000000,"buy");
        System.out.println("byte will be convert into long array: "+ByteUtil.toHexString(wtx.getEncode()));
        TxData txData4 = new TxData(MsgType.Wiring,wtx.getEncode());
        String str4 = ByteUtil.toHexString(txData4.getEncoded());
        log.debug(str4);

//        IdentityAnnouncement ida = new IdentityAnnouncement("hello","taugenesis");
//        TxData txData5 = new TxData(MsgType.IdentityAnnouncement,ida.getEncode());
//        String str5 = ByteUtil.toHexString(txData5.getEncoded());
//        log.debug(str5);
    }

    @Test
    public void decodeTxData(){
        TxData txData = new TxData(ByteUtil.toByte(txdata));
        log.debug("receiverPubkey 1st: "+ByteUtil.toHexString(txData.getReceiver()));
        log.debug("receiverPubkey 2nd: "+ByteUtil.toHexString(txData.getReceiver()));
        log.debug("Amount: "+txData.getAmount());
        log.debug(txData.getMsgType().toString());

//        TxData bootTX = new TxData(ByteUtil.toByte(bootnodetx));
//        log.debug(bootTX.getMsgType().toString());

    }*/

}
