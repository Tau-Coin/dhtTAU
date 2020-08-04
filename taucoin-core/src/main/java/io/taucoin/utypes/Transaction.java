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

public class Transaction {
    private long version;
    private String chainID;
    private long timestamp;
    private long txFee;
    private ArrayList<Long> senderPubkey;
    private long nonce;
    private TxData txData;
    private ArrayList<Long> signature;

    private boolean isParsed;
    private byte[] bEncoded;
    private byte[] bSigEncoded;
    private byte[] txID;

    /**
     * construct complete tx with signature.
     * @param version
     * @param chainID
     * @param timestamp
     * @param txFee
     * @param sender
     * @param nonce
     * @param txData
     * @param signature
     */
    public Transaction(byte version,byte[] chainID,long timestamp,int txFee,byte[] sender
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
            this.chainID = new String(chainID,UTF_8);
            this.timestamp = timestamp;
            this.txFee = txFee;
            this.senderPubkey = ByteUtil.byteArrayToSignLongArray(sender,4);
            this.nonce = nonce;
            this.txData = txData;
            this.signature = ByteUtil.byteArrayToSignLongArray(signature,8);
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
        this.chainID = new String(chainID,UTF_8);
        this.timestamp = timeStamp;
        this.txFee = txFee;
        this.senderPubkey = ByteUtil.byteArrayToSignLongArray(sender,4);
        this.nonce = nonce;
        this.txData = txData;
        isParsed = true;
    }

    /**
     * construct genesis transaction
     */
    public Transaction(byte version,String communityName,int blockTimeInterval,long genesisTimeStamp,String genesisMinerPk,TxData txData){
        this.version = version;
        String str = genesisMinerPk + genesisTimeStamp;
        String hash = ByteUtil.toHexString(HashUtil.sha1hash(str.getBytes()));
        String ID = communityName + ChainParam.ChainidDelimeter + blockTimeInterval + ChainParam.ChainidDelimeter + hash;
        this.chainID = new String(ID.getBytes(),UTF_8);
        this.timestamp = genesisTimeStamp;
        this.txFee = 0;
        this.senderPubkey = ByteUtil.byteArrayToSignLongArray(ByteUtil.toByte(genesisMinerPk),4);
        this.nonce = 0;
        this.txData = txData;
        isParsed = true;
    }

    /**
     * construct transaction from complete byte encoding.
     * @param bEncoded:complete byte encoding.
     */
    public Transaction(byte[] bEncoded) {
        this.bEncoded = bEncoded;
        this.isParsed = false;
    }

    /**
     * encoding transaction to bytes.
     * @return
     */
    public byte[] getEncoded() {
        if(bEncoded == null) {
            List list = new ArrayList();
            list.add(this.version);
            list.add(this.chainID);
            list.add(this.timestamp);
            list.add(this.txFee);
            list.add(this.senderPubkey);
            list.add(this.nonce);
            List txdata = Entry.bdecode(this.txData.getEncoded()).list();
            list.add(txdata);
            list.add(this.signature);
            Entry entry = Entry.fromList(list);
            this.bEncoded = entry.bencode();
        }
        return this.bEncoded;
    }

    /**
     * encoding transaction signature parts which is under protection of cryptographic signature.
     * @return
     */
    public byte[] getSigEncoded() {
        if(bSigEncoded == null) {
            List list = new ArrayList();
            list.add(this.version);
            list.add(this.chainID);
            list.add(this.timestamp);
            list.add(this.txFee);
            list.add(this.senderPubkey);
            list.add(this.nonce);
            List txdata = Entry.bdecode(this.txData.getEncoded()).list();
            list.add(txdata);
            Entry entry = Entry.fromList(list);
            this.bSigEncoded = entry.bencode();
        }
        return bSigEncoded;
    }

    /**
     * parse transaction bytes field to flat block field.
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
            this.txFee = ByteUtil.stringToLong(entrylist.get(3).toString());
            this.senderPubkey = ByteUtil.stringToArrayList(entrylist.get(4).toString());
            this.nonce = ByteUtil.stringToLong(entrylist.get(5).toString());
            this.txData = new TxData(((Entry)entrylist.get(6)).bencode());
            this.signature = ByteUtil.stringToArrayList(entrylist.get(7).toString());
            isParsed = true;
        }
    }

    /**
     * get tx version.
     * @return
     */
    public byte getVersion() {
        if(!isParsed) parseRLP();
        return (byte)version;
    }

    /**
     * get chainid tx belongs to.
     * @return
     */
    public byte[] getChainID() {
        if(!isParsed) parseRLP();
        return chainID.getBytes(UTF_8);
    }

    /**
     * get time stamp.
     * @return
     */
    public long getTimeStamp() {
        if(!isParsed) parseRLP();
        return timestamp;
    }

    /**
     * get tx fee maybe negative.
     * @return
     */
    public long getTxFee() {
        if(!isParsed) parseRLP();
        return txFee;
    }

    /**
     * get tx sender pubkey.
     * @return
     */
    public byte[] getSenderPubkey() {
        if(!isParsed) parseRLP();
        byte[] longbyte0 = ByteUtil.longToBytes(senderPubkey.get(0));
        byte[] longbyte1 = ByteUtil.longToBytes(senderPubkey.get(1));
        byte[] longbyte2 = ByteUtil.longToBytes(senderPubkey.get(2));
        byte[] longbyte3 = ByteUtil.longToBytes(senderPubkey.get(3));

        byte[] pubkeybytes = new byte[32];
        System.arraycopy(longbyte0,0,pubkeybytes,0,8);
        System.arraycopy(longbyte1,0,pubkeybytes,8,8);
        System.arraycopy(longbyte2,0,pubkeybytes,16,8);
        System.arraycopy(longbyte3,0,pubkeybytes,24,8);
        return pubkeybytes;
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
     * set tx signature signed with prikey.
     * @param signature
     */
    public void setSignature(byte[] signature){
        this.signature = ByteUtil.byteArrayToSignLongArray(signature,8);
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
        //if(chainID.length > ChainParam.ChainIDlength) return false;
        if(timestamp > System.currentTimeMillis()/1000 + ChainParam.BlockTimeDrift || timestamp < 0) return false;
//        if(senderPubkey != null && senderPubkey.length != ChainParam.PubkeyLength) return false;
        if(nonce < 0) return false;
//        if(signature != null && signature.length != ChainParam.SignatureLength) return false;
        return true;
    }

    /**
     * sign transaction with sender prikey.
     * @param prikey
     * @return
     */
    public byte[] signTransaction(byte[] prikey){
        byte[] sig = Ed25519.sign(this.getTransactionSigMsg(), this.getSenderPubkey(), prikey);
        this.signature = ByteUtil.byteArrayToSignLongArray(sig,8);
        return getSignature();
    }

    /**
     * verify transaction signature.
     * @return
     */
    public boolean verifyTransactionSig(){
        byte[] signature = this.getSignature();
        byte[] sigmsg = this.getTransactionSigMsg();
        return Ed25519.verify(signature,sigmsg,this.getSenderPubkey());
    }

    /**
     * get tx id(hash of transaction)
     * @return
     */
    public byte[] getTxID(){
        if(txID == null){
           Entry entry = Entry.bdecode(this.getEncoded());
           txID = HashUtil.sha1hash(entry.bencode());
        }
        return txID;
    }
}
