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
import com.frostwire.jlibtorrent.Ed25519;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.taucoin.config.ChainConfig;
import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPElement;
import io.taucoin.util.RLPItem;
import io.taucoin.util.RLPList;

public class Genesis {
    private byte version;
    private long TimeStamp;
    private int BlockNum=0;
    private BigInteger BaseTarget;
    private BigInteger CummulativeDifficulty;
    private byte[] GenerationSignature;
    private GenesisMsg Msg;
    private byte[] ChainID;
    private byte[] Signature;
    private byte[] minerPubkey;

    private byte[] rlpEncoded;
    private byte[] rlpSigEncoded;
    private boolean isParsed;

    /**
     * construct genesis block.
     * @param cf
     */
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
            this.minerPubkey = cf.getGenesisMinerPubkey();
            isParsed = true;
    }

    /**
     * construct genesis from encoded code.
     * @param rlpEncoded
     */
    public Genesis(byte[] rlpEncoded){
        this.rlpEncoded = rlpEncoded;
        this.isParsed = false;
    }

    /**
     * get genesis encoded code.
     * @return
     */
    public byte[] getEncoded(){
        if(rlpEncoded == null){
            byte[] version = RLP.encodeByte(this.version);
            byte[] timestamp = RLP.encodeElement(ByteUtil.longToBytes(this.TimeStamp));
            byte[] blocknum = RLP.encodeInt(this.BlockNum);
            byte[] basetarget = RLP.encodeBigInteger(this.BaseTarget);
            byte[] cummulativediffculty = RLP.encodeBigInteger(this.CummulativeDifficulty);
            byte[] generationsignature = RLP.encodeElement(this.GenerationSignature);
            byte[] msgList = this.Msg.getEncoded();
            byte[] chainid = RLP.encodeElement(this.ChainID);
            byte[] signature = RLP.encodeElement(this.Signature);
            byte[] pubkey = RLP.encodeElement(this.minerPubkey);
            this.rlpEncoded = RLP.encodeList(version,timestamp,blocknum,basetarget,cummulativediffculty,generationsignature,msgList,chainid,signature,pubkey);
        }
        return this.rlpEncoded;
    }

    /**
     * parse encoded genesis.
     */
    private void parseRLP(){
        if(isParsed){
            return;
        }else{
            RLPList blocklist = RLP.decode2(this.rlpEncoded);
            RLPList block = (RLPList) blocklist.get(0);
            this.version = block.get(0).getRLPData()[0];
            this.TimeStamp = ByteUtil.byteArrayToLong(block.get(1).getRLPData());
            this.BlockNum = ByteUtil.byteArrayToInt(block.get(2).getRLPData());
            this.BaseTarget = new BigInteger(block.get(3).getRLPData());
            this.CummulativeDifficulty = new BigInteger(block.get(4).getRLPData()==null ? BigInteger.ZERO.toByteArray():block.get(4).getRLPData());
            this.GenerationSignature = block.get(5).getRLPData();
            this.Msg = new GenesisMsg(block.get(6).getRLPData());
            this.ChainID = block.get(7).getRLPData();
            this.Signature = block.get(8).getRLPData();
            this.minerPubkey = block.get(9).getRLPData();
            isParsed = true;
        }
    }

    /**
     * get chainid to mark chain.
     * @return
     */
    public String getChainid(){
        if(!isParsed) parseRLP();
        return new String(this.ChainID);
    }

    /**
     * get signature encode parts to protect it from corruption.
     * @return
     */
    public byte[] getSigEncoded(){
        if(rlpSigEncoded == null){
            byte[] version = RLP.encodeByte(this.version);
            byte[] timestamp = RLP.encodeElement(ByteUtil.longToBytes(this.TimeStamp));
            byte[] blocknum = RLP.encodeInt(this.BlockNum);
            byte[] basetarget = RLP.encodeBigInteger(this.BaseTarget);
            byte[] cummulativediffculty = RLP.encodeBigInteger(this.CummulativeDifficulty);
            byte[] generationsignature = RLP.encodeElement(this.GenerationSignature);
            byte[] msgList = this.Msg.getEncoded();
            byte[] chainid = RLP.encodeElement(this.ChainID);
            byte[] pubkey = RLP.encodeElement(this.minerPubkey);
            this.rlpSigEncoded = RLP.encodeList(version,timestamp,blocknum,basetarget,cummulativediffculty,generationsignature,msgList,chainid,pubkey);
        }
        return rlpSigEncoded;
    }

    /**
     * fingerprint of sig encode part.
     * @return
     */
    public byte[] getGenesisSigMsg(){
        MessageDigest digest;
        try{
            digest = MessageDigest.getInstance("SHA-256");
        }catch (NoSuchAlgorithmException e){
            return null;
        }
        return digest.digest(this.getSigEncoded());
    }

    /**
     * sign genesis with prikey.
     * @param prikey
     * @return
     */
    public String signGenesisblock(byte[] prikey){
        byte[] sig = Ed25519.sign(this.getGenesisSigMsg(), this.minerPubkey, prikey);
        this.Signature = sig;
        return ByteUtil.toHexString(sig);
    }

    /**
     * verify genesis signature.
     * @return
     */
    public boolean verifyGenesisSig(){
        byte[] signature = this.Signature;
        byte[] sigmsg = this.getGenesisSigMsg();
        return Ed25519.verify(signature,sigmsg,this.minerPubkey);
    }

    /**
     * get genesis block hash.
     * @return
     */
    public byte[] getGenesisHash(){
        MessageDigest digest;
        try{
            digest = MessageDigest.getInstance("SHA-256");
        }catch (NoSuchAlgorithmException e){
            return null;
        }
        byte[] hash = digest.digest(this.getEncoded());
        return hash;
    }

    /**
     * validate genesis param.
     * @return
     */
    public boolean isGenesisParamValidate(){
        if(this.getEncoded().length > ChainParam.MaxBlockSize) return false;
        if(!isParsed) parseRLP();
        if(TimeStamp > System.currentTimeMillis()/1000 + ChainParam.BlockTimeDrift || TimeStamp < 0) return false;
        if(BlockNum != 0) return false;
        if(1 == BaseTarget.compareTo(ChainParam.MaxBaseTarget)) return false;
        if(1 == CummulativeDifficulty.compareTo(ChainParam.MaxCummulativeDiff)) return false;
        if(GenerationSignature.length != ChainParam.GenerationSigLength) return false;
        if(Msg.validateGenesisMsg() != CheckInfo.CheckPassed) return false;
        if(ChainID.length > ChainParam.ChainIDlength) return false;
        if(Signature != null && Signature.length != ChainParam.SignatureLength) return false;
        if(minerPubkey.length != ChainParam.PubkeyLength) return false;
        return true;
    }
}
