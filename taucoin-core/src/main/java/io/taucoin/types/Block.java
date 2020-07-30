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

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.taucoin.config.ChainConfig;
import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class Block {
    private byte version;
    private byte[] chainID;
    private long timestamp;
    private long blockNum;
    private byte[] previousBlockHash;
    private byte[] immutableBlockHash;
    private BigInteger baseTarget;
    private BigInteger cumulativeDifficulty;
    private byte[] generationSignature;
    private Transaction txMsg;
    private long minerBalance;
    private long senderBalance;
    private long receiverBalance;
    private long senderNonce;
    private byte[] signature;
    private byte[] minerPubkey;

    private byte[] rlpEncoded;
    private byte[] rlpSigEncoded;
    private byte[] hash;
    private boolean isParsed;

    /**
    *construct a complete block.
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
        this.chainID = chainID;
        this.timestamp = timestamp;
        this.blockNum = blockNum;
        this.previousBlockHash = previousBlockHash;
        this.immutableBlockHash = immutableBlockHash;
        this.baseTarget = baseTarget;
        this.cumulativeDifficulty = cumulativeDifficulty;
        this.generationSignature = generationSignature;
        this.txMsg = txMsg;
        this.minerBalance = minerBalance;
        this.senderBalance = senderBalance;
        this.receiverBalance = receiverBalance;
        this.senderNonce = senderNonce;
        this.signature = signature;
        this.minerPubkey = minerPubkey;
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
        this.chainID = chainID;
        this.timestamp = timestamp;
        this.blockNum = blockNum;
        this.previousBlockHash = previousBlockHash;
        this.immutableBlockHash = immutableBlockHash;
        this.baseTarget = baseTarget;
        this.cumulativeDifficulty = cumulativeDifficulty;
        this.generationSignature = generationSignature;
        this.txMsg = txMsg;
        this.minerBalance = minerBalance;
        this.senderBalance = senderBalance;
        this.receiverBalance = receiverBalance;
        this.senderNonce = senderNonce;
        this.minerPubkey = minerPubkey;
        isParsed = true;
    }

    /**
     * construct genesis block respecting user intention.
     * @param cf
     */
    public Block(ChainConfig cf){
        this.version = cf.getVersion();
        this.chainID = cf.getChainid();
        this.timestamp = cf.getGenesisTimeStamp();
        this.blockNum = cf.getBlockNum();
        this.previousBlockHash = null;
        this.immutableBlockHash = null;
        this.baseTarget = cf.getBaseTarget();
        this.cumulativeDifficulty = cf.getCummulativeDifficulty();
        this.generationSignature = cf.getGenerationSignature();
        this.txMsg = cf.getMsg();
        this.minerBalance = 0;
        this.senderBalance = 0;
        this.receiverBalance = 0;
        this.senderNonce = 0 ;
        this.minerPubkey = cf.getGenesisMinerPubkey();
        isParsed = true;
    }

    /**
     * construct block from complete byte encoding.
     * @param rlpEncoded:complete byte encoding.
     */
    public Block(byte[] rlpEncoded){
        this.rlpEncoded = rlpEncoded;
        isParsed = false;
    }

    /**
     * encoding block to bytes
     * @return
     */
    public byte[] getEncoded(){
        if(rlpEncoded == null) {
           byte[] version = RLP.encodeByte(this.version);
           byte[] chainid = RLP.encodeElement(this.chainID);
           byte[] timestamp = RLP.encodeElement(ByteUtil.longToBytes(this.timestamp));
           byte[] blocknum = RLP.encodeElement(ByteUtil.longToBytes(this.blockNum));
           byte[] previousblockhash = RLP.encodeElement(this.previousBlockHash);
           byte[] immutableblockhash = RLP.encodeElement(this.immutableBlockHash);
           byte[] basetarget = RLP.encodeBigInteger(this.baseTarget);
           byte[] cummulativediff = RLP.encodeBigInteger(this.cumulativeDifficulty);
           byte[] generationSig = RLP.encodeElement(this.generationSignature);
           byte[] txmsg = new byte[]{};
           if(txMsg != null){
               txmsg = txMsg.getEncoded();
           }
           byte[] minerbalance = RLP.encodeElement(ByteUtil.longToBytes(this.minerBalance));
           byte[] senderbalance = RLP.encodeElement(ByteUtil.longToBytes(this.senderBalance));
           byte[] receiverbalance = RLP.encodeElement(ByteUtil.longToBytes(this.receiverBalance));
           byte[] sendernonce = RLP.encodeElement(ByteUtil.longToBytes(this.senderNonce));
           byte[] signature = RLP.encodeElement(this.signature);
           byte[] minerpubkey = RLP.encodeElement(this.minerPubkey);
           this.rlpEncoded = RLP.encodeList(version,chainid,timestamp,blocknum,previousblockhash,immutableblockhash,
                   basetarget,cummulativediff,generationSig,txmsg,minerbalance,senderbalance,receiverbalance,
                   sendernonce,signature,minerpubkey);
        }
        return rlpEncoded;
    }

    /**
     * get hex string to save in libtorrent.
     * @return
     */
    public String getEncodeHexStr(){
        return ByteUtil.toHexString(getEncoded());
    }
    
    /**
     * encoding block signature parts which is under protection of cryptographic signature.
     * @return
     */
    public byte[] getSigEncoded(){
        if(rlpSigEncoded == null) {
            byte[] version = RLP.encodeByte(this.version);
            byte[] chainid = RLP.encodeElement(this.chainID);
            byte[] timestamp = RLP.encodeElement(ByteUtil.longToBytes(this.timestamp));
            byte[] blocknum = RLP.encodeElement(ByteUtil.longToBytes(this.blockNum));
            byte[] previousblockhash = RLP.encodeElement(this.previousBlockHash);
            byte[] immutableblockhash = RLP.encodeElement(this.immutableBlockHash);
            byte[] basetarget = RLP.encodeBigInteger(this.baseTarget);
            byte[] cummulativediff = RLP.encodeBigInteger(this.cumulativeDifficulty);
            byte[] generationSig = RLP.encodeElement(this.generationSignature);
            byte[] txmsg = new byte[]{};
            if(txMsg != null){
                txmsg = txMsg.getEncoded();
            }
            byte[] minerbalance = RLP.encodeElement(ByteUtil.longToBytes(this.minerBalance));
            byte[] senderbalance = RLP.encodeElement(ByteUtil.longToBytes(this.senderBalance));
            byte[] receiverbalance = RLP.encodeElement(ByteUtil.longToBytes(this.receiverBalance));
            byte[] sendernonce = RLP.encodeElement(ByteUtil.longToBytes(this.senderNonce));
            byte[] minerpubkey = RLP.encodeElement(this.minerPubkey);
            this.rlpSigEncoded = RLP.encodeList(version,chainid,timestamp,blocknum,previousblockhash,immutableblockhash,
                    basetarget,cummulativediff,generationSig,txmsg,minerbalance,senderbalance,receiverbalance,
                    sendernonce,minerpubkey);
        }
        return rlpSigEncoded;
    }

    /**
     * parse block bytes field to flat block field.
     */
    private void parseRLP(){
        if(isParsed){
            return;
        }else{
            RLPList list = RLP.decode2(this.rlpEncoded);
            RLPList block = (RLPList) list.get(0);
            this.version = block.get(0).getRLPData()[0];
            this.chainID = block.get(1).getRLPData();
            this.timestamp = ByteUtil.byteArrayToLong(block.get(2).getRLPData());
            this.blockNum = ByteUtil.byteArrayToLong(block.get(3).getRLPData());
            this.previousBlockHash = block.get(4).getRLPData();
            this.immutableBlockHash = block.get(5).getRLPData();
            this.baseTarget = new BigInteger(block.get(6).getRLPData());
            this.cumulativeDifficulty = new BigInteger(block.get(7).getRLPData() == null?BigInteger.ZERO.toByteArray():block.get(7).getRLPData());
            this.generationSignature = block.get(8).getRLPData();
            if(block.size() == 16){
                this.txMsg = new Transaction(block.get(9).getRLPData());
                this.minerBalance = ByteUtil.byteArrayToLong(block.get(10).getRLPData());
                this.senderBalance = ByteUtil.byteArrayToLong(block.get(11).getRLPData());
                this.receiverBalance = ByteUtil.byteArrayToLong(block.get(12).getRLPData());
                this.senderNonce = ByteUtil.byteArrayToLong(block.get(13).getRLPData());
                this.signature = block.get(14).getRLPData();
                this.minerPubkey = block.get(15).getRLPData();
            }else {
                this.minerBalance = ByteUtil.byteArrayToLong(block.get(9).getRLPData());
                this.senderBalance = ByteUtil.byteArrayToLong(block.get(10).getRLPData());
                this.receiverBalance = ByteUtil.byteArrayToLong(block.get(11).getRLPData());
                this.senderNonce = ByteUtil.byteArrayToLong(block.get(12).getRLPData());
                this.signature = block.get(13).getRLPData();
                this.minerPubkey = block.get(14).getRLPData();
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
        return version;
    }

    /**
     * get chainid.
     * @return
     */
    public byte[] getChainID() {
        if(!isParsed) parseRLP();
        return chainID;
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
        return previousBlockHash;
    }

    /**
     * get immutable block hash.
     * @return
     */
    public byte[] getImmutableBlockHash() {
        if(!isParsed) parseRLP();
        return immutableBlockHash;
    }

    /**
     * get current block basetarget
     * @return
     */
    public BigInteger getBaseTarget() {
        if(!isParsed) parseRLP();
        return baseTarget;
    }

    /**
     * get chain difficulty.
     * @return
     */
    public BigInteger getCumulativeDifficulty() {
        if(!isParsed) parseRLP();
        return cumulativeDifficulty;
    }

    /**
     * get current block generation signature.
     * @return
     */
    public byte[] getGenerationSignature() {
        if(!isParsed) parseRLP();
        return generationSignature;
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
        return signature;
    }

    /**
     * get miner pubkey.
     * @return
     */
    public byte[] getMinerPubkey(){
        if(!isParsed) parseRLP();
        return minerPubkey;
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
        this.chainID = chainID;
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
        this.previousBlockHash = previousBlockHash;
    }

    /**
     * set immutable block hash.
     * @param immutableBlockHash
     */
    public void setImmutableBlockHash(byte[] immutableBlockHash) {
        this.immutableBlockHash = immutableBlockHash;
    }

    /**
     * set block baseTarget.
     * @param baseTarget
     */
    public void setBaseTarget(BigInteger baseTarget) {
        this.baseTarget = baseTarget;
    }

    /**
     * set block cummulative difficulty.
     * @param cumulativeDifficulty
     */
    public void setCumulativeDifficulty(BigInteger cumulativeDifficulty) {
        this.cumulativeDifficulty = cumulativeDifficulty;
    }

    /**
     * set block generation signature.
     * @param generationSignature
     */
    public void setGenerationSignature(byte[] generationSignature) {
        this.generationSignature = generationSignature;
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
        this.minerPubkey = minerPubkey;
    }

    /**
     * set block Signature
     * @param signature
     */
    public void setSignature(byte[] signature){
        this.signature = signature;
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
        byte[] sig = Ed25519.sign(this.getBlockSigMsg(), this.minerPubkey, prikey);
        this.signature = sig;
        return this.signature;
    }

    /**
     * Validate block
     * 1:paramter is valid?
     * 2:about signature,your should verify it besides.
     * @return
     */
    public boolean isBlockParamValidate(){
        if(this.getEncoded().length > ChainParam.MaxBlockSize) return false;
        if(!isParsed) parseRLP();
        if(chainID.length >ChainParam.ChainIDlength) return false;
        if(timestamp > System.currentTimeMillis()/1000 + ChainParam.BlockTimeDrift || timestamp < 0) return false;
        if(blockNum < 0) return false;
        if(previousBlockHash != null && previousBlockHash.length > ChainParam.HashLength) return false;
        if(immutableBlockHash != null && immutableBlockHash.length > ChainParam.HashLength) return false;
        if(1 == baseTarget.compareTo(ChainParam.MaxBaseTarget)) return false;
        if(1 == cumulativeDifficulty.compareTo(ChainParam.MaxCummulativeDiff)) return false;
        if(generationSignature.length != ChainParam.GenerationSigLength) return false;
        if(txMsg != null && !txMsg.isTxParamValidate()) return false;
        if(minerBalance < 0) return false;
        if(senderBalance < 0) return false;
        if(receiverBalance < 0) return false;
        if(senderNonce < 0 ) return false;
        if(signature != null && signature.length != ChainParam.SignatureLength) return false;
        if(minerPubkey.length != ChainParam.PubkeyLength) return false;
        return true;
    }

    /**
     * verify block signature.
     * @return
     */
    public boolean verifyBlockSig(){
        byte[] signature = this.getSignature();
        byte[] sigmsg = this.getBlockSigMsg();
        return Ed25519.verify(signature,sigmsg,this.minerPubkey);
    }
}
