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
import io.taucoin.util.ByteUtil;
import io.taucoin.util.HashUtil;

import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Pair;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class Transaction {

    private static final Logger logger = LoggerFactory.getLogger("Transaction");

    // Transaction字段
    protected long version;
    protected String chainID;    //String encoding: UTF-8
    protected long timestamp;
    protected long txFee;
    protected long txType;
    protected ArrayList<Long> senderPubkey; //Pubkey - 32 bytes, 4 longs
    protected long nonce;
    protected ArrayList<Long> signature;    //Signature - 64 bytes, 8 longs

    // 中间结果，暂存内存，不上链
    protected byte[] encodedBytes;
    protected byte[] sigEncodedBytes;
    protected byte[] txHash;
    protected boolean isParsed;

    public static enum TxIndex {
        Version, 
        ChainID, 
        Timestamp, 
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
     * @param chainID
     * @param timestamp
     * @param txFee
     * @param txType
     * @param sender
     * @param nonce
     * @param signature
     */
    public Transaction(long version, byte[] chainID, long timestamp, long txFee, long txType, byte[] sender, 
            long nonce, byte[] signature){
        if(sender.length != ChainParam.SenderLength) {
            throw new IllegalArgumentException("Sender address should be : "+ChainParam.SenderLength + " bytes");
        }
        if(signature.length != ChainParam.SignatureLength) {
            throw new IllegalArgumentException("Signature should be : " + ChainParam.SignatureLength + " bytes");
        }
        this.version = version;
        this.chainID = new String(chainID, UTF_8);
        this.timestamp = timestamp;
        this.txFee = txFee;
        this.txType = txType;
        this.senderPubkey = ByteUtil.byteArrayToSignLongArray(sender, ChainParam.PubkeyLongArrayLength);
        this.nonce = nonce;
        this.signature = ByteUtil.byteArrayToSignLongArray(signature, ChainParam.SignLongArrayLength);
    }

    /**
     * construct complete tx without signature.
     * @param version
     * @param chainID
     * @param timestamp
     * @param txFee
     * @param txType
     * @param sender
     * @param nonce
     */
    public Transaction(long version, byte[] chainID, long timestamp, long txFee, long txType, byte[] sender, long nonce){
        if(sender.length != ChainParam.SenderLength) {
            throw new IllegalArgumentException("Sender address should be : "+ChainParam.SenderLength + " bytes");
        }
        this.version = version;
        this.chainID = new String(chainID);
        this.timestamp = timestamp;
        this.txFee = txFee;
        this.txType = txType;
        this.senderPubkey = ByteUtil.byteArrayToSignLongArray(sender, ChainParam.PubkeyLongArrayLength);
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
        return version;
    }

    /**
     * get chainid tx belongs to.
     * @return
     */
    public byte[] getChainID() {
        if(!isParsed) parseEncodedBytes();
        return chainID.getBytes(UTF_8);
    }

    /**
     * get time stamp.
     * @return
     */
    public long getTimeStamp() {
        if(!isParsed) parseEncodedBytes();
        return timestamp;
    }

    /**
     * get tx fee, > 0 in 1st version
     * @return
     */
    public long getTxFee() {
        if(!isParsed) parseEncodedBytes();
        return txFee;
    }

    /**
     * get tx type, 0-genesis tx, 1-forumNote tx, 2-wiring coins
     * @return
     */
    public long getTxType() {
        if(!isParsed) parseEncodedBytes();
        return txType;
    }

    /**
     * get tx sender pubkey.
     * @return
     */
    public byte[] getSenderPubkeyCowTC() {
        if(!isParsed) parseEncodedBytes();
        return ByteUtil.longArrayToBytes(senderPubkey, ChainParam.PubkeyLength);
    }

    /**
     * get tx sender pubkey.
     * @return
     */
    public byte[] getSenderPubkey() {
        if(!isParsed) parseEncodedBytes();

        byte[] longbyte0 = ByteUtil.longToBytes(senderPubkey.get(0));
        byte[] longbyte1 = ByteUtil.longToBytes(senderPubkey.get(1));
        byte[] longbyte2 = ByteUtil.longToBytes(senderPubkey.get(2));
        byte[] longbyte3 = ByteUtil.longToBytes(senderPubkey.get(3));
        byte[] pubkeybytes = new byte[ChainParam.PubkeyLength];

        System.arraycopy(longbyte0, 0, pubkeybytes, 0, 8);
        System.arraycopy(longbyte1, 0, pubkeybytes, 8, 8);
        System.arraycopy(longbyte2, 0, pubkeybytes, 16, 8);
        System.arraycopy(longbyte3, 0, pubkeybytes, 24, 8);

        return pubkeybytes;
    }

    /**
     * get tx nonce.
     * @return
     */
    public long getNonce() {
        if(!isParsed) parseEncodedBytes();
        return nonce;
    }

    /**
     * get transaction signature.
     * @return
     */
    public byte[] getSignatureCowTC() {
        if(!isParsed) parseEncodedBytes();
        return ByteUtil.longArrayToBytes(signature, ChainParam.SignatureLength);
    }

    /**
     * get transaction signature.
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
        byte[] sigBytes = new byte[ChainParam.SignatureLength];

        System.arraycopy(longbyte0, 0, sigBytes, 0, 8);
        System.arraycopy(longbyte1, 0, sigBytes, 8, 8);
        System.arraycopy(longbyte2, 0, sigBytes, 16, 8);
        System.arraycopy(longbyte3, 0, sigBytes, 24, 8);
        System.arraycopy(longbyte4, 0, sigBytes, 32, 8);
        System.arraycopy(longbyte5, 0, sigBytes, 40, 8);
        System.arraycopy(longbyte6, 0, sigBytes, 48, 8);
        System.arraycopy(longbyte7, 0, sigBytes, 56, 8);

        return sigBytes;
    }

    /**
     * set tx signature signed with prikey.
     * @param signature
     */
    public void setSignature(byte[] signature) {
        this.signature = ByteUtil.byteArrayToSignLongArray(signature,8);
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
        this.signature = ByteUtil.byteArrayToSignLongArray(sig, 8);
        return sig;
    }

    /**
     * sign transaction with sender prikey.
     * @param prikey
     * @return
     */
    public byte[] signTransactionWithPriKey(byte[] prikey) {
        byte[] sig = Ed25519.sign(this.getTransactionSigMsg(), this.getSenderPubkey(), prikey);
        this.signature = ByteUtil.byteArrayToSignLongArray(sig, 8);
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
        if(timestamp > System.currentTimeMillis() / 1000 + ChainParam.BlockTimeDrift || timestamp < 0) return false;
        if(nonce < 0) return false;
        return true;
    }

    /**
     * get tx id(hash of transaction)
     * @return
     */
    public byte[] getTxID(){
        if(txHash == null){
           txHash = HashUtil.sha1hash(this.getEncoded());
        }
        return txHash;
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

    /**
     * encoding transaction to long[].
     * @return
     */
    public abstract List getTxLongArray();

}
