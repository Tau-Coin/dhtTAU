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
    private String chainID;
    private long timestamp;
    private long blockNum;
    //long[] is used to save bytes to express a big unsigned num.
    //when you check this really number you should concatenate these bytes together.
    private ArrayList<Long> previousBlockHash;
    private ArrayList<Long> immutableBlockHash;
    //long is used to save 8 bytes to express a big unsigned number.
    //when you explain this big num you should concatenate these bytes together
    //and treat this number as a unsigned 8 bytes number.
    private long baseTarget;
    private long cumulativeDifficulty;
    //long[] used to save bytes
    private ArrayList<Long> generationSignature;
    private Transaction txMsg;
    private long minerBalance;
    private long senderBalance;
    private long receiverBalance;
    private long senderNonce;
    private ArrayList<Long> signature;
    private ArrayList<Long> minerPubkey;

    private byte[] bEncoded;
    private byte[] bSigEncoded;
    private byte[] hash;
    private boolean isParsed;

    /**
    *construct a complete block.
    *use BigInteger to express unsigned long.
    */
    public Block(byte version, byte[] chainID, long timestamp, long blockNum, byte[] previousBlockHash,
                 byte[] immutableBlockHash, BigInteger baseTarget, BigInteger cumulativeDifficulty,
                 byte[] generationSignature, Transaction txMsg, long minerBalance, long senderBalance,
                 long receiverBalance, long senderNonce, byte[] signature,byte[] minerPubkey){
        if (chainID.length > ChainParam.ChainIDlength) {
            throw new IllegalArgumentException("chainid need less than: " + ChainParam.ChainIDlength);
        }
        if (previousBlockHash.length > ChainParam.HashLength) {
            throw new IllegalArgumentException("pervious hash need less than: " + ChainParam.HashLength);
        }
        if (immutableBlockHash.length > ChainParam.HashLength) {
            throw new IllegalArgumentException("immutable hash need less than: " + ChainParam.HashLength);
        }
        if (signature.length != ChainParam.SignatureLength) {
            throw new IllegalArgumentException("signature length should =: " + ChainParam.SignatureLength);
        }
        if (minerPubkey.length != ChainParam.PubkeyLength) {
            throw new IllegalArgumentException("miner pubkey length should =: " + ChainParam.PubkeyLength);
        }
        this.version = version;
        this.chainID = new String(chainID,UTF_8);
        this.timestamp = timestamp;
        this.blockNum = blockNum;
        this.previousBlockHash = ByteUtil.unAlignByteArrayToSignLongArray(previousBlockHash,3);
        this.immutableBlockHash = ByteUtil.unAlignByteArrayToSignLongArray(immutableBlockHash,3);
        this.baseTarget = ByteUtil.byteArrayToLong(baseTarget.toByteArray());
        this.cumulativeDifficulty = ByteUtil.byteArrayToLong(cumulativeDifficulty.toByteArray());
        this.generationSignature = ByteUtil.byteArrayToSignLongArray(generationSignature,4);
        this.txMsg = txMsg;
        this.minerBalance = minerBalance;
        this.senderBalance = senderBalance;
        this.receiverBalance = receiverBalance;
        this.senderNonce = senderNonce;
        this.signature = ByteUtil.byteArrayToSignLongArray(signature,8);
        this.minerPubkey = ByteUtil.byteArrayToSignLongArray(minerPubkey,4);
        isParsed = true;
    }

    /**
     * construct a block without signature,this can be used to initial a block.
     * @param version: current block version.
     * @param chainID: block attached chainid.
     * @param timestamp: unix timestamp block was created.
     * @param blockNum: block index number start with 0.
     * @param previousBlockHash: current block father hash reference.
     * @param immutableBlockHash: the chain immutable point block hash.
     * @param baseTarget:block mining base target(pot).
     * @param cumulativeDifficulty:chain difficulty.
     * @param generationSignature:block mining random number.
     * @param txMsg:block transaction message.
     * @param minerBalance:block miner coin balance.
     * @param senderBalance:transaction sender balance.
     * @param receiverBalance:transaction receiver balance.
     * @param senderNonce:transaction sender nonce(power).
     */
    public Block(byte version, byte[] chainID, long timestamp, long blockNum, byte[] previousBlockHash,
                 byte[] immutableBlockHash, BigInteger baseTarget, BigInteger cumulativeDifficulty,
                 byte[] generationSignature, Transaction txMsg, long minerBalance, long senderBalance,
                 long receiverBalance, long senderNonce,byte[] minerPubkey){
        if(chainID.length > ChainParam.ChainIDlength){
            throw new IllegalArgumentException("chainid need less than: "+ ChainParam.ChainIDlength);
        }
        if(previousBlockHash.length > ChainParam.HashLength){
            throw new IllegalArgumentException("pervious hash need less than: "+ ChainParam.HashLength);
        }
        if(immutableBlockHash.length > ChainParam.HashLength){
            throw new IllegalArgumentException("immutable hash need less than: "+ ChainParam.HashLength);
        }
        if(minerPubkey.length != ChainParam.PubkeyLength){
            throw new IllegalArgumentException("miner pubkey length should =: " + ChainParam.PubkeyLength);
        }
        this.version = version;
        this.chainID = new String(chainID,UTF_8);
        this.timestamp = timestamp;
        this.blockNum = blockNum;
        this.previousBlockHash = ByteUtil.unAlignByteArrayToSignLongArray(previousBlockHash,3);
        this.immutableBlockHash = ByteUtil.unAlignByteArrayToSignLongArray(immutableBlockHash,3);
        this.baseTarget = ByteUtil.byteArrayToLong(baseTarget.toByteArray());
        this.cumulativeDifficulty = ByteUtil.byteArrayToLong(cumulativeDifficulty.toByteArray());
        this.generationSignature = ByteUtil.byteArrayToSignLongArray(generationSignature,4);
        this.txMsg = txMsg;
        this.minerBalance = minerBalance;
        this.senderBalance = senderBalance;
        this.receiverBalance = receiverBalance;
        this.senderNonce = senderNonce;
        this.minerPubkey = ByteUtil.byteArrayToSignLongArray(minerPubkey,4);
        isParsed = true;
    }

    /**
     * construct genesis block respecting user intention.
     * @param cf
     */
    public Block(ChainConfig cf){
        this.version = cf.getVersion();
        this.chainID = new String(cf.getChainid(),UTF_8);
        this.timestamp = cf.getGenesisTimeStamp();
        this.blockNum = cf.getBlockNum();
        this.previousBlockHash = null;
        this.immutableBlockHash = null;
        this.baseTarget = ByteUtil.byteArrayToSignLong(cf.getBaseTarget().toByteArray());
        this.cumulativeDifficulty = ByteUtil.byteArrayToSignLong(cf.getCummulativeDifficulty().toByteArray());
        this.generationSignature = ByteUtil.byteArrayToSignLongArray(cf.getGenerationSignature(),4);
        this.txMsg = cf.getUmsg();
        this.minerBalance = 0;
        this.senderBalance = 0;
        this.receiverBalance = 0;
        this.senderNonce = 0 ;
        this.minerPubkey = ByteUtil.byteArrayToSignLongArray(cf.getGenesisMinerPubkey(),4);
        isParsed = true;
    }

    /**
     * construct block from complete byte encoding.
     * @param bEncoded:complete byte encoding.
     */
    public Block(byte[] bEncoded){
        this.bEncoded = bEncoded;
        isParsed = false;
    }

    /**
     * encoding block to bytes
     * @return
     */
    public byte[] getEncoded(){
        if(bEncoded == null) {
           List list = new ArrayList();
           list.add(this.version);
           list.add(this.chainID);
           list.add(this.timestamp);
           list.add(this.blockNum);
           list.add(this.previousBlockHash);
           list.add(this.immutableBlockHash);
           list.add(this.baseTarget);
           list.add(this.cumulativeDifficulty);
           list.add(this.generationSignature);

           List txmsg = new ArrayList();
           if(txMsg != null){
               txmsg = Entry.bdecode(txMsg.getEncoded()).list();
           }
           list.add(txmsg);
           list.add(this.minerBalance);
           list.add(this.senderBalance);
           list.add(this.receiverBalance);
           list.add(this.senderNonce);
           list.add(this.signature);
           list.add(this.minerPubkey);
           Entry entry = Entry.fromList(list);
           this.bEncoded = entry.bencode();
        }
        return bEncoded;
    }

    /**
     * encoding block signature parts which is under protection of cryptographic signature.
     * @return
     */
    public byte[] getSigEncoded(){
        if(bSigEncoded == null) {
            List list = new ArrayList();
            list.add(this.version);
            list.add(this.chainID);
            list.add(this.timestamp);
            list.add(this.blockNum);
            list.add(this.previousBlockHash);
            list.add(this.immutableBlockHash);
            list.add(this.baseTarget);
            list.add(this.cumulativeDifficulty);
            list.add(this.generationSignature);

            List txmsg = new ArrayList();
            if(txMsg != null){
                //System.out.println("==========first time ? =======");
                txmsg = Entry.bdecode(txMsg.getEncoded()).list();
            }
            list.add(txmsg);
            list.add(this.minerBalance);
            list.add(this.senderBalance);
            list.add(this.receiverBalance);
            list.add(this.senderNonce);
            list.add(this.minerPubkey);
            Entry entry = Entry.fromList(list);
            this.bSigEncoded = entry.bencode();
        }
        return bSigEncoded;
    }

    /**
     * parse block bytes field to flat block field.
     */
    private void parseRLP(){
        if(isParsed){
            return;
        }else{
            Entry entry = Entry.bdecode(this.bEncoded);
            List entrylist = entry.list();
            this.version = ByteUtil.stringToLong(entrylist.get(0).toString());
            this.chainID = entrylist.get(1).toString();
            this.timestamp = ByteUtil.stringToLong(entrylist.get(2).toString());
            this.blockNum = ByteUtil.stringToLong(entrylist.get(3).toString());
            this.previousBlockHash = ByteUtil.stringToArrayList(entrylist.get(4).toString());
            this.immutableBlockHash = ByteUtil.stringToArrayList(entrylist.get(5).toString());
            this.baseTarget = ByteUtil.stringToLong(entrylist.get(6).toString());
            this.cumulativeDifficulty = ByteUtil.stringToLong(entrylist.get(7).toString());
            this.generationSignature = ByteUtil.stringToArrayList(entrylist.get(8).toString());
            if(entrylist.size() == 16){
                //System.out.println("======> "+entrylist.get(9).toString().replace("\n ",""));
                this.txMsg = new Transaction(((Entry)entrylist.get(9)).bencode());
                this.minerBalance = ByteUtil.stringToLong(entrylist.get(10).toString());
                this.senderBalance = ByteUtil.stringToLong(entrylist.get(11).toString());
                this.receiverBalance = ByteUtil.stringToLong(entrylist.get(12).toString());
                this.senderNonce = ByteUtil.stringToLong(entrylist.get(13).toString());
                this.signature = ByteUtil.stringToArrayList(entrylist.get(14).toString());
                this.minerPubkey = ByteUtil.stringToArrayList(entrylist.get(15).toString());
            }else {
                this.minerBalance = ByteUtil.stringToLong(entrylist.get(9).toString());
                this.senderBalance = ByteUtil.stringToLong(entrylist.get(10).toString());
                this.receiverBalance = ByteUtil.stringToLong(entrylist.get(11).toString());
                this.senderNonce = ByteUtil.stringToLong(entrylist.get(12).toString());
                this.signature = ByteUtil.stringToArrayList(entrylist.get(13).toString());
                this.minerPubkey = ByteUtil.stringToArrayList(entrylist.get(14).toString());
            }
            isParsed = true;
        }
    }

    /**
     * get block version.
     * @return
     */
    public byte getVersion() {
        if(!isParsed) parseRLP();
        return (byte)version;
    }

    /**
     * get chainid.
     * @return
     */
    public byte[] getChainID() {
        if(!isParsed) parseRLP();
        return chainID.getBytes(UTF_8);
    }

    /**
     * get timestamp.
     * @return
     */
    public long getTimeStamp() {
        if(!isParsed) parseRLP();
        return timestamp;
    }

    /**
     * get block number.
     * @return
     */
    public long getBlockNum() {
        if(!isParsed) parseRLP();
        return blockNum;
    }

    /**
     * get previous block hash.
     * @return
     */
    public byte[] getPreviousBlockHash() {
        if(!isParsed) parseRLP();
        byte[] longbyte0 = ByteUtil.longToBytes(previousBlockHash.get(0));
        byte[] longbyte1 = ByteUtil.longToBytes(previousBlockHash.get(1));
        byte[] longbyte2 = ByteUtil.keep4bytesOfLong(previousBlockHash.get(2));
        byte[] hash = new byte[20];
        System.arraycopy(longbyte0,0,hash,0,8);
        System.arraycopy(longbyte1,0,hash,8,8);
        System.arraycopy(longbyte2,0,hash,16,4);
        return hash;
    }

    /**
     * get immutable block hash.
     * @return
     */
    public byte[] getImmutableBlockHash() {
        if(!isParsed) parseRLP();
        byte[] longbyte0 = ByteUtil.longToBytes(immutableBlockHash.get(0));
        byte[] longbyte1 = ByteUtil.longToBytes(immutableBlockHash.get(1));
        byte[] longbyte2 = ByteUtil.keep4bytesOfLong(immutableBlockHash.get(2));
        byte[] hash = new byte[20];
        System.arraycopy(longbyte0,0,hash,0,8);
        System.arraycopy(longbyte1,0,hash,8,8);
        System.arraycopy(longbyte2,0,hash,16,4);
        return hash;
    }

    /**
     * get current block basetarget
     * @return
     */
    public BigInteger getBaseTarget() {
        if(!isParsed) parseRLP();
        byte[] targetbyte = ByteUtil.longToBytes(baseTarget);
        //transfer signed bytes to unsigned.
        return new BigInteger(targetbyte);
    }

    /**
     * get chain difficulty.
     * @return
     */
    public BigInteger getCumulativeDifficulty() {
        if(!isParsed) parseRLP();
        byte[] difficultybyte = ByteUtil.longToBytes(cumulativeDifficulty);
        return new BigInteger(difficultybyte);
    }

    /**
     * get current block generation signature.
     * @return
     */
    public byte[] getGenerationSignature() {
        if(!isParsed) parseRLP();
        byte[] longbyte0 = ByteUtil.longToBytes(generationSignature.get(0));
        byte[] longbyte1 = ByteUtil.longToBytes(generationSignature.get(1));
        byte[] longbyte2 = ByteUtil.longToBytes(generationSignature.get(2));
        byte[] longbyte3 = ByteUtil.longToBytes(generationSignature.get(3));
        byte[] geneSigbytes = new byte[32];
        System.arraycopy(longbyte0,0,geneSigbytes,0,8);
        System.arraycopy(longbyte1,0,geneSigbytes,8,8);
        System.arraycopy(longbyte2,0,geneSigbytes,16,8);
        System.arraycopy(longbyte3,0,geneSigbytes,24,8);
        return geneSigbytes;
    }

    /**
     * get current block transaction maybe empty block.
     * @return
     */
    public Transaction getTxMsg() {
        if(!isParsed) parseRLP();
        return txMsg;
    }

    /**
     * get current block miner balance.
     * @return
     */
    public long getMinerBalance() {
        if(!isParsed) parseRLP();
        return minerBalance;
    }

    /**
     * get transaction sender balance.
     * @return
     */
    public long getSenderBalance() {
        if(!isParsed) parseRLP();
        return senderBalance;
    }

    /**
     * get receiver balance.
     * @return
     */
    public long getReceiverBalance() {
        if(!isParsed) parseRLP();
        return receiverBalance;
    }

    /**
     * get transaction sender nonce.
     * @return
     */
    public long getSenderNonce() {
        if(!isParsed) parseRLP();
        return senderNonce;
    }

    /**
     * watch out for temporary block without signature ,return is null.
     * @return
     */
    public byte[] getSignature() {
        if(!isParsed) parseRLP();
        byte[] longbyte0 = ByteUtil.longToBytes(signature.get(0));
        byte[] longbyte1 = ByteUtil.longToBytes(signature.get(1));
        byte[] longbyte2 = ByteUtil.longToBytes(signature.get(2));
        byte[] longbyte3 = ByteUtil.longToBytes(signature.get(3));
        byte[] longbyte4 = ByteUtil.longToBytes(signature.get(4));
        byte[] longbyte5 = ByteUtil.longToBytes(signature.get(5));
        byte[] longbyte6 = ByteUtil.longToBytes(signature.get(6));
        byte[] longbyte7 = ByteUtil.longToBytes(signature.get(7));
        byte[] sigbytes = new byte[64];
        System.arraycopy(longbyte0,0,sigbytes,0,8);
        System.arraycopy(longbyte1,0,sigbytes,8,8);
        System.arraycopy(longbyte2,0,sigbytes,16,8);
        System.arraycopy(longbyte3,0,sigbytes,24,8);
        System.arraycopy(longbyte4,0,sigbytes,32,8);
        System.arraycopy(longbyte5,0,sigbytes,40,8);
        System.arraycopy(longbyte6,0,sigbytes,48,8);
        System.arraycopy(longbyte7,0,sigbytes,56,8);
        return sigbytes;
    }

    /**
     * get miner pubkey.
     * @return
     */
    public byte[] getMinerPubkey(){
        if(!isParsed) parseRLP();
        byte[] longbyte0 = ByteUtil.longToBytes(minerPubkey.get(0));
        byte[] longbyte1 = ByteUtil.longToBytes(minerPubkey.get(1));
        byte[] longbyte2 = ByteUtil.longToBytes(minerPubkey.get(2));
        byte[] longbyte3 = ByteUtil.longToBytes(minerPubkey.get(3));

        byte[] pubkeybytes = new byte[32];
        System.arraycopy(longbyte0,0,pubkeybytes,0,8);
        System.arraycopy(longbyte1,0,pubkeybytes,8,8);
        System.arraycopy(longbyte2,0,pubkeybytes,16,8);
        System.arraycopy(longbyte3,0,pubkeybytes,24,8);
        return pubkeybytes;
    }

    /**
     * set block version
     * @param version
     */
    public void setVersion(byte version) {
        this.version = version;
    }

    /**
     * set block chainID.
     * @param chainID
     */
    public void setChainID(byte[] chainID) {
        this.chainID = new String(chainID,UTF_8);
    }

    /**
     * set block time stamp.
     * @param timestamp
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * set block number.
     * @param blockNum
     */
    public void setBlockNum(long blockNum) {
        this.blockNum = blockNum;
    }

    /**
     * set previous block hash.
     * @param previousBlockHash
     */
    public void setPreviousBlockHash(byte[] previousBlockHash) {
        this.previousBlockHash = ByteUtil.byteArrayToSignLongArray(previousBlockHash,3);
    }

    /**
     * set immutable block hash.
     * @param immutableBlockHash
     */
    public void setImmutableBlockHash(byte[] immutableBlockHash) {
        this.immutableBlockHash = ByteUtil.byteArrayToSignLongArray(immutableBlockHash,3);
    }

    /**
     * set block baseTarget.
     * @param baseTarget
     */
    public void setBaseTarget(BigInteger baseTarget) {
        this.baseTarget = ByteUtil.byteArrayToSignLong(baseTarget.toByteArray());;
    }

    /**
     * set block cummulative difficulty.
     * @param cumulativeDifficulty
     */
    public void setCumulativeDifficulty(BigInteger cumulativeDifficulty) {
        this.cumulativeDifficulty = ByteUtil.byteArrayToSignLong(cumulativeDifficulty.toByteArray());
    }

    /**
     * set block generation signature.
     * @param generationSignature
     */
    public void setGenerationSignature(byte[] generationSignature) {
        this.generationSignature = ByteUtil.byteArrayToSignLongArray(generationSignature,4);
    }

    /**
     * set block tx Message.
     * @param txMsg
     */
    public void setTxMsg(Transaction txMsg) {
        this.txMsg = txMsg;
    }

    /**
     * set miner balance.
     * @param minerBalance
     */
    public void setMinerBalance(long minerBalance) {
        this.minerBalance = minerBalance;
    }

    /**
     * set sender balance.
     * @param senderBalance
     */
    public void setSenderBalance(long senderBalance) {
        this.senderBalance = senderBalance;
    }

    /**
     * set receiver balance.
     * @param receiverBalance
     */
    public void setReceiverBalance(long receiverBalance) {
        this.receiverBalance = receiverBalance;
    }

    /**
     * set sender nonce.
     * @param senderNonce
     */
    public void setSenderNonce(long senderNonce) {
        this.senderNonce = senderNonce;
    }

    /**
     * set miner pubkey.
     * @param minerPubkey
     */
    public void setMinerPubkey(byte[] minerPubkey) {
        this.minerPubkey = ByteUtil.byteArrayToSignLongArray(minerPubkey,4);
    }

    /**
     * set block Signature
     * @param signature
     */
    public void setSignature(byte[] signature){
        this.signature = ByteUtil.byteArrayToSignLongArray(signature,8);
    }

    /**
     * get block hash
     * @return
     */
    public byte[] getBlockHash(){
        if(hash == null){
            hash = HashUtil.sha1hash(this.getEncoded());
        }
        return hash;
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
        return digest.digest(this.getSigEncoded());
    }

    /**
     * sign block with miner prikey.
     * @param prikey
     * @return
     */
    public byte[] signBlock(byte[] prikey){
        if(this.txMsg.getTxData().getMsgType() == MsgType.GenesisMsg){
            this.txMsg.signTransaction(prikey);
        }
        byte[] sig = Ed25519.sign(this.getBlockSigMsg(),getMinerPubkey(), prikey);
        this.signature = ByteUtil.byteArrayToSignLongArray(sig,8);
        return getSignature();
    }

    /**
     * Validate block
     * 1:paramter is valid?
     * 2:about signature,your should verify it besides.
     * @return
     */
    public boolean isBlockParamValidate(){
//        if(this.getEncoded().length > ChainParam.MaxBlockSize) return false;
        if(!isParsed) parseRLP();
//        if(chainID.length >ChainParam.ChainIDlength) return false;
        if(timestamp > System.currentTimeMillis()/1000 + ChainParam.BlockTimeDrift || timestamp < 0) return false;
        if(blockNum < 0) return false;
//        if(previousBlockHash != null && previousBlockHash.length > ChainParam.HashLength) return false;
//        if(immutableBlockHash != null && immutableBlockHash.length > ChainParam.HashLength) return false;
//        if(1 == baseTarget.compareTo(ChainParam.MaxBaseTarget)) return false;
//        if(1 == cumulativeDifficulty.compareTo(ChainParam.MaxCummulativeDiff)) return false;
//        if(generationSignature.length != ChainParam.GenerationSigLength) return false;
//        if(txMsg != null && !txMsg.isTxParamValidate()) return false;
        if(minerBalance < 0) return false;
        if(senderBalance < 0) return false;
        if(receiverBalance < 0) return false;
        if(senderNonce < 0 ) return false;
//        if(signature != null && signature.length != ChainParam.SignatureLength) return false;
//        if(minerPubkey.length != ChainParam.PubkeyLength) return false;
        return true;
    }

    /**
     * verify block signature.
     * @return
     */
    public boolean verifyBlockSig(){
        byte[] signature = this.getSignature();
        byte[] sigmsg = this.getBlockSigMsg();
//        System.out.println("===================================================");
//        System.out.println(ByteUtil.toHexString(signature));
//        System.out.println(ByteUtil.toHexString(sigmsg));
//        System.out.println(ByteUtil.toHexString(getMinerPubkey()));

        return Ed25519.verify(signature,sigmsg,getMinerPubkey());
    }
}
