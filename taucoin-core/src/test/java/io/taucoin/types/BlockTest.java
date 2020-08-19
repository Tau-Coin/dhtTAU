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

import io.taucoin.util.ByteUtil;

import java.math.BigInteger;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockTest {
    private static final Logger log = LoggerFactory.getLogger("blockTest");
    private static final long version = 1;
    private static final byte[] pblockhash = ByteUtil.toByte("c5897865e8cd75d4aec7fe9583a869c8b962921c");
    private static final byte[] imblockhash = ByteUtil.toByte("b7516c32e5ff8144bb919a141ce051de00b09b01");
    private static final BigInteger basetarget = new BigInteger("21D0369D036978",16);
    private static final BigInteger cummulativediff = BigInteger.ZERO;
    private static final byte[] generationSig = ByteUtil.toByte("178f0713ef498e88def4156a9425e8469cdb0bf1");
    private static final byte[] tx = ByteUtil.toByte("f7516c32e5ff8144bb919a141ce051de00b09b02");
    private byte[] encodedBytes = null;
    @Test
    public void createBlock() {
        byte[] seed = Ed25519.createSeed();
        Pair<byte[], byte[]> keys = Ed25519.createKeypair(seed);
        byte[] pubkey = keys.first;
        long mBalance = 100000000000000L;
        long sBalance = 300300000000000L;
        long rBalance = 800000000000000L;
        Block block = new Block(version, 1597062314, 1, pblockhash, imblockhash,
                    basetarget, cummulativediff, generationSig, tx, mBalance, sBalance,
                    rBalance, 1345, pubkey);
        System.out.println("Version: " + block.getVersion());
        System.out.println("Timestamp: " + block.getTimeStamp());
        System.out.println("Previous block hash get: " + ByteUtil.toHexString(block.getPreviousBlockHash()));
        System.out.println("Immutable block hash get: " + ByteUtil.toHexString(block.getImmutableBlockHash()));
        System.out.println("Basetarget: " + block.getBaseTarget());
        System.out.println("Cummulativediff: " + block.getCumulativeDifficulty());
        System.out.println("Genersation signature: " + ByteUtil.toHexString(block.getGenerationSignature()));
        System.out.println("Tx hash: " + ByteUtil.toHexString(block.getTxHash()));
        System.out.println("Miner Balance: " + block.getMinerBalance());
        System.out.println("Sender Balance: " + block.getSenderBalance());
        System.out.println("Receiver Balance: " + block.getReceiverBalance());
        System.out.println("Sender nonce: " + block.getSenderNonce());
        System.out.println("Pubkey: " + ByteUtil.toHexString(block.getMinerPubkey()));
        System.out.println("Signature: " + ByteUtil.toHexString(block.signBlock(keys.second)));
        byte[] signBytes = block.signBlock(keys.second);
        block.setSignature(signBytes);
        encodedBytes= block.getEncoded();
        String str = new String(encodedBytes);
        System.out.println(str.length());
        System.out.println(str);
        boolean ret1 = block.isBlockParamValidate();
        System.out.println("param validate ?: "+ret1);
    }

    @Test
    public void decodeBlock() {
        byte[] encodedBytes = "li1ei1597062314ei1el20:-421270359733557918020:-585242927263223557610:3110244892el20:-523728592579228434820:-49309907058194959708:11574017ei9517607212509560ei0el19:169758336674231463220:-238125475547592082610:2631601137el19:-62559990736489644420:-49309907058194959708:11574018ei100000000000000ei300300000000000ei800000000000000ei1345el20:-113538715526926854419:752745604858715599720:-499604743546076548819:576811449480826335920:-517574174554620079020:-288670048887600925220:-122451662842657576920:-6288616399488319989el18:55499626314701271520:-199054152087173668720:-213946753850913011519:7538648299498507822ee".getBytes();
        Block block = new Block(encodedBytes);
        System.out.println("Version: " + block.getVersion());
        System.out.println("Timestamp: " + block.getTimeStamp());
        System.out.println("Previous block hash get: " + ByteUtil.toHexString(block.getPreviousBlockHash()));
        System.out.println("Immutable block hash get: " + ByteUtil.toHexString(block.getImmutableBlockHash()));
        System.out.println("Block hash get: " + ByteUtil.toHexString(block.getBlockHash()));
        System.out.println("Basetarget: " + block.getBaseTarget());
        System.out.println("Cummulativediff: " + block.getCumulativeDifficulty());
        System.out.println("Genersation signature: " + ByteUtil.toHexString(block.getGenerationSignature()));
        System.out.println("Tx hash: " + ByteUtil.toHexString(block.getTxHash()));
        System.out.println("Miner Balance: " + block.getMinerBalance());
        System.out.println("Sender Balance: " + block.getSenderBalance());
        System.out.println("Receiver Balance: " + block.getReceiverBalance());
        System.out.println("Sender nonce: " + block.getSenderNonce());
        System.out.println("Signature bool: " + block.verifyBlockSig());

        boolean ret1 = block.isBlockParamValidate();
        System.out.println("param validate ?: "+ret1);
    }
}
