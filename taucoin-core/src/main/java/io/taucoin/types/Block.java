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

import io.taucoin.genesis.GenesisConfig;
import io.taucoin.param.ChainParam;

import io.taucoin.util.ByteUtil;
import io.taucoin.util.DateUtil;
import io.taucoin.util.HashUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

import com.frostwire.jlibtorrent.Ed25519;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Block {
        
    private static final Logger logger = LoggerFactory.getLogger("Block");

    // Block字段
    private long version;
    private long timestamp;
    private long blockNum;

    private byte[] verticalHash;    // 代表历史区块immutable items(0: previous)
    private byte[] horizontalHash;  // 代表交易、社区消息immutable items(0: txHash)

    private byte[] immutableBlockHash;  //For voting and fork Hash - 20 Bytes

    // pot
    private BigInteger baseTarget;
    private BigInteger cumulativeDifficulty;
    private byte[] generationSignature;  //Hash - 20 Bytes

    // state
    private BigInteger minerBalance;
    private BigInteger senderBalance;
    private BigInteger receiverBalance;
    private BigInteger senderNonce;

    private byte[] signature;     //Signature - 64 Bytes
    private byte[] minerPubkey;   //Pubkey - 32 Bytes

    // 中间结果，暂存内存，不上链
    private byte[] blockHash;
    private byte[] encodedBytes;
    private byte[] sigEncodedBytes;

    private boolean isParsed;

    private enum BlockIndex {
        Version, 
        Timestamp, 
        BlockNum, 
        VHash, 
        HHash,
        IMBHash,
        BaseTarget,
        CDifficulty,
        GSignature,
        MBalance,
        SBalance,
        RBalance,
        SNonce,
        Signature,
        MPubkey
    }

    /**
     * construct a complete block.
     * @param version: current block version.
     * @param timestamp: unix timestamp block was created.
     * @param blockNum: block index number start with 0.
     * @param verticalHash: the historical block hashes.
     * @param horizontalHash: tx, messages immutable hash.
     * @param immutableBlockHash: the chain immutable point block hash.
     * @param baseTarget:block mining base target(pot).
     * @param cumulativeDifficulty:chain difficulty.
     * @param generationSignature:block mining random number.
     * @param txHash:block transaction hash.
     * @param minerBalance:block miner coin balance.
     * @param senderBalance:transaction sender balance.
     * @param receiverBalance:transaction receiver balance.
     * @param senderNonce:transaction sender nonce(power).
     * @param signature:block signature.
     */
    public  Block(long version, long timestamp, long blockNum,
            byte[] verticalHash, byte[] horizontalHash, byte[] immutableBlockHash,
            BigInteger baseTarget, BigInteger cumulativeDifficulty, byte[] generationSignature,
            BigInteger minerBalance, BigInteger senderBalance, BigInteger receiverBalance, BigInteger senderNonce,
            byte[] signature,byte[] minerPubkey){

        // Check - 可以在构造之前检查
        if (verticalHash.length != ChainParam.HashLength) {
            throw new IllegalArgumentException("Vertical blocks hash should be : " + ChainParam.HashLength + " bytes");
        }
        if (horizontalHash.length != ChainParam.HashLength) {
            throw new IllegalArgumentException("Horizontal blocks hash should be : " + ChainParam.HashLength + " bytes");
        }
        if (immutableBlockHash.length != ChainParam.HashLength) {
            throw new IllegalArgumentException("Immutable block hash should be : " + ChainParam.HashLength + " bytes");
        }
        if (generationSignature.length != ChainParam.HashLength) {
            throw new IllegalArgumentException("GenerationSignature should be : " + ChainParam.HashLength + " bytes");
        }
        if (signature.length != ChainParam.SignatureLength) {
            throw new IllegalArgumentException("Signature should be : " + ChainParam.SignatureLength + " bytes");
        }
        if (minerPubkey.length != ChainParam.PubkeyLength) {
            throw new IllegalArgumentException("Miner pubkey should be : " + ChainParam.PubkeyLength + " bytes");
        }

        this.version = version;
        this.timestamp = timestamp;
        this.blockNum = blockNum;

        this.verticalHash = verticalHash;
        this.horizontalHash = horizontalHash;
        this.immutableBlockHash = immutableBlockHash;

        this.baseTarget = baseTarget;
        this.cumulativeDifficulty = cumulativeDifficulty;
        this.generationSignature = generationSignature;

        this.minerBalance = minerBalance;
        this.senderBalance = senderBalance;
        this.receiverBalance = receiverBalance;
        this.senderNonce = senderNonce;

        this.signature = signature;
        this.minerPubkey = minerPubkey;

        isParsed = true;
    }

    /**
     * construct a complete block.
     * @param version: current block version.
     * @param timestamp: unix timestamp block was created.
     * @param blockNum: block index number start with 0.
     * @param verticalHash: the historical block hashes.
     * @param horizontalHash: tx, messages immutable hash.
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
    public  Block(long version, long timestamp, long blockNum,
            byte[] verticalHash, byte[] horizontalHash, byte[] immutableBlockHash,
            BigInteger baseTarget, BigInteger cumulativeDifficulty, byte[] generationSignature,
            BigInteger minerBalance, BigInteger senderBalance, BigInteger receiverBalance, BigInteger senderNonce,
            byte[] minerPubkey){

        // Check - 可以在构造之前检查
        if (verticalHash.length != ChainParam.HashLength) {
            throw new IllegalArgumentException("Vertical blocks hash should be : " + ChainParam.HashLength + " bytes");
        }
        if (horizontalHash.length != ChainParam.HashLength) {
            throw new IllegalArgumentException("Horizontal blocks hash should be : " + ChainParam.HashLength + " bytes");
        }
        if (immutableBlockHash.length != ChainParam.HashLength) {
            throw new IllegalArgumentException("Immutable block hash should be : " + ChainParam.HashLength + " bytes");
        }
        if (generationSignature.length != ChainParam.HashLength) {
            throw new IllegalArgumentException("GenerationSignature should be : " + ChainParam.HashLength + " bytes");
        }
        if (minerPubkey.length != ChainParam.PubkeyLength) {
            throw new IllegalArgumentException("Miner pubkey should be : " + ChainParam.PubkeyLength + " bytes");
        }

        this.version = version;
        this.timestamp = timestamp;
        this.blockNum = blockNum;

        this.verticalHash = verticalHash;
        this.horizontalHash = horizontalHash;
        this.immutableBlockHash = immutableBlockHash;

        this.baseTarget = baseTarget;
        this.cumulativeDifficulty = cumulativeDifficulty;
        this.generationSignature = generationSignature;

        this.minerBalance = minerBalance;
        this.senderBalance = senderBalance;
        this.receiverBalance = receiverBalance;
        this.senderNonce = senderNonce;

        this.minerPubkey = minerPubkey;

        isParsed = true;
    }

    /**
     * @TODO
     * construct genesis block respecting user intention.
     * @param cf
     */
    /*
    public Block(GenesisConfig cf){

        // Check
        if (cf.getGenerationSignature().length != ChainParam.HashLength) {
            throw new IllegalArgumentException("GenerationSignature should be : " + ChainParam.HashLength + " bytes");
        }
        if (cf.getPubkey().length != ChainParam.PubkeyLength) {
            throw new IllegalArgumentException("Miner pubkey should be : " + ChainParam.PubkeyLength + " bytes");
        }
        if (cf.getSignature().length != ChainParam.SignatureLength) {
            throw new IllegalArgumentException("Signature should be : " + ChainParam.SignatureLength + " bytes");
        }

        this.version = cf.getVersion();
        this.timestamp = cf.getTimeStamp();
        this.blockNum = 0L;

        this.verticalHash = ;
        this.horizontalHash = ;
        this.immutableBlockHash = ;

        this.baseTarget = cf.getBaseTarget();
        this.cumulativeDifficulty = cf.getCummulativeDifficulty();
        this.generationSignature = cf.getGenerationSignature();

        this.minerBalance = 0L;
        this.senderBalance = 0L;
        this.receiverBalance = 0L;
        this.senderNonce = 0L;

        this.minerPubkey = cf.getPubkey();
        this.signature = cf.getSignature();

        isParsed = true;
    }
    */

    /**
     * construct block from complete byte encoding.
     * @param encodedBytes: rlp complete byte encoding.
     */
    public Block(byte[] encodedBytes){
        this.encodedBytes = encodedBytes;
        isParsed = false;
    }

    /**
     * rlp encoding block to bytes
     * @return
     */
    public byte[] getEncoded(){

        if(encodedBytes == null) {

            byte[] version = RLP.encodeElement(ByteUtil.longToBytes(this.version));
            byte[] timestamp = RLP.encodeElement(ByteUtil.longToBytes(this.timestamp));
            byte[] blockNum = RLP.encodeElement(ByteUtil.longToBytes(this.blockNum));

            byte[] verticalHash = RLP.encodeElement(this.verticalHash);
            byte[] horizontalHash = RLP.encodeElement(this.horizontalHash);
            byte[] immutableblockhash = RLP.encodeElement(this.immutableBlockHash);

            byte[] baseTarget = RLP.encodeBigInteger(this.baseTarget);
            byte[] cumulativeDifficulty = RLP.encodeBigInteger(this.cumulativeDifficulty);
            byte[] generationSignature = RLP.encodeElement(this.generationSignature);

            byte[] minerBalance= RLP.encodeBigInteger(this.minerBalance);
            byte[] senderBalance = RLP.encodeBigInteger(this.senderBalance);
            byte[] receiverBalance = RLP.encodeBigInteger(this.receiverBalance);
            byte[] senderNonce = RLP.encodeBigInteger(this.senderNonce);

            byte[] signature = RLP.encodeElement(this.signature);
            byte[] minerPubkey = RLP.encodeElement(this.minerPubkey);

            this.encodedBytes = RLP.encodeList(version, timestamp, blockNum,
                                verticalHash, horizontalHash, immutableBlockHash,
                                baseTarget, cumulativeDifficulty, generationSignature,
                                minerBalance, senderBalance, receiverBalance,  senderNonce,
                                signature, minerPubkey);
        }

        return encodedBytes;
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
    public byte[] getSigEncodedBytes(){

        if (sigEncodedBytes == null) {

            byte[] version = RLP.encodeElement(ByteUtil.longToBytes(this.version));
            byte[] timestamp = RLP.encodeElement(ByteUtil.longToBytes(this.timestamp));
            byte[] blockNum = RLP.encodeElement(ByteUtil.longToBytes(this.blockNum));

            byte[] verticalHash = RLP.encodeElement(this.verticalHash);
            byte[] horizontalHash = RLP.encodeElement(this.horizontalHash);
            byte[] immutableblockhash = RLP.encodeElement(this.immutableBlockHash);

            byte[] baseTarget = RLP.encodeBigInteger(this.baseTarget);
            byte[] cumulativeDifficulty = RLP.encodeBigInteger(this.cumulativeDifficulty);
            byte[] generationSignature = RLP.encodeElement(this.generationSignature);

            byte[] minerBalance= RLP.encodeBigInteger(this.minerBalance);
            byte[] senderBalance = RLP.encodeBigInteger(this.senderBalance);
            byte[] receiverBalance = RLP.encodeBigInteger(this.receiverBalance);
            byte[] senderNonce = RLP.encodeBigInteger(this.senderNonce);

            byte[] minerPubkey = RLP.encodeElement(this.minerPubkey);

            this.encodedBytes = RLP.encodeList(version, timestamp, blockNum,
                                verticalHash, horizontalHash, immutableBlockHash,
                                baseTarget, cumulativeDifficulty, generationSignature,
                                minerBalance, senderBalance, receiverBalance,  senderNonce,
                                minerPubkey);
        }

        return sigEncodedBytes;
    }

    /**
     * parse block bytes field to flat block field.
     */
    private void parseEncodedBytes(){
        if (isParsed) {
            return;
        } else {

            RLPList list = RLP.decode2(this.encodedBytes);
            RLPList block = (RLPList) list.get(0);

            this.version = ByteUtil.byteArrayToLong(block.get(BlockIndex.Version.ordinal()).getRLPData());
            this.timestamp = ByteUtil.byteArrayToLong(block.get(BlockIndex.Timestamp.ordinal()).getRLPData());
            this.blockNum = ByteUtil.byteArrayToLong(block.get(BlockIndex.BlockNum.ordinal()).getRLPData());

            this.verticalHash = block.get(BlockIndex.VHash.ordinal()).getRLPData();
            this.horizontalHash = block.get(BlockIndex.HHash.ordinal()).getRLPData();
            this.immutableBlockHash = block.get(BlockIndex.IMBHash.ordinal()).getRLPData();

            this.baseTarget = new BigInteger(block.get(BlockIndex.BaseTarget.ordinal()).getRLPData());
            this.cumulativeDifficulty = new BigInteger(block.get(BlockIndex.CDifficulty.ordinal()).getRLPData());
            this.generationSignature = block.get(BlockIndex.GSignature.ordinal()).getRLPData();

            this.minerBalance = new BigInteger(block.get(BlockIndex.MBalance.ordinal()).getRLPData());
            this.senderBalance = new BigInteger(block.get(BlockIndex.SBalance.ordinal()).getRLPData());
            this.receiverBalance = new BigInteger(block.get(BlockIndex.RBalance.ordinal()).getRLPData());
            this.senderNonce = new BigInteger(block.get(BlockIndex.SNonce.ordinal()).getRLPData());

            this.signature = block.get(BlockIndex.Signature.ordinal()).getRLPData();
            this.minerPubkey = block.get(BlockIndex.MPubkey.ordinal()).getRLPData();
        }

        isParsed = true;
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
        return this.timestamp;
    }

    /**
     * get block number.
     * @return
     */
    public long getBlockNum() {
        if(!isParsed) parseEncodedBytes();
        return this.blockNum;
    }

    /**
     * get vertical hash.
     * @return
     */
    public byte[] getVerticalHash() {
        if(!isParsed) parseEncodedBytes();
        return this.verticalHash;
    }

    /**
     * get horizontal hash.
     * @return
     */
    public byte[] getHorizontalHash() {
        if(!isParsed) parseEncodedBytes();
        return this.horizontalHash;
    }

    /**
     * get immutable block hash.
     * @return
     */
    public byte[] getImmutableBlockHash() {
        if(!isParsed) parseEncodedBytes();
        return this.immutableBlockHash;
    }

    /**
     * get current block basetarget
     * @return
     */
    public BigInteger getBaseTarget() {
        if(!isParsed) parseEncodedBytes();
        return this.baseTarget;
    }

    /**
     * get chain difficulty.
     * @return
     */
    public BigInteger getCumulativeDifficulty() {
        if(!isParsed) parseEncodedBytes();
        return this.cumulativeDifficulty;
    }

    /**
     * get current block generation signature.
     * @return
     */
    public byte[] getGenerationSignature() {
        if(!isParsed) parseEncodedBytes();
        return this.generationSignature;
    }

    /**
     * get current block miner balance.
     * @return
     */
    public BigInteger getMinerBalance() {
        if(!isParsed) parseEncodedBytes();
        return this.minerBalance;
    }

    /**
     * get transaction sender balance.
     * @return
     */
    public BigInteger getSenderBalance() {
        if(!isParsed) parseEncodedBytes();
        return this.senderBalance;
    }

    /**
     * get receiver balance.
     * @return
     */
    public BigInteger getReceiverBalance() {
        if(!isParsed) parseEncodedBytes();
        return this.receiverBalance;
    }

    /**
     * get transaction sender nonce.
     * @return
     */
    public BigInteger getSenderNonce() {
        if(!isParsed) parseEncodedBytes();
        return this.senderNonce;
    }

    /**
     * get block signature hash
     * @return
     */
    public byte[] getSignature() {
        if(!isParsed) parseEncodedBytes();
        return this.signature;
    }

    /**
     * get miner pubkey
     * @return
     */
    public byte[] getMinerPubkey() {
        if(!isParsed) parseEncodedBytes();
        return this.minerPubkey;
    }

    /**
     * get block hash
     * @return
     */
    public byte[] getBlockHash(){
        if(blockHash == null){
            blockHash = HashUtil.sha1hash(this.getEncoded());
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
        byte[] sig = Ed25519.sign(this.getBlockSigMsg(), getMinerPubkey(), prikey);
        this.signature = sig;
        return sig;
    }

    /**
     * Validate block
     * 1:paramter is valid?
     * 2:about signature,your should verify it besides.
     * @return
     */
    public boolean isBlockParamValidate(){
        if(!isParsed) parseEncodedBytes();

        if((timestamp > (System.currentTimeMillis() / 1000 + ChainParam.BlockTimeDrift)) || timestamp < 0) return false;
        if(blockNum < 0) return false;
        if(minerBalance.compareTo(BigInteger.ZERO) < 0) return false;
        if(senderBalance.compareTo(BigInteger.ZERO) < 0) return false;
        if(receiverBalance.compareTo(BigInteger.ZERO) < 0) return false;
        if(senderNonce.compareTo(BigInteger.ZERO) < 0) return false;

        return true;
    }

    /**
     * verify block signature.
     * @return
     */
    public boolean verifyBlockSig(){
        byte[] signature = this.getSignature();
        byte[] sigmsg = this.getBlockSigMsg();
        return Ed25519.verify(signature, sigmsg, getMinerPubkey());
    }

    /**
     * set block version
     * @param version
     */
    public void setVersion(long version) {
        this.version = version;
        this.encodedBytes = null;
        this.sigEncodedBytes = null;
    }

    /**
     * set block time stamp.
     * @param timestamp
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        this.encodedBytes = null;
        this.sigEncodedBytes = null;
    }

    /**
     * set block number.
     * @param blockNum
     */
    public void setBlockNum(long blockNum) {
        this.blockNum = blockNum;
        this.encodedBytes = null;
        this.sigEncodedBytes = null;
    }

    /**
     * set vertical block hash.
     * @param verticalHash
     */
    public void setVerticalHash(byte[] verticalHash) {
        this.verticalHash = verticalHash;
        this.encodedBytes = null;
        this.sigEncodedBytes = null;
    }

    /**
     * set horizontal block hash.
     * @param horizontalHash
     */
    public void setHorizontalHash(byte[] horizontalHash) {
        this.verticalHash = verticalHash;
        this.encodedBytes = null;
        this.sigEncodedBytes = null;
    }

    /**
     * set immutable block hash.
     * @param immutableBlockHash
     */
    public void setImmutableBlockHash(byte[] immutableBlockHash) {
        this.immutableBlockHash = immutableBlockHash;
        this.encodedBytes = null;
        this.sigEncodedBytes = null;
    }

    /**
     * set block baseTarget.
     * @param baseTarget
     */
    public void setBaseTarget(BigInteger baseTarget) {
        this.baseTarget = baseTarget;
        this.encodedBytes = null;
        this.sigEncodedBytes = null;
    }

    /**
     * set block cummulative difficulty.
     * @param cumulativeDifficulty
     */
    public void setCumulativeDifficulty(BigInteger cumulativeDifficulty) {
        this.cumulativeDifficulty = cumulativeDifficulty;
        this.encodedBytes = null;
        this.sigEncodedBytes = null;
    }

    /**
     * set block generation signature.
     * @param generationSignature
     */
    public void setGenerationSignature(byte[] generationSignature) {
        this.generationSignature = generationSignature;
        this.encodedBytes = null;
        this.sigEncodedBytes = null;
    }

    /**
     * set miner balance.
     * @param minerBalance
     */
    public void setMinerBalance(BigInteger minerBalance) {
        this.minerBalance = minerBalance;
        this.encodedBytes = null;
        this.sigEncodedBytes = null;
    }

    /**
     * set sender balance.
     * @param senderBalance
     */
    public void setSenderBalance(BigInteger senderBalance) {
        this.senderBalance = senderBalance;
        this.encodedBytes = null;
        this.sigEncodedBytes = null;
    }

    /**
     * set receiver balance.
     * @param receiverBalance
     */
    public void setReceiverBalance(BigInteger receiverBalance) {
        this.receiverBalance = receiverBalance;
        this.encodedBytes = null;
        this.sigEncodedBytes = null;
    }

    /**
     * set sender nonce.
     * @param senderNonce
     */
    public void setSenderNonce(BigInteger senderNonce) {
        this.senderNonce = senderNonce;
        this.encodedBytes = null;
        this.sigEncodedBytes = null;
    }

    /**
     * set miner pubkey.
     * @param minerPubkey
     */
    public void setMinerPubkey(byte[] minerPubkey) {
        this.minerPubkey = minerPubkey;
        this.encodedBytes = null;
        this.sigEncodedBytes = null;
    }

    /**
     * set block Signature
     * @param signature
     */
    public void setSignature(byte[] signature){
        this.signature = signature;
        this.encodedBytes = null;
    }

    @Override
    public String toString(){
        StringBuilder strBlock = new StringBuilder();
        strBlock.append("block: [");
        strBlock.append(" Block hash: ").append(ByteUtil.toHexString(this.getBlockHash()));
        strBlock.append(" Version: ").append(this.getVersion());
        strBlock.append(" Timestamp: ").append(this.getTimeStamp());
        strBlock.append(" Blocknum: ").append(this.getBlockNum());
        strBlock.append(" Vertical hash: ").append(this.getVerticalHash());
        strBlock.append(" Horizontal hash: ").append(ByteUtil.toHexString(this.getHorizontalHash()));
        strBlock.append(" Immutable block hash: ").append(ByteUtil.toHexString(this.getImmutableBlockHash()));
        strBlock.append(" BaseTarget: ").append(this.getBaseTarget());
        strBlock.append(" CumulativeDifficulty: ").append(this.getCumulativeDifficulty());
        strBlock.append(" Generationsignature: ").append(ByteUtil.toHexString(this.getGenerationSignature()));
        strBlock.append(" Miner balance: ").append(this.getMinerBalance());
        strBlock.append(" Sender balance: ").append(this.getSenderBalance());
        strBlock.append(" Receiver balance: ").append(this.getReceiverBalance());
        strBlock.append(" Sender nonce: ").append(this.getSenderNonce());
        strBlock.append(" Signature: ").append(ByteUtil.toHexString(this.getSignature()));
        strBlock.append(" Minerpubkey: ").append(ByteUtil.toHexString(this.getMinerPubkey()));
        strBlock.append("]");
        return strBlock.toString();
    }

}
