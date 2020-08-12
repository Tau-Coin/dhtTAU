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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

import io.taucoin.param.ChainParam;
import io.taucoin.utypes.TxData;
import io.taucoin.utypes.Block;
import io.taucoin.utypes.Transaction;
import io.taucoin.util.ByteUtil;

public class BlockTest {
    private static final Logger log = LoggerFactory.getLogger("blockTest");
    private static final long version = 1;
    private static final byte[] pblockhash = ByteUtil.toByte("c5897865e8cd75d4aec7fe9583a869c8b962921c");
    private static final byte[] imblockhash = ByteUtil.toByte("b7516c32e5ff8144bb919a141ce051de00b09b01");
    private static final BigInteger basetarget = new BigInteger("21D0369D036978",16);
    private static final BigInteger cummulativediff = BigInteger.ZERO;
    private static final byte[] generationSig = ByteUtil.toByte("c178f0713ef498e88def4156a9425e8469cdb0b7138a21e20d4be7e4836a8d66");
    private static final byte[] tx = ByteUtil.toByte("f7516c32e5ff8144bb919a141ce051de00b09b02");
    private static final byte[] signature = ByteUtil.toByte("281f3c2fe309683c74762f965f38bd8f8910d8dbeca1da904d6821e8101075776243379a4efdfdc8c10ae34be767a825f770e6a62b5430c030f179b74057e747");
    private static final byte[] pubkey = ByteUtil.toByte("ae20f5f96e89b8945a1194749456f74357864d5902ee8a5c19c3e75d0cef91ea");

    @Test
    public void createBlock(){
        Transaction tx = createTx();
        long mBalance = 100000000000000L;
        long sBalance = 300300000000000L;
        long rBalance = 800000000000000L;
        Block block = new Block(version, 1597062314, 1, pblockhash, imblockhash,
                    basetarget, cummulativediff, generationSig, tx, mBalance, sBalance,
                    rBalance, 1345, signature, pubkey);
        System.out.println(block.getVersion());
        byte[] bencoded= block.getEncodedBytes();
        String str = new String(bencoded);
        System.out.println(str);
        System.out.println(str.length());
        boolean ret1 = block.isBlockParamValidate();
        System.out.println("param validate ?: "+ret1);
    }

    @Test
    public void print(){
       log.debug(System.getProperty("java.library.path"));
       long var = 0x7fffffffffffffffL;
       log.debug("max basetarget: "+ ChainParam.MaxBaseTarget.toString());

       log.debug("equal ? : "+ (Long.MAX_VALUE == var));
       log.debug("max balance: "+ Long.MAX_VALUE);
       log.debug("bytes are: "+ BigInteger.valueOf(Long.MAX_VALUE).longValue());
       log.debug("-1 bytes are: "+ByteUtil.toHexString(ByteUtil.longToBytes(-1)));
    }
}
