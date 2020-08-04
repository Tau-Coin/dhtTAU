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

import com.frostwire.jlibtorrent.Ed25519;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

import io.taucoin.param.ChainParam;
import io.taucoin.utypes.Block;
import io.taucoin.utypes.Transaction;
import io.taucoin.util.ByteUtil;

public class BlockTest {
    private static final Logger log = LoggerFactory.getLogger("blockTest");
    private static final byte version = 1;
    private static final byte[] chainid = "TAUcoin#300#3938383036366633393364383365393338373434".getBytes();
    private static final String transaction = "6c313a3135323a544155636f696e233330302333393338333833303336333636363333333933333634333833333635333933333338333733343334393a313530303030303030333a3730306c32303a2d3432313237303335393733333535373931383032303a2d3538353234323932373236333232333535373632303a2d3530383833343339373636383531343534303931393a3638333330383536313934383230343730333665313a306c313a34313a306c32303a2d3132353137383335303231383738363138313131393a3834393036303333373134393331363339343431393a3736323235343632303233333338353734353431393a2d39353439343033333133363731343231333032303a2d3536303334353433303237373433363230353331373a343335363932343939363635313936373365656c31393a3238393130393536363231363533323137383831393a3833393139343732373835343036353139313932303a2d3835373031313136353138303635383632323431393a3535373737343534313930383430303236373931393a3730383035363431373530343633313133363832303a2d3435333635363337353934373739373039303731393a2d36313637333935343735383338363035343431393a333532363733333831313137333238373735316565";
    private static final byte[] pblockhash = ByteUtil.toByte("c5897865e8cd75d4aec7fe9583a869c8b962921c");
    private static final byte[] imblockhash = ByteUtil.toByte("b7516c32e5ff8144bb919a141ce051de00b09b01");
    private static final BigInteger basetarget = new BigInteger("21D0369D036978",16);
    private static final BigInteger cummulativediff = BigInteger.ZERO;
    private static final byte[] generationSig = ByteUtil.toByte("c178f0713ef498e88def4156a9425e8469cdb0b7138a21e20d4be7e4836a8d66");
    private static final byte[] signature = ByteUtil.toByte("281f3c2fe309683c74762f965f38bd8f8910d8dbeca1da904d6821e8101075776243379a4efdfdc8c10ae34be767a825f770e6a62b5430c030f179b74057e747");
    private static final String bk = "6c313a3135323a544155636f696e233330302333393338333833303336333636363333333933333634333833333635333933333338333733343334393a313530303030303030313a316c32303a2d3432313237303335393733333535373931383032303a2d3538353234323932373236333232333535373631303a33313130323434383932656c32303a2d3532333732383539323537393232383433343832303a2d34393330393930373035383139343935393730383a31313537343031376531363a39353137363037323132353039353630313a306c32303a2d3435303535383730353830303539393332343032303a2d3832313932373839353434363531373338383431393a3736323339343430343435363134343132353031383a393538313134333134333839373230343232656c313a3135323a544155636f696e233330302333393338333833303336333636363333333933333634333833333635333933333338333733343334393a313530303030303030333a3730306c32303a2d3432313237303335393733333535373931383032303a2d3538353234323932373236333232333535373632303a2d3530383833343339373636383531343534303931393a3638333330383536313934383230343730333665313a306c313a34313a306c32303a2d3132353137383335303231383738363138313131393a3834393036303333373134393331363339343431393a3736323235343632303233333338353734353431393a2d39353439343033333133363731343231333032303a2d3536303334353433303237373433363230353331373a343335363932343939363635313936373365656c31393a3238393130393536363231363533323137383831393a3833393139343732373835343036353139313932303a2d3835373031313136353138303635383632323431393a3535373737343534313930383430303236373931393a3730383035363431373530343633313133363832303a2d3435333635363337353934373739373039303731393a2d36313637333935343735383338363035343431393a333532363733333831313137333238373735316565363a313030303030363a313030303030353a3830303030313a326c31393a3238393130393536363231363533323137383831393a3833393139343732373835343036353139313932303a2d3835373031313136353138303635383632323431393a3535373737343534313930383430303236373931393a3730383035363431373530343633313133363832303a2d3435333635363337353934373739373039303731393a2d36313637333935343735383338363035343431393a33353236373333383131313733323837373531656c31393a3733303735353334383635383637383936333532303a2d3132363631343033333134363535343132383631393a3733323035383531343433373238393235333932303a2d333937383534303732323638393132333235346565";
    private static final byte[] pubkey = ByteUtil.toByte("ae20f5f96e89b8945a1194749456f74357864d5902ee8a5c19c3e75d0cef91ea");
    private static final String prikey = "982b0f5423295889ca0f410064b11d29659e7408ca598e104766632056343344ecd4ea4328d0b318e0e94077dba7b78778216ce290eff255459915297e81173d";
    private static final String bks = "6c313a3135323a544155636f696e233330302333393338333833303336333636363333333933333634333833333635333933333338333733343334393a313530303030303030313a316c32303a2d3432313237303335393733333535373931383032303a2d3538353234323932373236333232333535373631303a33313130323434383932656c32303a2d3532333732383539323537393232383433343832303a2d34393330393930373035383139343935393730383a31313537343031376531363a39353137363037323132353039353630313a306c32303a2d3435303535383730353830303539393332343032303a2d3832313932373839353434363531373338383431393a3736323339343430343435363134343132353031383a393538313134333134333839373230343232656c313a3135323a544155636f696e233330302333393338333833303336333636363333333933333634333833333635333933333338333733343334393a313530303030303030333a3730306c32303a2d3432313237303335393733333535373931383032303a2d3538353234323932373236333232333535373632303a2d3530383833343339373636383531343534303931393a3638333330383536313934383230343730333665313a306c313a34313a306c32303a2d3132353137383335303231383738363138313131393a3834393036303333373134393331363339343431393a3736323235343632303233333338353734353431393a2d39353439343033333133363731343231333032303a2d3536303334353433303237373433363230353331373a343335363932343939363635313936373365656c31393a3238393130393536363231363533323137383831393a3833393139343732373835343036353139313932303a2d3835373031313136353138303635383632323431393a3535373737343534313930383430303236373931393a3730383035363431373530343633313133363832303a2d3435333635363337353934373739373039303731393a2d36313637333935343735383338363035343431393a333532363733333831313137333238373735316565363a313030303030363a313030303030353a3830303030313a326c32303a2d3138373334363835353333323537373035373431393a3738333035333439303233383133303938393531393a2d35363439343830313937353731373435303532303a2d3138313638383535383239313939373431303231393a3431343933303038353635373037373335383732303a2d3531313639313234323235323839383430313131383a32363530393831363930393930333334363631393a32393736363132313235303239323633363232656c32303a2d3538393934343530363032303531363834393231393a3634393031333137363634343334333939333931393a3633303638313333373238373535373338353231393a313835363538313835383234313737383135346565";
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
                    basetarget, cummulativediff, generationSig, tx, 100000, 100000,
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
        byte[] signature = ByteUtil.toByte("e6001a46dd2708b26caba60179bc6fc7f828e6c4edd03117e6c920304886c32a3995472526200053b8fd134324605c3503add16157c11b7a294f0cf4fa7d6906");
        byte[] sigmsg = ByteUtil.toByte("25c15cf38391e90f6165aabb9a6c1c2293a71377f7bc187a8f849c436491f1b8");
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
       log.debug(System.getProperty("java.library.path"));
       long var = 0x7fffffffffffffffL;
       log.debug("max basetarget: "+ ChainParam.MaxBaseTarget.toString());

       log.debug("equal ? : "+ (Long.MAX_VALUE == var));
       log.debug("max balance: "+ Long.MAX_VALUE);
       log.debug("bytes are: "+ BigInteger.valueOf(Long.MAX_VALUE).longValue());
       log.debug("-1 bytes are: "+ByteUtil.toHexString(ByteUtil.longToBytes(-1)));
    }
}
