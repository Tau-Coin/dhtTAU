/*
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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.taucoin.util.ByteUtil;


public class TxDataTest {
    private static final Logger log = LoggerFactory.getLogger("txDataTest");
    private static final String announcement = "share your favourite music";
    private static final String attachment = "http://www.kugou.com/song/6nnyabb.html?frombaidu?frombaidu#hash=B06A440B4C21E29B34AC8038E308E02F&album_id=0";
    private static final String txdata = "f889809a736861726520796f7572206661766f7572697465206d75736963b86b687474703a2f2f7777772e6b75676f752e636f6d2f736f6e672f366e6e796162622e68746d6c3f66726f6d62616964753f66726f6d626169647523686173683d423036413434304234433231453239423334414338303338453330384530324626616c62756d5f69643d30";
    @Test
    public void createTxData(){
        TxData txData = new TxData(MsgType.TorrentPublish,announcement,attachment);
        String str = ByteUtil.toHexString(txData.getEncoded());
        log.debug(str);
    }

    @Test
    public void decodeTxData(){
        TxData txData = new TxData(ByteUtil.toByte(txdata));
        log.debug(txData.getAnnoucement());
        log.debug(txData.getMsgType().toString());
    }
}
