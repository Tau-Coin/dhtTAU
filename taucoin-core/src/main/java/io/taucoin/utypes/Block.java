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
package io.taucoin.utypes;


import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Entry;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.taucoin.config.ChainConfig;
import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Block {
    private long version;
    private long timestamp;
    private long blockNum;
    private ArrayList<Long> previousBlockHash;
    private ArrayList<Long> immutableBlockHash;
    private long baseTarget;
    private long cumulativeDifficulty;
    private ArrayList<Long> generationSignature;
    private ArrayList<Long> txHash;
    private long minerBalance;
    private long senderBalance;
    private long receiverBalance;
    private long senderNonce;
    private ArrayList<Long> signature;
    private ArrayList<Long> minerPubkey;

    private byte[] encodedBytes;
    private byte[] sigEncodedBytes;
    private byte[] blockHash;
    private boolean isParsed;

    private static enum BlockIndex {
        Version, 
        Timestamp, 
        BlockNum, 
        PBHash, 
        IBHash,
        BaseTarget,
        CDifficulty,
        GSignature,
        TxHash,
        MBalance,
        SBalance,
        RBalance,
        SNonce,
        Signature,
        MPubkey
    }

    /**
    *construct a complete block.
    *use BigInteger to express unsigned long.
    */
    public Block(long version, long timestamp, long blockNum, byte[] previousBlockHash,
                 byte[] immutableBlockHash, BigInteger baseTarget, BigInteger cumulativeDifficulty,
                 byte[] generationSignature, byte[] txHash, long minerBalance, long senderBalance,
                 long receiverBalance, long senderNonce, byte[] signature,byte[] minerPubkey){
        if (previousBlockHash.length > ChainParam.HashLength) {
            throw new IllegalArgumentException("pervious block hash need less than: " + ChainParam.HashLength);
        }
        if (immutableBlockHash.length > ChainParam.HashLength) {
            throw new IllegalArgumentException("immutable block hash need less than: " + ChainParam.HashLength);
        }
        if (signature.length != ChainParam.SignatureLength) {
            throw new IllegalArgumentException("signature length should =: " + ChainParam.SignatureLength);
        }
        if (minerPubkey.length != ChainParam.PubkeyLength) {
            throw new IllegalArgumentException("miner pubkey length should =: " + ChainParam.PubkeyLength);
        }
        this.version = version;
        this.timestamp = timestamp;
        this.blockNum = blockNum;
        this.previousBlockHash = ByteUtil.unAlignByteArrayToSignLongArray(previousBlockHash, ChainParam.HashLongArrayLength);
        this.immutableBlockHash = ByteUtil.unAlignByteArrayToSignLongArray(immutableBlockHash, ChainParam.HashLongArrayLength);
        this.baseTarget = ByteUtil.byteArrayToLong(baseTarget.toByteArray());
        this.cumulativeDifficulty = ByteUtil.byteArrayToLong(cumulativeDifficulty.toByteArray());
        this.generationSignature = ByteUtil.byteArrayToSignLongArray(generationSignature, ChainParam.HashLongArrayLength);
        this.txHash = ByteUtil.unAlignByteArrayToSignLongArray(txHash, ChainParam.HashLongArrayLength);
        this.minerBalance = minerBalance;
        this.senderBalance = senderBalance;
        this.receiverBalance = receiverBalance;
        this.senderNonce = senderNonce;
        this.signature = ByteUtil.byteArrayToSignLongArray(signature, ChainParam.SignLongArrayLength);
        this.minerPubkey = ByteUtil.byteArrayToSignLongArray(minerPubkey, ChainParam.PubkeyLongArrayLength);
        isParsed = true;
    }

    /**
     * construct a block without signature,this can be used to initial a block.
     * @param version: current block version.
     * @param timestamp: unix timestamp block was created.
     * @param blockNum: block index number start with 0.
     * @param previousBlockHash: current block father hash reference.
     * @param immutableBlockHash: the chain immutable point block hash.
     * @param baseTarget:block mining base target(pot).
     * @param cumulativeDifficulty:chain difficulty.
     * @param generationSignature:block mining random number.
     * @param txHash:block transaction hash.
     * @param minerBalance:block miner coin balance.
     * @param senderBalance:transaction sender balance.
     * @param receiverBalance:transaction receiver balance.
     * @param senderNonce:transaction sender nonce(power).
     */
    public Block(long version, long timestamp, long blockNum, byte[] previousBlockHash,
                 byte[] immutableBlockHash, BigInteger baseTarget, BigInteger cumulativeDifficulty,
                 byte[] generationSignature, byte[] txHash, long minerBalance, long senderBalance,
                 long receiverBalance, long senderNonce,byte[] minerPubkey){
        if(previousBlockHash.length > ChainParam.HashLength){
            throw new IllegalArgumentException("pervious block hash need less than: "+ ChainParam.HashLength);
        }
        if(immutableBlockHash.length > ChainParam.HashLength){
            throw new IllegalArgumentException("immutable block hash need less than: "+ ChainParam.HashLength);
        }
        if(minerPubkey.length != ChainParam.PubkeyLength){
            throw new IllegalArgumentException("miner pubkey length should =: " + ChainParam.PubkeyLength);
        }
        this.version = version;
        this.timestamp = timestamp;
        this.blockNum = blockNum;
        this.previousBlockHash = ByteUtil.unAlignByteArrayToSignLongArray(previousBlockHash, ChainParam.HashLongArrayLength);
        this.immutableBlockHash = ByteUtil.unAlignByteArrayToSignLongArray(immutableBlockHash, ChainParam.HashLongArrayLength);
        this.baseTarget = ByteUtil.byteArrayToLong(baseTarget.toByteArray());
        this.cumulativeDifficulty = ByteUtil.byteArrayToLong(cumulativeDifficulty.toByteArray());
        this.generationSignature = ByteUtil.byteArrayToSignLongArray(generationSignature, ChainParam.SignLongArrayLength);
        this.txHash = ByteUtil.unAlignByteArrayToSignLongArray(txHash, ChainParam.HashLongArrayLength);
        this.minerBalance = minerBalance;
        this.senderBalance = senderBalance;
        this.receiverBalance = receiverBalance;
        this.senderNonce = senderNonce;
        this.minerPubkey = ByteUtil.byteArrayToSignLongArray(minerPubkey, ChainParam.PubkeyLongArrayLength);
        isParsed = true;
    }

    /**
     * construct genesis block respecting user intention.
     * @param cf
     */
    public Block(ChainConfig cf){
        this.version = cf.getVersion();
        this.timestamp = cf.getGenesisTimeStamp();
        this.blockNum = cf.getBlockNum();
        this.previousBlockHash = null;
        this.immutableBlockHash = null;
        this.baseTarget = ByteUtil.byteArrayToSignLong(cf.getBaseTarget().toByteArray());
        this.cumulativeDifficulty = ByteUtil.byteArrayToSignLong(cf.getCummulativeDifficulty().toByteArray());
        this.generationSignature = ByteUtil.byteArrayToSignLongArray(cf.getGenerationSignature(), ChainParam.HashLongArrayLength);
        //this.txMsg = cf.getUmsg();
        this.minerBalance = 0;
        this.senderBalance = 0;
        this.receiverBalance = 0;
        this.senderNonce = 0 ;
        this.minerPubkey = ByteUtil.byteArrayToSignLongArray(cf.getGenesisMinerPubkey(), ChainParam.PubkeyLongArrayLength);
        isParsed = true;
    }

    /**
     * construct block from complete byte encoding.
     * @param encodedBytes:complete byte encoding.
     */
    public Block(byte[] encodedBytes){
        this.encodedBytes = encodedBytes;
        isParsed = false;
    }

    /**
     * bencoding block to bytes
     * @return
     */
    public byte[] getEncodedBytes(){
        if(encodedBytes == null) {
           List list = new ArrayList();
           list.add(this.version);
           list.add(this.timestamp);
           list.add(this.blockNum);
           list.add(this.previousBlockHash);
           list.add(this.immutableBlockHash);
           list.add(this.baseTarget);
           list.add(this.cumulativeDifficulty);
           list.add(this.generationSignature);
           list.add(this.txHash);
           list.add(this.minerBalance);
           list.add(this.senderBalance);
           list.add(this.receiverBalance);
           list.add(this.senderNonce);
           list.add(this.signature);
           list.add(this.minerPubkey);
           Entry entry = Entry.fromList(list);
           this.encodedBytes = entry.bencode();
        }
        return encodedBytes;
    }

    /**
     * encoding block signature parts which is under protection of cryptographic signature.
     * @return
     */
    public byte[] getSigEncodedBytes(){
        if(sigEncodedBytes == null) {
            List list = new ArrayList();
            list.add(this.version);
            list.add(this.timestamp);
            list.add(this.blockNum);
            list.add(this.previousBlockHash);
            list.add(this.immutableBlockHash);
            list.add(this.baseTarget);
            list.add(this.cumulativeDifficulty);
            list.add(this.generationSignature);
            list.add(this.txHash);
            list.add(this.minerBalance);
            list.add(this.senderBalance);
            list.add(this.receiverBalance);
            list.add(this.senderNonce);
            list.add(this.minerPubkey);
            Entry entry = Entry.fromList(list);
            this.sigEncodedBytes = entry.bencode();
        }
        return sigEncodedBytes;
    }

    /**
     * parse block bytes field to flat block field.
     */
    private void parseEncodedBytes(){
        if(isParsed){
            return;
        }else{
            Entry entry = Entry.bdecode(this.encodedBytes);
            List entrylist = entry.list();

            this.version = ByteUtil.stringToLong(entrylist.get(BlockIndex.Version.ordinal()).toString());
            this.timestamp = ByteUtil.stringToLong(entrylist.get(BlockIndex.Timestamp.ordinal()).toString());
            this.blockNum = ByteUtil.stringToLong(entrylist.get(BlockIndex.BlockNum.ordinal()).toString());
            this.previousBlockHash = ByteUtil.stringToArrayList(entrylist.get(BlockIndex.PBHash.ordinal()).toString());
            this.immutableBlockHash = ByteUtil.stringToArrayList(entrylist.get(BlockIndex.IBHash.ordinal()).toString());
            this.baseTarget = ByteUtil.stringToLong(entrylist.get(BlockIndex.BaseTarget.ordinal()).toString());
            this.cumulativeDifficulty = ByteUtil.stringToLong(entrylist.get(BlockIndex.CDifficulty.ordinal()).toString());
            this.generationSignature = ByteUtil.stringToArrayList(entrylist.get(BlockIndex.GSignature.ordinal()).toString());

            if(entrylist.size() == (BlockIndex.MPubkey.ordinal()+ 1)){
                this.txHash = ByteUtil.stringToArrayList(entrylist.get(BlockIndex.TxHash.ordinal()).toString());
                this.minerBalance = ByteUtil.stringToLong(entrylist.get(BlockIndex.MBalance.ordinal()).toString());
                this.senderBalance = ByteUtil.stringToLong(entrylist.get(BlockIndex.SBalance.ordinal()).toString());
                this.receiverBalance = ByteUtil.stringToLong(entrylist.get(BlockIndex.RBalance.ordinal()).toString());
                this.senderNonce = ByteUtil.stringToLong(entrylist.get(BlockIndex.SNonce.ordinal()).toString());
                this.signature = ByteUtil.stringToArrayList(entrylist.get(BlockIndex.Signature.ordinal()).toString());
                this.minerPubkey = ByteUtil.stringToArrayList(entrylist.get(BlockIndex.MPubkey.ordinal()).toString());
            }else {
                this.minerBalance = ByteUtil.stringToLong(entrylist.get(BlockIndex.MBalance.ordinal()).toString());
                this.senderBalance = ByteUtil.stringToLong(entrylist.get(BlockIndex.SBalance.ordinal()).toString());
                this.receiverBalance = ByteUtil.stringToLong(entrylist.get(BlockIndex.RBalance.ordinal()).toString());
                this.senderNonce = ByteUtil.stringToLong(entrylist.get(BlockIndex.SNonce.ordinal()).toString());
                this.signature = ByteUtil.stringToArrayList(entrylist.get(BlockIndex.Signature.ordinal()).toString());
                this.minerPubkey = ByteUtil.stringToArrayList(entrylist.get(BlockIndex.MPubkey.ordinal()).toString());
            }
            isParsed = true;
        }
    }

    /**
     * get block version.
     * @return
     */
    public long getVersion() {
        if(!isParsed) parseEncodedBytes();
        return version;
    }

    /**
     * get timestamp.
     * @return
     */
    public long getTimeStamp() {
        if(!isParsed) parseEncodedBytes();
        return timestamp;
    }

    /**
     * get block number.
     * @return
     */
    public long getBlockNum() {
        if(!isParsed) parseEncodedBytes();
        return blockNum;
    }

    /**
     * get previous block hash.
     * @return
     */
    public byte[] getPreviousBlockHash() {
        if(!isParsed) parseEncodedBytes();
        byte[] longbyte0 = ByteUtil.longToBytes(previousBlockHash.get(0));
        byte[] longbyte1 = ByteUtil.longToBytes(previousBlockHash.get(1));
        byte[] longbyte2 = ByteUtil.keep4bytesOfLong(previousBlockHash.get(2));
        byte[] hash = new byte[ChainParam.HashLength];
        System.arraycopy(longbyte0, 0, hash, 0, 8);
        System.arraycopy(longbyte1, 0, hash, 8, 8);
        System.arraycopy(longbyte2, 0, hash, 16, 4);
        return hash;
    }

    /**
     * get immutable block hash.
     * @return
     */
    public byte[] getImmutableBlockHash() {
        if(!isParsed) parseEncodedBytes();
        byte[] longbyte0 = ByteUtil.longToBytes(immutableBlockHash.get(0));
        byte[] longbyte1 = ByteUtil.longToBytes(immutableBlockHash.get(1));
        byte[] longbyte2 = ByteUtil.keep4bytesOfLong(immutableBlockHash.get(2));
        byte[] hash = new byte[ChainParam.HashLength];
        System.arraycopy(longbyte0, 0, hash, 0, 8);
        System.arraycopy(longbyte1, 0, hash, 8, 8);
        System.arraycopy(longbyte2, 0, hash, 16, 4);
        return hash;
    }

    /**
     * get current block basetarget
     * @return
     */
    public BigInteger getBaseTarget() {
        if(!isParsed) parseEncodedBytes();
        byte[] targetbyte = ByteUtil.longToBytes(baseTarget);
        //transfer signed bytes to unsigned.
        return new BigInteger(targetbyte);
    }

    /**
     * get chain difficulty.
     * @return
     */
    public BigInteger getCumulativeDifficulty() {
        if(!isParsed) parseEncodedBytes();
        byte[] difficultybyte = ByteUtil.longToBytes(cumulativeDifficulty);
        return new BigInteger(difficultybyte);
    }

    /**
     * get current block generation signature.
     * @return
     */
    public byte[] getGenerationSignature() {
        if(!isParsed) parseEncodedBytes();
        byte[] longbyte0 = ByteUtil.longToBytes(generationSignature.get(0));
        byte[] longbyte1 = ByteUtil.longToBytes(generationSignature.get(1));
        byte[] longbyte2 = ByteUtil.longToBytes(generationSignature.get(2));
        byte[] geneSigbytes = new byte[ChainParam.HashLength];
        System.arraycopy(longbyte0, 0, geneSigbytes, 0, 8);
        System.arraycopy(longbyte1, 0, geneSigbytes, 8, 8);
        System.arraycopy(longbyte2, 0, geneSigbytes, 16, 4);
        return geneSigbytes;
    }

    /**
     * get current block transaction maybe empty block.
     * @return
     */
    public byte[] getTxHash() {
        if(!isParsed) parseEncodedBytes();
        byte[] longbyte0 = ByteUtil.longToBytes(generationSignature.get(0));
        byte[] longbyte1 = ByteUtil.longToBytes(generationSignature.get(1));
        byte[] longbyte2 = ByteUtil.longToBytes(generationSignature.get(2));
        byte[] txHashBytes = new byte[ChainParam.HashLength];
        System.arraycopy(longbyte0, 0, txHashBytes, 0, 8);
        System.arraycopy(longbyte1, 0, txHashBytes, 8, 8);
        System.arraycopy(longbyte2, 0, txHashBytes, 16, 4);
        return txHashBytes;
    }

    /**
     * get current block miner balance.
     * @return
     */
    public long getMinerBalance() {
        if(!isParsed) parseEncodedBytes();
        return minerBalance;
    }

    /**
     * get transaction sender balance.
     * @return
     */
    public long getSenderBalance() {
        if(!isParsed) parseEncodedBytes();
        return senderBalance;
    }

    /**
     * get receiver balance.
     * @return
     */
    public long getReceiverBalance() {
        if(!isParsed) parseEncodedBytes();
        return receiverBalance;
    }

    /**
     * get transaction sender nonce.
     * @return
     */
    public long getSenderNonce() {
        if(!isParsed) parseEncodedBytes();
        return senderNonce;
    }

    /**
     * watch out for temporary block without signature ,return is null.
     * @return
     */
    public byte[] getSignature() {
        if(!isParsed) parseEncodedBytes();
        byte[] longbyte0 = ByteUtil.longToBytes(signature.get(0));
        byte[] longbyte1 = ByteUtil.longToBytes(signature.get(1));
        byte[] longbyte2 = ByteUtil.longToBytes(signature.get(2));
        byte[] longbyte3 = ByteUtil.longToBytes(signature.get(3));
        byte[] longbyte4 = ByteUtil.longToBytes(signature.get(4));
        byte[] longbyte5 = ByteUtil.longToBytes(signature.get(5));
        byte[] longbyte6 = ByteUtil.longToBytes(signature.get(6));
        byte[] longbyte7 = ByteUtil.longToBytes(signature.get(7));
        byte[] sigbytes = new byte[ChainParam.SignatureLength];
        System.arraycopy(longbyte0, 0, sigbytes, 0, 8);
        System.arraycopy(longbyte1, 0, sigbytes, 8, 8);
        System.arraycopy(longbyte2, 0, sigbytes, 16, 8);
        System.arraycopy(longbyte3, 0, sigbytes, 24, 8);
        System.arraycopy(longbyte4, 0, sigbytes, 32, 8);
        System.arraycopy(longbyte5, 0, sigbytes, 40, 8);
        System.arraycopy(longbyte6, 0, sigbytes, 48, 8);
        System.arraycopy(longbyte7, 0, sigbytes, 56, 8);
        return sigbytes;
    }

    /**
     * get miner pubkey.
     * @return
     */
    public byte[] getMinerPubkey(){
        if(!isParsed) parseEncodedBytes();
        byte[] longbyte0 = ByteUtil.longToBytes(minerPubkey.get(0));
        byte[] longbyte1 = ByteUtil.longToBytes(minerPubkey.get(1));
        byte[] longbyte2 = ByteUtil.longToBytes(minerPubkey.get(2));
        byte[] longbyte3 = ByteUtil.longToBytes(minerPubkey.get(3));

        byte[] pubkeybytes = new byte[ChainParam.PubkeyLength];
        System.arraycopy(longbyte0, 0, pubkeybytes, 0, 8);
        System.arraycopy(longbyte1, 0, pubkeybytes, 8, 8);
        System.arraycopy(longbyte2, 0, pubkeybytes, 16, 8);
        System.arraycopy(longbyte3, 0, pubkeybytes, 24, 8);

        return pubkeybytes;
    }
    /**
     * get block hash
     * @return
     */
    public byte[] getBlockHash(){
        if(blockHash == null){
            blockHash = HashUtil.sha1hash(this.getEncodedBytes());
        }
        return blockHash;
    }

    /**
     * get block signature digest message used to block protection.
     * @return
     */
    public byte[] getBlockSigMsg(){
        MessageDigest digest;
        try{
            digest = MessageDigest.getInstance("SHA-256");
        }catch (NoSuchAlgorithmException e){
            return null;
        }
        return digest.digest(this.getSigEncodedBytes());
    }

    /**
     * sign block with miner prikey.
     * @param prikey
     * @return
     */
    public byte[] signBlock(byte[] prikey){
        /*
        if(this.txMsg.getTxData().getMsgType() == MsgType.GenesisMsgTx){
            this.txMsg.signTransaction(prikey);
        }
        */
        byte[] sig = Ed25519.sign(this.getBlockSigMsg(),getMinerPubkey(), prikey);
        this.signature = ByteUtil.byteArrayToSignLongArray(sig, 8);
        return getSignature();
    }

    /**
     * Validate block
     * 1:paramter is valid?
     * 2:about signature,your should verify it besides.
     * @return
     */
    public boolean isBlockParamValidate(){
        if(!isParsed) parseEncodedBytes();
        if((timestamp > (System.currentTimeMillis()/1000 + ChainParam.BlockTimeDrift)) || timestamp < 0) return false;
        if(blockNum < 0) return false;
        if(minerBalance < 0) return false;
        if(senderBalance < 0) return false;
        if(receiverBalance < 0) return false;
        if(senderNonce < 0 ) return false;
        return true;
    }

    /**
     * verify block signature.
     * @return
     */
    public boolean verifyBlockSig(){
        byte[] signature = this.getSignature();
        byte[] sigmsg = this.getBlockSigMsg();
        return Ed25519.verify(signature,sigmsg,getMinerPubkey());
    }

    /**
     * set block version
     * @param version
     */
    public void setVersion(byte version) {
        this.version = version;
        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * set block time stamp.
     * @param timestamp
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * set block number.
     * @param blockNum
     */
    public void setBlockNum(long blockNum) {
        this.blockNum = blockNum;
        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * set previous block hash.
     * @param previousBlockHash
     */
    public void setPreviousBlockHash(byte[] previousBlockHash) {
        this.previousBlockHash = ByteUtil.byteArrayToSignLongArray(previousBlockHash,3);
        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * set immutable block hash.
     * @param immutableBlockHash
     */
    public void setImmutableBlockHash(byte[] immutableBlockHash) {
        this.immutableBlockHash = ByteUtil.byteArrayToSignLongArray(immutableBlockHash,3);
        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * set block baseTarget.
     * @param baseTarget
     */
    public void setBaseTarget(BigInteger baseTarget) {
        this.baseTarget = ByteUtil.byteArrayToSignLong(baseTarget.toByteArray());;
        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * set block cummulative difficulty.
     * @param cumulativeDifficulty
     */
    public void setCumulativeDifficulty(BigInteger cumulativeDifficulty) {
        this.cumulativeDifficulty = ByteUtil.byteArrayToSignLong(cumulativeDifficulty.toByteArray());
        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * set block generation signature.
     * @param generationSignature
     */
    public void setGenerationSignature(byte[] generationSignature) {
        this.generationSignature = ByteUtil.byteArrayToSignLongArray(generationSignature,4);
        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * set block tx Message.
     * @param txMsg
     */
    public void setTxHash(byte[] txHash) {
        this.txHash = ByteUtil.unAlignByteArrayToSignLongArray(txHash, ChainParam.HashLongArrayLength);
        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * set miner balance.
     * @param minerBalance
     */
    public void setMinerBalance(long minerBalance) {
        this.minerBalance = minerBalance;
        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * set sender balance.
     * @param senderBalance
     */
    public void setSenderBalance(long senderBalance) {
        this.senderBalance = senderBalance;
        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * set receiver balance.
     * @param receiverBalance
     */
    public void setReceiverBalance(long receiverBalance) {
        this.receiverBalance = receiverBalance;
        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * set sender nonce.
     * @param senderNonce
     */
    public void setSenderNonce(long senderNonce) {
        this.senderNonce = senderNonce;
        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * set miner pubkey.
     * @param minerPubkey
     */
    public void setMinerPubkey(byte[] minerPubkey) {
        this.minerPubkey = ByteUtil.byteArrayToSignLongArray(minerPubkey, ChainParam.PubkeyLongArrayLength);
        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * set block Signature
     * @param signature
     */
    public void setSignature(byte[] signature){
        this.signature = ByteUtil.byteArrayToSignLongArray(signature, ChainParam.SignLongArrayLength);
        encodedBytes = null;
    }

}
