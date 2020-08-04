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

import java.math.BigInteger;

import io.taucoin.param.ChainParam;
import io.taucoin.types.GenesisMsg;
import io.taucoin.types.MsgType;
import io.taucoin.types.Transaction;
import io.taucoin.types.TxData;
import io.taucoin.util.ByteUtil;

import com.frostwire.jlibtorrent.swig.byte_vector;
import com.frostwire.jlibtorrent.swig.sha1_hash;

/**
 * configure every new chain parameter.
 */
public class ChainConfig {
    private byte version;
    private String CommunityName;
    private int BlockTimeInterval=600;
    private String GenesisMinerPubkey="";
    private long GenesisTimeStamp;
    private int BlockNum=0;
    private BigInteger BaseTarget= new BigInteger("21D0369D036978",16);
    private BigInteger CummulativeDifficulty = BigInteger.ZERO;
    private String GenerationSignature;
    private Transaction Msg;
    private String Signature="";

    /**
     * construct chain config without signature.
     * @param version
     * @param communityName
     * @param blockTimeInterval
     * @param genesisMinerPubkey
     * @param generationSignature
     */
    private  ChainConfig(byte version, String communityName, int blockTimeInterval, String genesisMinerPubkey,
                         String generationSignature, GenesisMsg msg){
        this.version = version;
        this.CommunityName = communityName;
        this.BlockTimeInterval = blockTimeInterval;
        this.GenesisMinerPubkey = genesisMinerPubkey;
        this.GenesisTimeStamp = System.currentTimeMillis()/1000;
        this.GenerationSignature = generationSignature;

        TxData txData = new TxData(MsgType.GenesisMsg,msg.getEncoded());
        //construct genesis transaction without signature
        //this special tx will be signed simultaneously when genesis block is signed.
        this.Msg = new Transaction(version,communityName,blockTimeInterval,this.GenesisTimeStamp,genesisMinerPubkey,txData);
    }

    /**
     * fingerprint of a chain miner pubkey and timestamp.
     * to composite chainID.
     * @return
     */
    private String fingerPrintOfPubkeyAndTime(){
        byte_vector bv = new byte_vector();
        String str = this.GenesisMinerPubkey + this.GenesisTimeStamp;
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
    public static ChainConfig NewTauChainConfig(){
        GenesisMsg msg = new GenesisMsg(ByteUtil.toByte(ChainParam.TauGenesisMsg));
        ChainConfig cf = new ChainConfig(ChainParam.DefaultGenesisVersion,ChainParam.TauCommunityName,ChainParam.DefaultBlockTimeInterval,ChainParam.TauGenesisMinerPubkey,ChainParam.TauGenerationSignature,msg);
        cf.GenesisTimeStamp = ChainParam.TauGenesisTimeStamp;
        cf.Signature = ChainParam.TauGenesisSignature;
        return cf;
    }

    /**
     * when create a new chain this variable is needed.
     * @param version block chain version of new chain.
     * @param communityName community name of new chain community.
     * @param blockTimeInterval block mined time interval on new chain.
     * @param genesisMinerPubkey genesis miner pubkey.
     * @param generationSignature initial generation signature to bring up block chain.
     * @return
     */
    public static ChainConfig NewChainConfig(byte version, String communityName,int blockTimeInterval,String genesisMinerPubkey,
                                             String generationSignature,GenesisMsg msg){
        return new ChainConfig(version,communityName,blockTimeInterval,genesisMinerPubkey,generationSignature,msg);
    }

    /**
     * get genesis config version.
     * @return
     */
    public byte getVersion() {
        return version;
    }

    /**
     * get community name.
     * @return
     */
    public String getCommunityName() {
        return CommunityName;
    }

    /**
     * get block time interval.
     * @return
     */
    public int getBlockTimeInterval() {
        return BlockTimeInterval;
    }

    /**
     * get genesis miner pubkey.
     * @return
     */
    public byte[] getGenesisMinerPubkey() {
        return ByteUtil.toByte(GenesisMinerPubkey);
    }

    /**
     * get genesis timestamp.
     * @return
     */
    public long getGenesisTimeStamp() {
        return GenesisTimeStamp;
    }

    /**
     * get block num.
     * @return
     */
    public int getBlockNum() {
        return BlockNum;
    }

    /**
     * get genesis basetarget.
     * @return
     */
    public BigInteger getBaseTarget() {
        return BaseTarget;
    }

    /**
     * get cummulative difficulty.
     * @return
     */
    public BigInteger getCummulativeDifficulty() {
        return CummulativeDifficulty;
    }

    /**
     * get generation signature.
     * @return
     */
    public byte[] getGenerationSignature() {
        return ByteUtil.toByte(GenerationSignature);
    }

    /**
     * get signature.
     * @return
     */
    public byte[] getSignature() {
        return ByteUtil.toByte(Signature);
    }

    /**
     * get Chainid.
     * @return
     */
    public byte[] getChainid(){
        String id = this.CommunityName + ChainParam.ChainidDelimeter + this.BlockTimeInterval + ChainParam.ChainidDelimeter
                + fingerPrintOfPubkeyAndTime();
        return id.getBytes();
    }

    /**
     * get genesis tx message.
     * @return
     */
    public Transaction getMsg() {
        return Msg;
    }

    /**
     * get genesis utypes tx message.
     * @return
     */
    public io.taucoin.utypes.Transaction getUmsg(){
        return null;
    }
}
