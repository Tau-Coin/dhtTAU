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

import io.taucoin.param.ChainParam;
import io.taucoin.util.HashUtil;

import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Pair;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Transaction {

    private static final Logger logger = LoggerFactory.getLogger("Transaction");

    // Transaction字段
    protected long version;
    protected long timestamp;
    protected byte[] chainID;

    protected BigInteger txFee;
    protected long txType;

    protected byte[] senderPubkey; //Pubkey - 32 bytes
    protected BigInteger nonce;
    protected byte[] signature;    //Signature - 64 bytes

    // 中间结果，暂存内存，不上链
    protected byte[] encodedBytes;
    protected byte[] sigEncodedBytes;

    protected byte[] txHash;
    protected boolean isParsed;

    public static enum TxIndex {
        Version, 
        Timestamp, 
        ChainID, 
        TxFee, 
        TxType, 
        Sender,
        Nonce,
        Signature,
        TxData,
    }

    /**
     * construct complete tx with signature.
     * @param version
     * @param timestamp
     * @param chainID
     * @param txFee
     * @param txType
     * @param sender
     * @param nonce
     * @param signature
     */
    public Transaction(long version, long timestamp, byte[] chainID, 
                    BigInteger txFee, long txType,
                    byte[] sender, BigInteger nonce, byte[] signature){

        if(sender.length != ChainParam.SenderLength) {
            throw new IllegalArgumentException("Sender address should be : "+ChainParam.SenderLength + " bytes");
        }
        if(signature.length != ChainParam.SignatureLength) {
            throw new IllegalArgumentException("Signature should be : " + ChainParam.SignatureLength + " bytes");
        }

        this.version = version;
        this.timestamp = timestamp;
        this.chainID = chainID;

        this.txFee = txFee;
        this.txType = txType;

        this.senderPubkey = sender;
        this.nonce = nonce;
        this.signature = signature;
    }

    /**
     * construct complete tx without signature.
     * @param version
     * @param timestamp
     * @param chainID
     * @param txFee
     * @param txType
     * @param sender
     * @param nonce
     */
    public Transaction(long version, long timestamp, byte[] chainID, 
                    BigInteger txFee, long txType,
                    byte[] sender, BigInteger nonce){

        if(sender.length != ChainParam.SenderLength) {
            throw new IllegalArgumentException("Sender address should be : "+ChainParam.SenderLength + " bytes");
        }

        this.version = version;
        this.timestamp = timestamp;
        this.chainID = chainID;

        this.txFee = txFee;
        this.txType = txType;

        this.senderPubkey = sender;
        this.nonce = nonce;
    }

    /**
     * construct transaction from complete byte encoding.
     * @param encodedBytes:complete byte encoding.
     */
    public Transaction(byte[] encodedBytes) {
        this.encodedBytes = encodedBytes;
        this.isParsed = false;
    }

    /**
     * get tx version.
     * @return
     */
    public long getVersion() {
        if(!isParsed) parseEncodedBytes();
        return this.version;
    }

    /**
     * get time stamp.
     * @return
     */
    public long getTimeStamp() {
        if(!isParsed) parseEncodedBytes();
        return this.timestamp;
    }

    /**
     * get chainid tx belongs to.
     * @return
     */
    public byte[] getChainID() {
        if(!isParsed) parseEncodedBytes();
        return this.chainID;
    }

    /**
     * get tx fee, > 0 in 1st version
     * @return
     */
    public BigInteger getTxFee() {
        if(!isParsed) parseEncodedBytes();
        return this.txFee;
    }

    /**
     * get tx type, 0-genesis tx, 1-forumNote tx, 2-wiring coins
     * @return
     */
    public long getTxType() {
        if(!isParsed) parseEncodedBytes();
        return this.txType;
    }

    /**
     * get tx sender pubkey.
     * @return
     */
    public byte[] getSenderPubkey() {
        if(!isParsed) parseEncodedBytes();
        return this.senderPubkey;
    }

    /**
     * get tx nonce.
     * @return
     */
    public BigInteger  getNonce() {
        if(!isParsed) parseEncodedBytes();
        return this.nonce;
    }

    /**
     * get transaction signature.
     * @return
     */
    public byte[] getSignature() {
        if(!isParsed) parseEncodedBytes();
        return this.signature;
    }

    /**
     * set tx signature signed with prikey.
     * @param signature
     */
    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    /**
     * get tx sign parts bytes.
     * @return
     */
    public byte[] getTransactionSigMsg() {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        return digest.digest(this.getSigEncodedBytes());
    }

    /**
     * sign transaction with seed.
     * @param seed
     * @return
     */
    public byte[] signTransactionWithSeed(byte[] seed) {
        // create private key from seed
        Pair<byte[], byte[]> keys = Ed25519.createKeypair(seed);
        byte[] sig = Ed25519.sign(this.getTransactionSigMsg(), this.getSenderPubkey(), keys.second);
        this.signature = sig;
        return sig;
    }

    /**
     * sign transaction with sender prikey.
     * @param prikey
     * @return
     */
    public byte[] signTransactionWithPriKey(byte[] prikey) {
        byte[] sig = Ed25519.sign(this.getTransactionSigMsg(), this.getSenderPubkey(), prikey);
        this.signature = sig;
        return sig;
    }

    /**
     * verify transaction signature.
     * @return
     */
    public boolean verifyTransactionSig() {
        byte[] signature = this.getSignature();
        byte[] sigmsg = this.getTransactionSigMsg();
        return Ed25519.verify(signature, sigmsg, this.getSenderPubkey());
    }

    /**
     * Validate transaction
     * 1:paramter is valid?
     * 2:about signature,your should verify it besides.
     * @return
     */
    public boolean isTxParamValidate(){
        if(!isParsed) parseEncodedBytes();
        if(this.timestamp > System.currentTimeMillis() / 1000 + ChainParam.BlockTimeDrift || timestamp < 0) return false;
        if(this.nonce.compareTo(BigInteger.ZERO) < 0) return false;
        return true;
    }

    /**
     * get tx id(hash of transaction)
     * @return
     */
    public byte[] getTxID(){
        if(this.txHash == null){
           this.txHash = HashUtil.sha1hash(this.getEncoded());
        }
        return this.txHash;
    }

    /**
     * encoding transaction to bytes.
     * @return
     */
    public abstract byte[] getEncoded();

    /**
     * encoding transaction signature parts which is under protection of cryptographic signature.
     * @return
     */
    public abstract byte[] getSigEncodedBytes();

    /**
     * parse transaction bytes field to flat block field.
     */
    public abstract void parseEncodedBytes();
}
