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
import org.spongycastle.util.encoders.Hex;

public class BlockTest {
    private static final long version = 1;
    private static final byte[] verticalHash = ByteUtil.toByte("c5897865e8cd75d4aec7fe9583a869c8b962921c");
    private static final byte[] horizontalHash = ByteUtil.toByte("c5897865e8cd75d4aec7fe9583a869c8b962921c");
    private static final byte[] imblockhash = ByteUtil.toByte("b7516c32e5ff8144bb919a141ce051de00b09b01");

    private static final BigInteger basetarget = new BigInteger("21D0369D036978",16);
    private static final BigInteger cummulativediff = BigInteger.ZERO;
    private static final byte[] generationSig = ByteUtil.toByte("178f0713ef498e88def4156a9425e8469cdb0bf1");

    private static byte[] encodedBytes = null;
    @Test
    public void createBlock() {
        byte[] seed = Ed25519.createSeed();
        Pair<byte[], byte[]> keys = Ed25519.createKeypair(seed);
        byte[] pubkey = keys.first;
        BigInteger mBalance = BigInteger.valueOf(100000000000000L);
        BigInteger sBalance = BigInteger.valueOf(300300000000000L);
        BigInteger rBalance = BigInteger.valueOf(800000000000000L);
        BigInteger sNonce = BigInteger.valueOf(900L);

        Block block = new Block(version, 1597062314L, 1,
                    verticalHash, horizontalHash, imblockhash,
                    basetarget, cummulativediff, generationSig,
                    mBalance, sBalance, rBalance, sNonce,
                    pubkey);
        System.out.println("Version: " + block.getVersion());
        System.out.println("Timestamp: " + block.getTimeStamp());

        System.out.println("Vertical block hash get: " + ByteUtil.toHexString(block.getVerticalHash()));
        System.out.println("Horizontal block hash get: " + ByteUtil.toHexString(block.getHorizontalHash()));
        System.out.println("Immutable block hash get: " + ByteUtil.toHexString(block.getImmutableBlockHash()));

        System.out.println("Basetarget: " + block.getBaseTarget());
        System.out.println("Cummulativediff: " + block.getCumulativeDifficulty());
        System.out.println("Genersation signature: " + ByteUtil.toHexString(block.getGenerationSignature()));

        System.out.println("Miner Balance: " + block.getMinerBalance());
        System.out.println("Sender Balance: " + block.getSenderBalance());
        System.out.println("Receiver Balance: " + block.getReceiverBalance());
        System.out.println("Sender nonce: " + block.getSenderNonce());

        System.out.println("Pubkey: " + ByteUtil.toHexString(block.getMinerPubkey()));
        System.out.println("Signature: " + ByteUtil.toHexString(block.signBlock(keys.second)));

        byte[] signBytes = block.signBlock(keys.second);
        block.setSignature(signBytes);
        encodedBytes = block.getEncoded();
        System.out.println(Hex.toHexString(encodedBytes));
        boolean ret = block.isBlockParamValidate();
        System.out.println("param validate ?: " + ret);
        System.out.println("0 Block hash get: " + ByteUtil.toHexString(block.getBlockHash()));

        System.out.println("============================================================");


        encodedBytes = block.getEncoded();
        Block dblock = new Block(encodedBytes);
        System.out.println("Block : " + encodedBytes.length);
        System.out.println("1 Block hash get: " + ByteUtil.toHexString(dblock.getBlockHash()));

        System.out.println("Version: " + dblock.getVersion());
        System.out.println("Timestamp: " + dblock.getTimeStamp());

        System.out.println("Vertical block hash get: " + ByteUtil.toHexString(dblock.getVerticalHash()));
        System.out.println("Horizontal hash get: " + ByteUtil.toHexString(dblock.getHorizontalHash()));
        System.out.println("Immutable block hash get: " + ByteUtil.toHexString(dblock.getImmutableBlockHash()));

        System.out.println("Basetarget: " + dblock.getBaseTarget());
        System.out.println("Cummulativediff: " + dblock.getCumulativeDifficulty());
        System.out.println("Genersation signature: " + ByteUtil.toHexString(dblock.getGenerationSignature()));

        System.out.println("Miner Balance: " + dblock.getMinerBalance());
        System.out.println("Sender Balance: " + dblock.getSenderBalance());
        System.out.println("Receiver Balance: " + dblock.getReceiverBalance());
        System.out.println("Sender nonce: " + dblock.getSenderNonce());

        System.out.println("Miner pubkey: " + ByteUtil.toHexString(dblock.getMinerPubkey()));
        System.out.println("Signature: " + ByteUtil.toHexString(dblock.getSignature()));

        System.out.println("Signature bool: " + dblock.verifyBlockSig());

        boolean dret = dblock.isBlockParamValidate();
        System.out.println("param validate ?: " + dret);
    }

}
