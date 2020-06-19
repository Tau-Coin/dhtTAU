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
import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Pair;

import java.math.BigInteger;

import io.taucoin.util.ByteUtil;

public class BlockTest {
    private static final Logger log = LoggerFactory.getLogger("blockTest");
    private static final byte version = 1;
    private static final byte[] chainid = "TAUcoin#300#3938383036366633393364383365393338373434".getBytes();
    private static final String transaction = "f9019f01b4544155636f696e233330302333393338333833303336333636363333333933333634333833333635333933333338333733343334880000000008f0d1801e84ffffffc4b84063353839373836356538636437356434616563376665393538336138363963386239363239323163633661656632626635656433666632616564306562323363880000000000000000f889809a736861726520796f7572206661766f7572697465206d75736963b86b687474703a2f2f7777772e6b75676f752e636f6d2f736f6e672f366e6e796162622e68746d6c3f66726f6d62616964753f66726f6d626169647523686173683d423036413434304234433231453239423334414338303338453330384530324626616c62756d5f69643d30b88236353238316633633266653330393638336337343736326639363566333862643866383931306438646265636131646139303464363832316538313031303735373736323433333739613465666466646338633130616533346265373637613832356637373065366136326235343330633033306631373962373430353765373437";
    private static final byte[] pblockhash = ByteUtil.toByte("c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c");
    private static final byte[] imblockhash = ByteUtil.toByte("b7516c32e5ff8144bb919a141ce051de00b09b01d79e2217ba94dc567242e6ce");
    private static final BigInteger basetarget = new BigInteger("21D0369D036978",16);
    private static final BigInteger cummulativediff = BigInteger.ZERO;
    private static final byte[] generationSig = ByteUtil.toByte("c178f0713ef498e88def4156a9425e8469cdb0b7138a21e20d4be7e4836a8d66");
    private static final byte[] signature = ByteUtil.toByte("281f3c2fe309683c74762f965f38bd8f8910d8dbeca1da904d6821e8101075776243379a4efdfdc8c10ae34be767a825f770e6a62b5430c030f179b74057e747");
    private static final String bk = "f902dd01b4544155636f696e233330302333393338333833303336333636363333333933333634333833333635333933333338333733343334880000000008f0d180880000000000000001a0c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23ca0b7516c32e5ff8144bb919a141ce051de00b09b01d79e2217ba94dc567242e6ce8721d0369d03697880a0c178f0713ef498e88def4156a9425e8469cdb0b7138a21e20d4be7e4836a8d66f9019f01b4544155636f696e233330302333393338333833303336333636363333333933333634333833333635333933333338333733343334880000000008f0d1801e84ffffffc4b84063353839373836356538636437356434616563376665393538336138363963386239363239323163633661656632626635656433666632616564306562323363880000000000000000f889809a736861726520796f7572206661766f7572697465206d75736963b86b687474703a2f2f7777772e6b75676f752e636f6d2f736f6e672f366e6e796162622e68746d6c3f66726f6d62616964753f66726f6d626169647523686173683d423036413434304234433231453239423334414338303338453330384530324626616c62756d5f69643d30b882363532383166336332666533303936383363373437363266393635663338626438663839313064386462656361316461393034643638323165383130313037353737363234333337396134656664666463386331306165333462653736376138323566373730653661363262353433306330333066313739623734303537653734378800000000000186a08800000000000186a0880000000000013880880000000000000002b840281f3c2fe309683c74762f965f38bd8f8910d8dbeca1da904d6821e8101075776243379a4efdfdc8c10ae34be767a825f770e6a62b5430c030f179b74057e747a06569a52dd12c3f03ee6dc413ab33795a6597f1671659137bc8c9624abbb05c4a";
    private static final byte[] pubkey = ByteUtil.toByte("6569a52dd12c3f03ee6dc413ab33795a6597f1671659137bc8c9624abbb05c4a");
    private static final String prikey = "88885b264ab7c302e5a4475f8f2bc0db8da4cbac7d9a5d1b6ed8de2d0e43d45766357cbb9eb344b0a785abc05e26f50443b4b997f9d5a52faba1535d358a372c";
    private static final String bks = "f902dd01b4544155636f696e233330302333393338333833303336333636363333333933333634333833333635333933333338333733343334880000000008f0d180880000000000000001a0c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23ca0b7516c32e5ff8144bb919a141ce051de00b09b01d79e2217ba94dc567242e6ce8721d0369d03697880a0c178f0713ef498e88def4156a9425e8469cdb0b7138a21e20d4be7e4836a8d66f9019f01b4544155636f696e233330302333393338333833303336333636363333333933333634333833333635333933333338333733343334880000000008f0d1801e84ffffffc4b84063353839373836356538636437356434616563376665393538336138363963386239363239323163633661656632626635656433666632616564306562323363880000000000000000f889809a736861726520796f7572206661766f7572697465206d75736963b86b687474703a2f2f7777772e6b75676f752e636f6d2f736f6e672f366e6e796162622e68746d6c3f66726f6d62616964753f66726f6d626169647523686173683d423036413434304234433231453239423334414338303338453330384530324626616c62756d5f69643d30b882363532383166336332666533303936383363373437363266393635663338626438663839313064386462656361316461393034643638323165383130313037353737363234333337396134656664666463386331306165333462653736376138323566373730653661363262353433306330333066313739623734303537653734378800000000000186a08800000000000186a0880000000000013880880000000000000002b840a38bbb0c0dd342c3ac687d7e29a221073a7d26216d0ca2093ba1e1f9aa37c82a51d6aa8ce184845c958ab0f3845ee6e3cec7be978ea91bbab771a953672f5f08a06569a52dd12c3f03ee6dc413ab33795a6597f1671659137bc8c9624abbb05c4a";
    @Test
    public void createBlock(){
        Transaction tx = new Transaction(ByteUtil.toByte(transaction));
        Block block = new Block(version,chainid,150000000,1,pblockhash,imblockhash,
                basetarget,cummulativediff,generationSig,tx,100000,100000,
                80000,2,signature,pubkey);
        String str = ByteUtil.toHexString(block.getEncoded());
        log.debug(str);
        log.debug("block size is: "+ ByteUtil.toByte(str).length + " bytes");
    }

    @Test
    public void decodeBlock(){
        Block block = new Block(ByteUtil.toByte(bk));
        log.debug("version: "+block.getVersion());
        String chainid = new String(block.getChainID());
        log.debug("chainid: "+chainid);
        log.debug("timestamp: "+block.getTimeStamp());
        log.debug("blocknum: "+block.getBlockNum());
        log.debug("phash: "+ByteUtil.toHexString(block.getPreviousBlockHash()));
        log.debug("imhash: "+ByteUtil.toHexString(block.getImmutableBlockHash()));
        log.debug("basetarget: "+block.getBaseTarget().toString());
        log.debug("cummulativediff: "+block.getCumulativeDifficulty().toString());
        log.debug("blockhash: "+ByteUtil.toHexString(block.getBlockHash()));
    }
    @Test
    public void unSigBlock(){
        Transaction tx = new Transaction(ByteUtil.toByte(transaction));
        Block block = new Block(version,chainid,150000000,1,pblockhash,imblockhash,
                basetarget,cummulativediff,generationSig,tx,100000,100000,
                80000,2,pubkey);
        String str = ByteUtil.toHexString(block.getSigEncoded());
        log.debug(str);
        log.debug("block size is: "+ ByteUtil.toByte(str).length + " bytes");
        log.debug("block sig msg: "+ByteUtil.toHexString(block.getBlockSigMsg()));
        byte[] blocksig = Ed25519.sign(block.getBlockSigMsg(),pubkey,ByteUtil.toByte(prikey));
        block.setSignature(blocksig);
        log.debug("signature is: "+ByteUtil.toHexString(blocksig));
        log.debug("signature size is: "+blocksig.length);
        String str1 = ByteUtil.toHexString(block.getEncoded());
        log.debug("block with sig is: "+str1);
    }

    @Test
    public void verifyBlockSig(){
        byte[] signature = ByteUtil.toByte("a38bbb0c0dd342c3ac687d7e29a221073a7d26216d0ca2093ba1e1f9aa37c82a51d6aa8ce184845c958ab0f3845ee6e3cec7be978ea91bbab771a953672f5f08");
        byte[] sigmsg = ByteUtil.toByte("f03ebfb61c7362ebfeca8d8c89396b5fc2ee64d4e188bdd41149ae1de3c94cde");
        if(Ed25519.verify(signature,sigmsg,pubkey)){
            log.debug("verify passed....");
        }
        Block bk = new Block(ByteUtil.toByte(bks));
        if (bk.verifyBlockSig()){
            log.debug("verify passed too....");
        }
    }
}
