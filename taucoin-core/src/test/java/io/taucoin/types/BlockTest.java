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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Pair;

import java.math.BigInteger;

import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteUtil;

public class BlockTest {
    private static final Logger log = LoggerFactory.getLogger("blockTest");
    private static final byte version = 1;
    private static final byte[] chainid = "TAUcoin#300#3938383036366633393364383365393338373434".getBytes();
    private static final String transaction = "f8e201b4544155636f696e233330302333393338333833303336333636363333333933333634333833333635333933333338333733343334880000000008f0d18084ffffffc4a0c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c880000000000000000f104afeea0c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c88000000003b9aca0083627579b840281f3c2fe309683c74762f965f38bd8f8910d8dbeca1da904d6821e8101075776243379a4efdfdc8c10ae34be767a825f770e6a62b5430c030f179b74057e747";
    private static final byte[] pblockhash = ByteUtil.toByte("c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c");
    private static final byte[] imblockhash = ByteUtil.toByte("b7516c32e5ff8144bb919a141ce051de00b09b01d79e2217ba94dc567242e6ce");
    private static final BigInteger basetarget = new BigInteger("21D0369D036978",16);
    private static final BigInteger cummulativediff = BigInteger.ZERO;
    private static final byte[] generationSig = ByteUtil.toByte("c178f0713ef498e88def4156a9425e8469cdb0b7138a21e20d4be7e4836a8d66");
    private static final byte[] signature = ByteUtil.toByte("281f3c2fe309683c74762f965f38bd8f8910d8dbeca1da904d6821e8101075776243379a4efdfdc8c10ae34be767a825f770e6a62b5430c030f179b74057e747");
    private static final String bk = "f9021f01b4544155636f696e233330302333393338333833303336333636363333333933333634333833333635333933333338333733343334880000000008f0d180880000000000000001a0c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23ca0b7516c32e5ff8144bb919a141ce051de00b09b01d79e2217ba94dc567242e6ce8721d0369d03697880a0c178f0713ef498e88def4156a9425e8469cdb0b7138a21e20d4be7e4836a8d66f8e201b4544155636f696e233330302333393338333833303336333636363333333933333634333833333635333933333338333733343334880000000008f0d18084ffffffc4a0c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c880000000000000000f104afeea0c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c88000000003b9aca0083627579b840281f3c2fe309683c74762f965f38bd8f8910d8dbeca1da904d6821e8101075776243379a4efdfdc8c10ae34be767a825f770e6a62b5430c030f179b74057e7478800000000000186a08800000000000186a0880000000000013880880000000000000002b840281f3c2fe309683c74762f965f38bd8f8910d8dbeca1da904d6821e8101075776243379a4efdfdc8c10ae34be767a825f770e6a62b5430c030f179b74057e747a06569a52dd12c3f03ee6dc413ab33795a6597f1671659137bc8c9624abbb05c4a";
    private static final byte[] pubkey = ByteUtil.toByte("6569a52dd12c3f03ee6dc413ab33795a6597f1671659137bc8c9624abbb05c4a");
    private static final String prikey = "88885b264ab7c302e5a4475f8f2bc0db8da4cbac7d9a5d1b6ed8de2d0e43d45766357cbb9eb344b0a785abc05e26f50443b4b997f9d5a52faba1535d358a372c";
    private static final String bks = "f9013b01b4544155636f696e233330302333393338333833303336333636363333333933333634333833333635333933333338333733343334880000000008f0d180880000000000000001a0c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23ca0b7516c32e5ff8144bb919a141ce051de00b09b01d79e2217ba94dc567242e6ce8721d0369d03697880a0c178f0713ef498e88def4156a9425e8469cdb0b7138a21e20d4be7e4836a8d668800000000000186a08800000000000186a0880000000000013880880000000000000002b840f7ebadc98ef9adad8ef61ec5c1bad145a2ccd81e4eb499e1662799313c059d6fd0b086f1c9c443b99fce0b08bf7a4473e57b3c022cfc3baf1e1255af8d1c270ca06569a52dd12c3f03ee6dc413ab33795a6597f1671659137bc8c9624abbb05c4a";
    @Test
    public void createBlock(){
        Transaction tx = new Transaction(ByteUtil.toByte(transaction));
            Block block = new Block(version, chainid, 150000000, 1, pblockhash, imblockhash,
                    basetarget, cummulativediff, generationSig, tx, 100000, 100000,
                    80000, 2, signature, pubkey);
            String str = ByteUtil.toHexString(block.getEncoded());
            log.debug(str);
            log.debug("block size is: " + ByteUtil.toByte(str).length + " bytes");
            boolean ret1 = block.isBlockParamValidate();
            log.debug("param validate ?: "+ret1);
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
        boolean ret1 = block.isBlockParamValidate();
        log.debug("param validate ?: "+ret1);
    }
    @Test
    public void unSigBlock(){
        Transaction tx = new Transaction(ByteUtil.toByte(transaction));
            Block block = new Block(version, chainid, 150000000, 1, pblockhash, imblockhash,
                    basetarget, cummulativediff, generationSig, null, 100000, 100000,
                    80000, 2, pubkey);
        boolean ret2 = block.isBlockParamValidate();
        log.debug("===>param validate ?: "+ret2);
            String str = ByteUtil.toHexString(block.getSigEncoded());
            log.debug(str);
            log.debug("block size is: " + ByteUtil.toByte(str).length + " bytes");
            log.debug("block sig msg: " + ByteUtil.toHexString(block.getBlockSigMsg()));
            byte[] blocksig = Ed25519.sign(block.getBlockSigMsg(), pubkey, ByteUtil.toByte(prikey));
            block.setSignature(blocksig);
            log.debug("signature is: " + ByteUtil.toHexString(blocksig));
            log.debug("signature size is: " + blocksig.length);
            String str1 = ByteUtil.toHexString(block.getEncoded());
            log.debug("block with sig is: " + str1);
            boolean ret1 = block.isBlockParamValidate();
            log.debug("param validate ?: "+ret1);
    }

    @Test
    public void verifyBlockSig(){
        byte[] signature = ByteUtil.toByte("f7ebadc98ef9adad8ef61ec5c1bad145a2ccd81e4eb499e1662799313c059d6fd0b086f1c9c443b99fce0b08bf7a4473e57b3c022cfc3baf1e1255af8d1c270c");
        byte[] sigmsg = ByteUtil.toByte("74fcd625ffeb9c8fddae19a1572d01cc1756921a823aaad40c229d9fb8bd7819");
        if(Ed25519.verify(signature,sigmsg,pubkey)){
            log.debug("verify passed....");
        }
        Block bk = new Block(ByteUtil.toByte(bks));
        if (bk.verifyBlockSig()){
            log.debug("verify passed too....");
        }
        boolean ret1 = bk.isBlockParamValidate();
        log.debug("param validate ?: "+ret1);
    }

    @Test
    public void print(){
       log.debug("max basetarget: "+ ChainParam.MaxBaseTarget.toString());
       log.debug("max balance: "+ Long.MAX_VALUE);
    }
}
