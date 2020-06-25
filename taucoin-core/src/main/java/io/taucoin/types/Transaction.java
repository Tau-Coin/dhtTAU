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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class Transaction {
    private byte version;
    private byte[] chainID;
    private long timeStamp;
    private int txFee;
    private byte[] senderPubkey;
    private long nonce;
    private TxData txData;
    private byte[] signature;

    private boolean isParsed;
    private byte[] rlpEncoded;
    private byte[] rlpSigEncoded;
    private byte[] txID;

    /**
     * construct complete tx with signature.
     * @param version
     * @param chainID
     * @param timeStamp
     * @param txFee
     * @param sender
     * @param nonce
     * @param txData
     * @param signature
     */
    public Transaction(byte version,byte[] chainID,long timeStamp,int txFee,byte[] sender
           ,long nonce,TxData txData,byte[] signature){
            if(chainID.length > ChainParam.ChainIDlength) {
                throw new IllegalArgumentException("chainid need less than: "+ ChainParam.ChainIDlength);
            }
            if(sender.length != ChainParam.SenderLength) {
                throw new IllegalArgumentException("sender address length should =: "+ChainParam.SenderLength);
            }
            if(signature.length != ChainParam.SignatureLength) {
                throw new IllegalArgumentException("signature length should =: " + ChainParam.SignatureLength);
            }
            this.version = version;
            this.chainID = chainID;
            this.timeStamp = timeStamp;
            this.txFee = txFee;
            this.senderPubkey = sender;
            this.nonce = nonce;
            this.txData = txData;
            this.signature = signature;
            isParsed = true;
    }

    /**
     * construct temporary transaction without signature.
     * @param version: transaction version in current chain.
     * @param chainID: chain id this transaction referenced to.
     * @param timeStamp: transaction unix timestamp.
     * @param txFee: transaction fee allowed to negative.
     * @param sender:transaction sender.
     * @param nonce: sender transaction counter.
     * @param txData:transaction message.
     */
    public Transaction(byte version,byte[] chainID,long timeStamp,int txFee,byte[] sender
            ,long nonce,TxData txData){
        if(chainID.length > ChainParam.ChainIDlength) {
            throw new IllegalArgumentException("chainid need less than: "+ ChainParam.ChainIDlength);
        }
        if(sender.length != ChainParam.SenderLength) {
            throw new IllegalArgumentException("sender address length should =: "+ChainParam.SenderLength);
        }
        this.version = version;
        this.chainID = chainID;
        this.timeStamp = timeStamp;
        this.txFee = txFee;
        this.senderPubkey = sender;
        this.nonce = nonce;
        this.txData = txData;
        isParsed = true;
    }

    /**
     * construct transaction from complete byte encoding.
     * @param rlpEncoded:complete byte encoding.
     */
    public Transaction(byte[] rlpEncoded) {
        this.rlpEncoded = rlpEncoded;
        this.isParsed = false;
    }

    /**
     * encoding transaction to bytes.
     * @return
     */
    public byte[] getEncoded() {
        if(rlpEncoded == null) {
            byte[] version = RLP.encodeByte(this.version);
            byte[] chainid = RLP.encodeElement(this.chainID);
            byte[] timestamp = RLP.encodeElement(ByteUtil.longToBytes(this.timeStamp));
            byte[] txfee = RLP.encodeInt(this.txFee);
            byte[] sender = RLP.encodeElement(this.senderPubkey);
            byte[] nonce = RLP.encodeElement(ByteUtil.longToBytes(this.nonce));
            byte[] txdata = this.txData.getEncoded();
            byte[] signature = RLP.encodeElement(this.signature);
            this.rlpEncoded = RLP.encodeList(version,chainid,timestamp,txfee,sender,nonce,txdata,signature);
        }
        return rlpEncoded;
    }

    /**
     * encoding transaction signature parts which is under protection of cryptographic signature.
     * @return
     */
    public byte[] getSigEncoded() {
        if(rlpSigEncoded == null) {
            byte[] version = RLP.encodeByte(this.version);
            byte[] chainid = RLP.encodeElement(this.chainID);
            byte[] timestamp = RLP.encodeElement(ByteUtil.longToBytes(this.timeStamp));
            byte[] txfee = RLP.encodeInt(this.txFee);
            byte[] sender = RLP.encodeElement(this.senderPubkey);
            byte[] nonce = RLP.encodeElement(ByteUtil.longToBytes(this.nonce));
            byte[] txdata = this.txData.getEncoded();
            this.rlpSigEncoded = RLP.encodeList(version,chainid,timestamp,txfee,sender,nonce,txdata);
        }
        return rlpSigEncoded;
    }

    /**
     * parse transaction bytes field to flat block field.
     */
    private void parseRLP(){
        if(isParsed){
            return;
        }else{
            RLPList list = RLP.decode2(this.rlpEncoded);
            RLPList tx = (RLPList) list.get(0);
            this.version = tx.get(0).getRLPData()[0];
            this.chainID = tx.get(1).getRLPData();
            this.timeStamp = ByteUtil.byteArrayToLong(tx.get(2).getRLPData());
            this.txFee = ByteUtil.byteArrayToInt(tx.get(3).getRLPData());
            this.senderPubkey = tx.get(4).getRLPData();
            this.nonce = ByteUtil.byteArrayToLong(tx.get(5).getRLPData());
            this.txData = new TxData(tx.get(6).getRLPData());
            this.signature = tx.get(7).getRLPData();
            isParsed = true;
        }
    }

    /**
     * get tx version.
     * @return
     */
    public byte getVersion() {
        if(!isParsed) parseRLP();
        return version;
    }

    /**
     * get chainid tx belongs to.
     * @return
     */
    public byte[] getChainID() {
        if(!isParsed) parseRLP();
        return chainID;
    }

    /**
     * get time stamp.
     * @return
     */
    public long getTimeStamp() {
        if(!isParsed) parseRLP();
        return timeStamp;
    }

    /**
     * get tx fee maybe negative.
     * @return
     */
    public int getTxFee() {
        if(!isParsed) parseRLP();
        return txFee;
    }

    /**
     * get tx sender pubkey.
     * @return
     */
    public byte[] getSenderPubkey() {
        if(!isParsed) parseRLP();
        return senderPubkey;
    }

    /**
     * get tx nonce.
     * @return
     */
    public long getNonce() {
        if(!isParsed) parseRLP();
        return nonce;
    }

    /**
     * get tx data msg.
     * @return
     */
    public TxData getTxData() {
        if(!isParsed) parseRLP();
        return txData;
    }

    /**
     * get transaction signature.
     * @return
     */
    public byte[] getSignature() {
        if(!isParsed) parseRLP();
        return signature;
    }

    /**
     * set tx signature signed with prikey.
     * @param signature
     */
    public void setSignature(byte[] signature){
        this.signature = signature;
    }

    /**
     * get tx sign parts bytes.
     * @return
     */
    public byte[] getTransactionSigMsg(){
        MessageDigest digest;
        try{
            digest = MessageDigest.getInstance("SHA-256");
        }catch (NoSuchAlgorithmException e){
            return null;
        }
        return digest.digest(this.getSigEncoded());
    }

    /**
     * Validate transaction
     * 1:paramter is valid?
     * 2:about signature,your should verify it besides.
     * @return
     */
    public boolean isTxParamValidate(){
        if(!isParsed) parseRLP();
        if(chainID.length > ChainParam.ChainIDlength) return false;
        if(timeStamp > System.currentTimeMillis()/1000 + ChainParam.BlockTimeDrift || timeStamp < 0) return false;
        if(senderPubkey.length != ChainParam.PubkeyLength) return false;
        if(nonce < 0) return false;
        if(signature != null && signature.length != ChainParam.SignatureLength) return false;
        return true;
    }

    /**
     * verify transaction signature.
     * @return
     */
    public boolean verifyBlockSig(){
        byte[] signature = this.getSignature();
        byte[] sigmsg = this.getTransactionSigMsg();
        return Ed25519.verify(signature,sigmsg,this.senderPubkey);
    }

    /**
     * get tx id(hash of transaction)
     * @return
     */
    public byte[] getTxID(){
        if(txID == null){
            MessageDigest digest;
            try{
                digest = MessageDigest.getInstance("SHA-256");
            }catch (NoSuchAlgorithmException e){
                return null;
            }
            txID = digest.digest(this.getEncoded());
        }
        return txID;
    }
}
