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
package io.taucoin.genesis;

import io.taucoin.account.AccountManager;
import io.taucoin.param.ChainParam;
import io.taucoin.types.Block;
import io.taucoin.types.GenesisTx;
import io.taucoin.types.Transaction;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.HashUtil;

import com.frostwire.jlibtorrent.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Configuration for new blockchain genesis block.
 */
public class GenesisConfig {

    private static final Logger logger = LoggerFactory.getLogger("GenesisConfig");

    public static final BigInteger DefaultBaseTarget = new BigInteger("21D0369D036978", 16);
    public static final BigInteger DefaultCummulativeDifficulty
            = BigInteger.ZERO;

    public static final BigInteger DefaultGenesisTxFee = BigInteger.ZERO;

    // genesis block fields
    private long version;
    private long timeStamp;
    private byte[] horizontalHash;
    private BigInteger baseTarget;
    private BigInteger cummulativeDifficulty;
    private byte[] generationSignature;
    private byte[] pubkey;
    private byte[] signature;

    protected Block genesisBlock;

    // genesis transaction hash
    private String communityName;
    protected GenesisTx genesisTx;

    /**
     * GenesisConfig constructor.
     *
     * This constructor is only used for TAU blockchain.
     * Because TAU blockchain genesis block signature is prebuilt.
     */
    public GenesisConfig(long version, long timeStamp,
            BigInteger baseTarget, BigInteger cummulativeDifficulty,
            byte[] pubkey, byte[] signature, GenesisTx genesisTx) {

        this.version = version;
        this.timeStamp = timeStamp;
        this.baseTarget = baseTarget;
        this.cummulativeDifficulty = cummulativeDifficulty;
        this.pubkey = pubkey;

        // According to POT consensus,
        // generationSignature = hash(previous block generationSignature + pubkey)
        // here previous block generationSignature is null.
        this.generationSignature = HashUtil.sha1hash(this.pubkey);
        this.signature = signature;
        this.genesisTx = genesisTx;
        this.horizontalHash = HashUtil.sha1hash(this.genesisTx.getTxID());
        this.communityName = TauGenesisTransaction.CommunityName;

        // construct genesis block
        this.genesisBlock = new Block(this);
    }

    /**
     * GenesisConfig constructor.
     *
     * This constructor is only used for creating new community. 
     */
    public GenesisConfig(String communityName,
            ArrayList<GenesisItem> genesisItems) {

        // First of all, get account state from TAU blockchain.
        Pair<byte[], byte[]> senderKey = AccountManager.getInstance().getKeyPair();

        this.communityName = communityName;
        long timeStamp = System.currentTimeMillis() / 1000;
        byte[] chainID = chainID(communityName, senderKey.first, timeStamp);

        // create genesis transaction
        this.genesisTx = new GenesisTx(1L, chainID, timeStamp, DefaultGenesisTxFee,
                senderKey.first, BigInteger.ZERO, genesisItems);
        this.genesisTx.signTransactionWithPriKey(senderKey.second);

        this.version = 1L;
        this.timeStamp = System.currentTimeMillis() / 1000;

        this.horizontalHash = HashUtil.sha1hash(this.genesisTx.getTxID());

        this.baseTarget = DefaultBaseTarget;
        this.cummulativeDifficulty = DefaultCummulativeDifficulty;

        this.pubkey = new byte[senderKey.first.length];
        System.arraycopy(senderKey.first, 0, this.pubkey, 0, senderKey.first.length);

        this.generationSignature = HashUtil.sha1hash(this.pubkey);
        this.signature = null;

        // construct genesis block
        this.genesisBlock = new Block(this);
        this.genesisBlock.signBlock(senderKey.second);
        this.signature = this.genesisBlock.getSignature();
    }

    /**
     * Get genesis block.
     */
    public Block getBlock() {
        return genesisBlock;
    }

    /**
     * Get genesis transaction
     */
    public Transaction getTransaction() {
        return genesisTx;
    }

    /**
     * get genesis config version.
     * @return
     */
    public long getVersion() {
        return version;
    }

    /**
     * get community name.
     * @return
     */
    public String getCommunityName() {
        return communityName;
    }

    /**
     * get genesis miner pubkey.
     * @return
     */
    public byte[] getPubkey() {
        return pubkey;
    }

    /**
     * get genesis timestamp.
     * @return
     */
    public long getTimeStamp() {
        return timeStamp;
    }

    /**
     * get block num.
     * @return
     */
    public long getBlockNumber() {
        return 0L;
    }

    /**
     * get genesis horizontalHash.
     * @return
     */
    public byte[] getHorizontalHash() {
        return horizontalHash;
    }

    /**
     * get genesis basetarget.
     * @return
     */
    public BigInteger getBaseTarget() {
        return baseTarget;
    }

    /**
     * get cummulative difficulty.
     * @return
     */
    public BigInteger getCummulativeDifficulty() {
        return cummulativeDifficulty;
    }

    /**
     * get generation signature.
     * @return
     */
    public byte[] getGenerationSignature() {
        return generationSignature;
    }

    /**
     * get signature.
     * @return
     */
    public byte[] getSignature() {
        return signature;
    }

    /**
     * get Chainid.
     * @return
     */
    public byte[] getChainID() {
        return genesisTx.getChainID();
    }

    /**
     * generate chain ID.
     */
    public static byte[] chainID(String communityName, byte[] pubkey, long time) {
        byte[] timeBytes = ByteUtil.longToBytes(time);
        byte[] hashData = new byte[pubkey.length + timeBytes.length];

        System.arraycopy(pubkey, 0, hashData, 0, pubkey.length);
        System.arraycopy(timeBytes, 0, hashData, pubkey.length, timeBytes.length);
        byte[] hash = HashUtil.sha1hash(hashData);

        String idStr = communityName + ChainParam.ChainidDelimeter + Hex.toHexString(hash);
        return idStr.getBytes(UTF_8);
    }
}
