package io.taucoin.types;

import java.math.BigInteger;

import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class Block {
    private byte version;
    private String chainID;
    private long timeStamp;
    private long blockNum;
    private String previousBlockHash;
    private String immutableBlockHash;
    private BigInteger baseTarget;
    private BigInteger cumulativeDifficulty;
    private String generationSignature;
    private Transaction txMsg;
    private long minerBalance;
    private long senderBalance;
    private long receiverBalance;
    private long senderNonce;
    private String signature;

    private byte[] rlpEncoded;
    private boolean isParse;

    public Block(byte version, String chainID, long timeStamp, long blockNum, String previousBlockHash,
                 String immutableBlockHash, BigInteger baseTarget, BigInteger cumulativeDifficulty,
                 String generationSignature, Transaction txMsg, long minerBalance, long senderBalance,
                 long receiverBalance, long senderNonce, String signature) {
        if(chainID.getBytes().length > ChainParam.ChainIDlength){
            throw new IllegalArgumentException("chainid need less than: "+ ChainParam.ChainIDlength);
        }
        if(previousBlockHash.getBytes().length > ChainParam.HashLength){
            throw new IllegalArgumentException("pervious hash need less than: "+ ChainParam.HashLength);
        }
        if(immutableBlockHash.getBytes().length > ChainParam.HashLength){
            throw new IllegalArgumentException("immutable hash need less than: "+ ChainParam.HashLength);
        }
        if(signature.getBytes().length != ChainParam.SignatureLength){
            throw new IllegalArgumentException("signature length should =: " + ChainParam.SignatureLength);
        }
        this.version = version;
        this.chainID = chainID;
        this.timeStamp = timeStamp;
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
        isParse = true;
    }

    public Block(byte[] rlpEncoded){
        this.rlpEncoded = rlpEncoded;
        isParse = false;
    }

    public byte[] getEncoded(){
        if(rlpEncoded == null) {
           byte[] version = RLP.encodeByte(this.version);
           byte[] chainid = RLP.encodeString(this.chainID);
           byte[] timestamp = RLP.encodeElement(ByteUtil.longToBytes(this.timeStamp));
           byte[] blocknum = RLP.encodeElement(ByteUtil.longToBytes(this.blockNum));
           byte[] previousblockhash = RLP.encodeString(this.previousBlockHash);
           byte[] immutableblockhash = RLP.encodeString(this.immutableBlockHash);
           byte[] basetarget = RLP.encodeBigInteger(this.baseTarget);
           byte[] cummulativediff = RLP.encodeBigInteger(this.cumulativeDifficulty);
           byte[] generationSig = RLP.encodeString(this.generationSignature);
           byte[] txmsg = txMsg.getEncoded();
           byte[] minerbalance = RLP.encodeElement(ByteUtil.longToBytes(this.minerBalance));
           byte[] senderbalance = RLP.encodeElement(ByteUtil.longToBytes(this.senderBalance));
           byte[] receiverbalance = RLP.encodeElement(ByteUtil.longToBytes(this.receiverBalance));
           byte[] sendernonce = RLP.encodeElement(ByteUtil.longToBytes(this.senderNonce));
           byte[] signature = RLP.encodeString(this.signature);
           this.rlpEncoded = RLP.encodeList(version,chainid,timestamp,blocknum,previousblockhash,immutableblockhash,
                   basetarget,cummulativediff,generationSig,txmsg,minerbalance,senderbalance,receiverbalance,
                   sendernonce,signature);
        }
        return rlpEncoded;
    }

    private void parseRLP(){
        if(isParse){
            return;
        }else{
            RLPList list = RLP.decode2(this.rlpEncoded);
            RLPList block = (RLPList) list.get(0);
            this.version = block.get(0).getRLPData()[0];
            this.chainID = new String(block.get(1).getRLPData());
            this.timeStamp = ByteUtil.byteArrayToLong(block.get(2).getRLPData());
            this.blockNum = ByteUtil.byteArrayToLong(block.get(3).getRLPData());
            this.previousBlockHash = ByteUtil.toHexString(block.get(4).getRLPData());
            this.immutableBlockHash = ByteUtil.toHexString(block.get(5).getRLPData());
            this.baseTarget = new BigInteger(block.get(6).getRLPData());
            this.cumulativeDifficulty = new BigInteger(block.get(7).getRLPData() == null?BigInteger.ZERO.toByteArray():block.get(7).getRLPData());
            this.generationSignature = ByteUtil.toHexString(block.get(8).getRLPData());
            this.txMsg = new Transaction(block.get(9).getRLPData());
            this.minerBalance = ByteUtil.byteArrayToLong(block.get(10).getRLPData());
            this.senderBalance = ByteUtil.byteArrayToLong(block.get(11).getRLPData());
            this.receiverBalance = ByteUtil.byteArrayToLong(block.get(12).getRLPData());
            this.senderNonce = ByteUtil.byteArrayToLong(block.get(13).getRLPData());
            this.signature = ByteUtil.toHexString(block.get(14).getRLPData());
            isParse = true;
        }
    }

    public byte getVersion() {
        if(!isParse) parseRLP();
        return version;
    }

    public String getChainID() {
        if(!isParse) parseRLP();
        return chainID;
    }

    public long getTimeStamp() {
        if(!isParse) parseRLP();
        return timeStamp;
    }

    public long getBlockNum() {
        if(!isParse) parseRLP();
        return blockNum;
    }

    public String getPreviousBlockHash() {
        if(!isParse) parseRLP();
        return previousBlockHash;
    }

    public String getImmutableBlockHash() {
        if(!isParse) parseRLP();
        return immutableBlockHash;
    }

    public BigInteger getBaseTarget() {
        if(!isParse) parseRLP();
        return baseTarget;
    }

    public BigInteger getCumulativeDifficulty() {
        if(!isParse) parseRLP();
        return cumulativeDifficulty;
    }

    public String getGenerationSignature() {
        if(!isParse) parseRLP();
        return generationSignature;
    }

    public Transaction getTxMsg() {
        if(!isParse) parseRLP();
        return txMsg;
    }

    public long getMinerBalance() {
        if(!isParse) parseRLP();
        return minerBalance;
    }

    public long getSenderBalance() {
        if(!isParse) parseRLP();
        return senderBalance;
    }

    public long getReceiverBalance() {
        if(!isParse) parseRLP();
        return receiverBalance;
    }

    public long getSenderNonce() {
        if(!isParse) parseRLP();
        return senderNonce;
    }

    public String getSignature() {
        if(!isParse) parseRLP();
        return signature;
    }
}
