package io.taucoin.types;

import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class Transaction {
    private byte version;
    private String chainID;
    private long timeStamp;
    private short expiredTime;
    private int TxFee;
    private String sender;
    private long nonce;
    private TxData txData;
    private String signature;

    private boolean isParse;
    private byte[] rlpEncoded;
    public Transaction(byte version,String chainID,long timeStamp,short expiredTime,int txFee,String sender
           ,long nonce,TxData txData,String signature){
            if(chainID.getBytes().length > ChainParam.ChainIDlength) {
                throw new IllegalArgumentException("chainid need less than: "+ ChainParam.ChainIDlength);
            }
            if(sender.getBytes().length != ChainParam.SenderLength) {
                throw new IllegalArgumentException("sender address length should =: "+ChainParam.SenderLength);
            }
            if(signature.getBytes().length != ChainParam.SignatureLength) {
                throw new IllegalArgumentException("signature length should =: " + ChainParam.SignatureLength);
            }
            this.version = version;
            this.chainID = chainID;
            this.timeStamp = timeStamp;
            this.expiredTime = expiredTime;
            this.TxFee = txFee;
            this.sender = sender;
            this.nonce = nonce;
            this.txData = txData;
            this.signature = signature;
            isParse = true;
    }

    public Transaction(byte[] rlpEncoded) {
        this.rlpEncoded = rlpEncoded;
        this.isParse = false;
    }

    public byte[] getEncoded() {
        if(rlpEncoded == null) {
            byte[] version = RLP.encodeByte(this.version);
            byte[] chainid = RLP.encodeString(this.chainID);
            byte[] timestamp = RLP.encodeElement(ByteUtil.longToBytes(this.timeStamp));
            byte[] expiretime = RLP.encodeShort(this.expiredTime);
            byte[] txfee = RLP.encodeInt(this.TxFee);
            byte[] sender = RLP.encodeString(this.sender);
            byte[] nonce = RLP.encodeElement(ByteUtil.longToBytes(this.nonce));
            byte[] txdata = this.txData.getEncoded();
            byte[] signature = RLP.encodeString(this.signature);
            this.rlpEncoded = RLP.encodeList(version,chainid,timestamp,expiretime,txfee,sender,nonce,txdata,signature);
        }
        return rlpEncoded;
    }

    private void parseRLP(){
        if(isParse){
            return;
        }else{
            RLPList list = RLP.decode2(this.rlpEncoded);
            RLPList tx = (RLPList) list.get(0);
            this.version = tx.get(0).getRLPData()[0];
            this.chainID = new String(tx.get(1).getRLPData());
            this.timeStamp = ByteUtil.byteArrayToLong(tx.get(2).getRLPData());
            this.expiredTime = ByteUtil.byteArrayToShort(tx.get(3).getRLPData());
            this.TxFee = ByteUtil.byteArrayToInt(tx.get(4).getRLPData());
            this.sender = ByteUtil.toHexString(tx.get(5).getRLPData());
            this.nonce = ByteUtil.byteArrayToLong(tx.get(6).getRLPData());
            this.txData = new TxData(tx.get(7).getRLPData());
            this.signature = ByteUtil.toHexString(tx.get(8).getRLPData());
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

    public short getExpiredTime() {
        if(!isParse) parseRLP();
        return expiredTime;
    }

    public int getTxFee() {
        if(!isParse) parseRLP();
        return TxFee;
    }

    public String getSender() {
        if(!isParse) parseRLP();
        return sender;
    }

    public long getNonce() {
        if(!isParse) parseRLP();
        return nonce;
    }

    public TxData getTxData() {
        if(!isParse) parseRLP();
        return txData;
    }

    public String getSignature() {
        if(!isParse) parseRLP();
        return signature;
    }
}
