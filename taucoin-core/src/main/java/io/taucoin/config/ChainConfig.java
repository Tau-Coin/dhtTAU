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
package io.taucoin.config;

import io.taucoin.genesis.GenesisItem;
import io.taucoin.param.ChainParam;
import io.taucoin.types.GenesisTx;
import io.taucoin.types.Transaction;
import io.taucoin.util.ByteArrayWrapper;

import com.frostwire.jlibtorrent.swig.byte_vector;
import com.frostwire.jlibtorrent.swig.sha1_hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.HashMap;

import static io.taucoin.types.TypesConfig.TxType;

/**
 * configuration for new chain.
 */
public class ChainConfig {

    private static final Logger logger = LoggerFactory.getLogger("ChainConfig");

    private long version;
    private String communityName;
    private int blockTimeInterval = 600; // seconds
    private byte[] genesisMinerPubkey;
    private long genesisTimeStamp;
    private long blockNum = 0;
    private BigInteger baseTarget = new BigInteger("21D0369D036978", 16);
    private BigInteger cummulativeDifficulty = BigInteger.ZERO;
    private byte[] generationSignature;
    private Transaction msg;
    private byte[] signature;

    private byte[] chainID;

    /**
     * construct chain config without signature.
     * @param version
     * @param communityName
     * @param blockTimeInterval
     * @param genesisMinerPubkey
     * @param generationSignature
     */
    public ChainConfig(long version, String communityName, int blockTimeInterval,
            byte[] genesisMinerPubkey, long genesisTimeStamp, byte[] generationSignature,
            HashMap<ByteArrayWrapper, GenesisItem> genesisMsg) {

        this.version = version;
        this.communityName = communityName;
        this.blockTimeInterval = blockTimeInterval;
        this.genesisMinerPubkey = genesisMinerPubkey;
        this.genesisTimeStamp = System.currentTimeMillis() / 1000;

        // TAU blockchain uses the specified timestamp.
        if (ChainParam.TauGenesisMinerPubkey.equals(new String(genesisMinerPubkey))) {
            this.genesisTimeStamp = ChainParam.TauGenesisTimeStamp;
        }

        this.generationSignature = generationSignature;

        this.chainID = getChainid();

        // TODO: create genesis transaction
        this.msg = new GenesisTx(this.version, this.chainID, this.genesisTimeStamp,
                0, 0L, this.genesisMinerPubkey, 0L, genesisMsg);
    }

    public ChainConfig(String communityName, HashMap<ByteArrayWrapper, GenesisItem> genesisMsg) {
        // TODO:
    }

    /**
     * fingerprint of a chain miner pubkey and timestamp.
     * to composite chainID.
     * @return
     */
    private String fingerPrintOfPubkeyAndTime(){
        byte_vector bv = new byte_vector();
        String str = new String(genesisMinerPubkey) + this.genesisTimeStamp;
        for (byte ch:str.getBytes()) {
           bv.push_back(ch);
        }
        sha1_hash hash= new sha1_hash(bv);
        return hash.to_hex();
    }

    /**
     * TAU chain basic config on every node.
     * @return
     */
    public static ChainConfig NewTauChainConfig() {

        ChainConfig cf = new ChainConfig((long)ChainParam.DefaultGenesisVersion,
                ChainParam.TauCommunityName,
                ChainParam.DefaultBlockTimeInterval,
                ChainParam.TauGenesisMinerPubkey.getBytes(),
                System.currentTimeMillis() / 1000,
                ChainParam.TauGenerationSignature.getBytes(), null);

        return cf;
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
     * get block time interval.
     * @return
     */
    public int getBlockTimeInterval() {
        return blockTimeInterval;
    }

    /**
     * get genesis miner pubkey.
     * @return
     */
    public byte[] getGenesisMinerPubkey() {
        return genesisMinerPubkey;
    }

    /**
     * get genesis timestamp.
     * @return
     */
    public long getGenesisTimeStamp() {
        return genesisTimeStamp;
    }

    /**
     * get block num.
     * @return
     */
    public long getBlockNum() {
        return blockNum;
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
    public byte[] getChainid() {
        String id = this.communityName + ChainParam.ChainidDelimeter + fingerPrintOfPubkeyAndTime();
        return id.getBytes();
    }

    /**
     * get genesis tx message.
     * @return
     */
    public Transaction getMsg() {
        return msg;
    }
}
