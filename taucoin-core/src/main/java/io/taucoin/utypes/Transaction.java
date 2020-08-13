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


import io.taucoin.config.ChainConfig;
import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Entry;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Transaction {

    private long version;
    private String chainID;
    private long timestamp;
    private long txFee;
    private long txType;
    private ArrayList<Long> senderPubkey;
    private long nonce;
    private ArrayList<Long> signature;
    private HashMap<ArrayList<Long>, ArrayList<Long>> genesisMsg; // genesis msg tx
    private String forumNote;    // forum note tx
    private ArrayList<Long> receiverPubkey;     // wiring coins tx
    private long amount;      // wiring coins tx

    private byte[] encodedBytes;
    private byte[] sigEncodedBytes;
    private byte[] txHash;
    private boolean isParsed;

    private static enum TxIndex {
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
     * @param genesisMsg
     * @param forumNote
     * @param receiver
     * @param amount
     * @param signature
     */
    public Transaction(long version, byte[] chainID, long timestamp, int txFee, int txType, byte[] sender, 
            long nonce, byte[] genesisMsg, byte[] forumNote, byte[] receiver, long amount, byte[] signature){
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
        if(txType == ChainParam.TxType.GMsgType.ordinal()) {
            //Todo

        } else if (txType == ChainParam.TxType.FNoteType.ordinal()) {
            this.forumNote = new String(forumNote, UTF_8);
        } else if (txType == ChainParam.TxType.WCoinsType.ordinal()) {
            this.receiverPubkey = ByteUtil.byteArrayToSignLongArray(receiver, ChainParam.PubkeyLongArrayLength);
            this.amount = amount;
        }

        isParsed = true;
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
     * @param genesisMsg
     * @param forumNote
     * @param receiver
     * @param amount
     */
    public Transaction(long version, byte[] chainID, long timestamp, int txFee, int txType, byte[] sender, 
            long nonce, byte[] genesisMsg, byte[] forumNote, byte[] receiver, long amount){
        if(sender.length != ChainParam.SenderLength) {
            throw new IllegalArgumentException("Sender address should be : "+ChainParam.SenderLength + " bytes");
        }
        this.version = version;
        this.chainID = new String(chainID, UTF_8);
        this.timestamp = timestamp;
        this.txFee = txFee;
        this.txType = txType;
        this.senderPubkey = ByteUtil.byteArrayToSignLongArray(sender, ChainParam.PubkeyLongArrayLength);
        this.nonce = nonce;
        if(txType == ChainParam.TxType.GMsgType.ordinal()) {
            //Todo

        } else if (txType == ChainParam.TxType.FNoteType.ordinal()) {
            this.forumNote = new String(forumNote, UTF_8);
        } else if (txType == ChainParam.TxType.WCoinsType.ordinal()) {
            this.receiverPubkey = ByteUtil.byteArrayToSignLongArray(receiver, ChainParam.PubkeyLongArrayLength);
            this.amount = amount;
        }

        isParsed = true;
    }

    /**
     * construct transaction from complete byte encoding.
     * @param bEncodedBytes:complete byte encoding.
     */
    public Transaction(byte[] encodedBytes) {
        this.encodedBytes = encodedBytes;
        this.isParsed = false;
    }

    /**
     * encoding transaction to bytes.
     * @return
     */
    public byte[] getEncodedBytes() {
        if(encodedBytes == null) {
            List list = new ArrayList();
            list.add(this.version);
            list.add(this.chainID);
            list.add(this.timestamp);
            list.add(this.txFee);
            list.add(this.senderPubkey);
            list.add(this.nonce);
            list.add(this.signature);
            if(txType == ChainParam.TxType.GMsgType.ordinal()) {
                list.add(this.genesisMsg);
            } else if (txType == ChainParam.TxType.FNoteType.ordinal()) {
                list.add(this.forumNote);
            } else if (txType == ChainParam.TxType.WCoinsType.ordinal()) {
                list.add(this.receiverPubkey);
                list.add(this.amount);
            }
            Entry entry = Entry.fromList(list);
            this.encodedBytes = entry.bencode();
        }
        return this.encodedBytes;
    }

    /**
     * encoding transaction signature parts which is under protection of cryptographic signature.
     * @return
     */
    public byte[] getSigEncodedBytes() {
        if(sigEncodedBytes == null) {
            List list = new ArrayList();
            list.add(this.version);
            list.add(this.chainID);
            list.add(this.timestamp);
            list.add(this.txFee);
            list.add(this.senderPubkey);
            list.add(this.nonce);
            if(txType == ChainParam.TxType.GMsgType.ordinal()) {
                list.add(this.genesisMsg);
            } else if (txType == ChainParam.TxType.FNoteType.ordinal()) {
                list.add(this.forumNote);
            } else if (txType == ChainParam.TxType.WCoinsType.ordinal()) {
                list.add(this.receiverPubkey);
                list.add(this.amount);
            }
            Entry entry = Entry.fromList(list);
            this.sigEncodedBytes = entry.bencode();
        }
        return sigEncodedBytes;
    }

    /**
     * encoding transaction to long[].
     * @return
     */
    public List getTxLongArray() {
        List list = new ArrayList();
        list.add(this.version);
        list.add(this.chainID);
        list.add(this.timestamp);
        list.add(this.txFee);
        list.add(this.senderPubkey);
        list.add(this.nonce);
        list.add(this.signature);
        if(txType == ChainParam.TxType.GMsgType.ordinal()) {
            list.add(this.genesisMsg);
        } else if (txType == ChainParam.TxType.FNoteType.ordinal()) {
            list.add(this.forumNote);
        } else if (txType == ChainParam.TxType.WCoinsType.ordinal()) {
            list.add(this.receiverPubkey);
            list.add(this.amount);
        }
        return list;
    }

    /**
     * parse transaction bytes field to flat block field.
     */
    private void parseEncodedBytes(){
        if(isParsed) {
            return;
        } else {
            Entry entry = Entry.bdecode(this.encodedBytes);
            List entrylist = entry.list();

            this.version = ByteUtil.stringToLong(entrylist.get(TxIndex.Version.ordinal()).toString());
            this.chainID = entrylist.get(TxIndex.ChainID.ordinal()).toString();
            this.timestamp = ByteUtil.stringToLong(entrylist.get(TxIndex.Timestamp.ordinal()).toString());
            this.txFee = ByteUtil.stringToLong(entrylist.get(TxIndex.TxFee.ordinal()).toString());
            this.txType = ByteUtil.stringToLong(entrylist.get(TxIndex.TxType.ordinal()).toString());
            this.senderPubkey = ByteUtil.stringToArrayList(entrylist.get(TxIndex.Sender.ordinal()).toString());
            this.nonce = ByteUtil.stringToLong(entrylist.get(TxIndex.Nonce.ordinal()).toString());
            this.signature = ByteUtil.stringToArrayList(entrylist.get(TxIndex.Signature.ordinal()).toString());
            if(txType == ChainParam.TxType.GMsgType.ordinal()) {
                //Todo
            } else if (txType == ChainParam.TxType.FNoteType.ordinal()) {
                this.forumNote= entrylist.get(TxIndex.TxData.ordinal()).toString();
            } else if (txType == ChainParam.TxType.WCoinsType.ordinal()) {
                this.receiverPubkey = ByteUtil.stringToArrayList(entrylist.get(TxIndex.TxData.ordinal()).toString());
                this.amount = ByteUtil.stringToLong(entrylist.get(TxIndex.TxData.ordinal()+ 1).toString());
            }

            isParsed = true;
        }
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
     * get tx fee maybe negative.
     * @return
     */
    public long getTxFee() {
        if(!isParsed) parseEncodedBytes();
        return txFee;
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
        return digest.digest(this.getSigEncodedBytes());
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
     * sign transaction with sender prikey.
     * @param prikey
     * @return
     */
    public byte[] signTransaction(byte[] prikey){
        byte[] sig = Ed25519.sign(this.getTransactionSigMsg(), this.getSenderPubkey(), prikey);
        this.signature = ByteUtil.byteArrayToSignLongArray(sig, 8);
        return sig;
    }

    /**
     * verify transaction signature.
     * @return
     */
    public boolean verifyTransactionSig(){
        byte[] signature = this.getSignature();
        byte[] sigmsg = this.getTransactionSigMsg();
        return Ed25519.verify(signature, sigmsg, this.getSenderPubkey());
    }

    /**
     * get tx id(hash of transaction)
     * @return
     */
    public byte[] getTxID(){
        if(txHash == null){
           Entry entry = Entry.bdecode(this.getEncodedBytes());
           txHash = HashUtil.sha1hash(entry.bencode());
        }
        return txHash;
    }
}
