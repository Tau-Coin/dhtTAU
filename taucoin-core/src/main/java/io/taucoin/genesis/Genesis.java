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
package io.taucoin.genesis;
import java.math.BigInteger;

import io.taucoin.config.ChainConfig;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPElement;
import io.taucoin.util.RLPItem;
import io.taucoin.util.RLPList;

public class Genesis {
    private int version;
    private long TimeStamp;
    private int BlockNum=0;
    private BigInteger BaseTarget;
    private BigInteger CummulativeDifficulty;
    private String GenerationSignature;
    private GenesisMsg Msg;
    private String ChainID;
    private String Signature;

    private byte[] rlpEncoded;
    private boolean isParse;

    public Genesis(ChainConfig cf){
            this.version = cf.getVersion();
            this.TimeStamp = cf.getGenesisTimeStamp();
            this.BlockNum = cf.getBlockNum();
            this.BaseTarget = cf.getBaseTarget();
            this.CummulativeDifficulty = cf.getCummulativeDifficulty();
            this.GenerationSignature = cf.getGenerationSignature();
            this.Msg = cf.getMsg();
            this.ChainID = cf.getChainid();
            this.Signature = cf.getSignature();
    }

    public Genesis(byte[] rlpEncoded){
        this.rlpEncoded = rlpEncoded;
        this.isParse = false;
    }

    public byte[] getEncoded(){
        if(rlpEncoded == null){
            byte[] version = RLP.encodeInt(this.version);
            byte[] timestamp = RLP.encodeElement(ByteUtil.longToBytes(this.TimeStamp));
            byte[] blocknum = RLP.encodeInt(this.BlockNum);
            byte[] basetarget = RLP.encodeBigInteger(this.BaseTarget);
            byte[] cummulativediffculty = RLP.encodeBigInteger(this.CummulativeDifficulty);
            byte[] generationsignature = RLP.encodeString(this.GenerationSignature);
            byte[] msgList = this.Msg.getEncoded();
            byte[] chainid = RLP.encodeString(this.ChainID);
            byte[] signature = RLP.encodeString(this.Signature);
            this.rlpEncoded = RLP.encodeList(version,timestamp,blocknum,basetarget,cummulativediffculty,generationsignature,msgList,chainid,signature);
        }
        return this.rlpEncoded;
    }

    private void parseRLP(){
        if(isParse){
            return;
        }else{
            RLPList blocklist = RLP.decode2(this.rlpEncoded);
            RLPList block = (RLPList) blocklist.get(0);
            this.version = ByteUtil.byteArrayToInt(block.get(0).getRLPData());
            this.TimeStamp = ByteUtil.byteArrayToLong(block.get(1).getRLPData());
            this.BlockNum = ByteUtil.byteArrayToInt(block.get(2).getRLPData());
            this.BaseTarget = new BigInteger(block.get(3).getRLPData());
            this.CummulativeDifficulty = new BigInteger(block.get(4).getRLPData()==null ? BigInteger.ZERO.toByteArray():block.get(4).getRLPData());
            this.GenerationSignature = ByteUtil.toHexString(block.get(5).getRLPData());
            this.Msg = new GenesisMsg(block.get(6).getRLPData());
            this.ChainID = new String(block.get(7).getRLPData());
            this.Signature = ByteUtil.toHexString(block.get(8).getRLPData());
            isParse = true;
        }
    }

    public String getChainid(){
        if(!isParse) parseRLP();
        return this.ChainID;
    }
}
