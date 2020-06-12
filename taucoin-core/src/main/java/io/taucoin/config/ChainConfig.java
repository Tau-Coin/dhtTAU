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
package io.taucoin.config;

import java.math.BigInteger;
import io.taucoin.param.ChainParam;

public class ChainConfig {
    private int version;
    private String CommunityName;
    private int BlockTimeInterval=600;
    private String GenesisMinerPubkey="";
    private long GenesisTimeStamp;
    private int BlockNum=0;
    private BigInteger BaseTarget= new BigInteger("0x21D0369D036978");
    private BigInteger CummulativeDifficulty = BigInteger.ZERO;
    private String GenerationSignature;
    private String Signature;

    private  ChainConfig(int version, String communityName,int blockTimeInterval,String genesisMinerPubkey,
                         String generationSignature,String signature){
        this.version = version;
        this.CommunityName = communityName;
        this.BlockTimeInterval = blockTimeInterval;
        this.GenesisMinerPubkey = genesisMinerPubkey;
        this.GenesisTimeStamp = System.currentTimeMillis()/1000;
        this.GenerationSignature = generationSignature;
        this.Signature = signature;
    }

    public static ChainConfig NewTauChainConfig(){
        ChainConfig cf = new ChainConfig(ChainParam.DefaultGenesisVersion,ChainParam.TauCommunityName,ChainParam.DefaultBlockTimeInterval,ChainParam.TauGenesisMinerPubkey,ChainParam.TauGenerationSignature,ChainParam.TauGenesisSignature);
        cf.GenesisTimeStamp = ChainParam.TauGenesisTimeStamp;
        return cf;
    }
    public static ChainConfig NewChainConfig(int version, String communityName,int blockTimeInterval,String genesisMinerPubkey,
                                             String generationSignature,String signature){
        return new ChainConfig(version,communityName,blockTimeInterval,genesisMinerPubkey,generationSignature,signature);
    }
    public int getVersion() {
        return version;
    }

    public String getCommunityName() {
        return CommunityName;
    }

    public int getBlockTimeInterval() {
        return BlockTimeInterval;
    }

    public String getGenesisMinerPubkey() {
        return GenesisMinerPubkey;
    }

    public long getGenesisTimeStamp() {
        return GenesisTimeStamp;
    }

    public int getBlockNum() {
        return BlockNum;
    }

    public BigInteger getBaseTarget() {
        return BaseTarget;
    }

    public BigInteger getCummulativeDifficulty() {
        return CummulativeDifficulty;
    }

    public String getGenerationSignature() {
        return GenerationSignature;
    }

    public String getSignature() {
        return Signature;
    }
}
