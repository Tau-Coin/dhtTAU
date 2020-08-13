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
import io.taucoin.genesis.GenesisItem;
import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Entry;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Transaction {

    private static final Logger logger = LoggerFactory.getLogger("Transaction");

    // Transaction字段
    private long version;
    private String chainID;
    private long timestamp;
    private long txFee;
    private long txType;
    private ArrayList<Long> senderPubkey;   //Pubkey - 32 bytes
    private long nonce;
    private ArrayList<Long> signature;   //Signature - 64 bytes
    private HashMap<ArrayList<Long>, ArrayList<Long>> genesisMsg; // genesis msg tx
    private String forumNote;    // forum note tx
    private ArrayList<Long> receiverPubkey;     // wiring coins tx, pubkey - 32 bytes
    private long amount;      // wiring coins tx

    // 中间结果，暂存内存，不上链
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
    public Transaction(long version, byte[] chainID, long timestamp, long txFee, long txType, byte[] sender, 
            long nonce, HashMap<ByteArrayWrapper, GenesisItem> genesisMsg, byte[] forumNote, byte[] receiver, long amount, byte[] signature){
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
            this.genesisMsg = genesisMapTrans(genesisMsg);
        } else if (txType == ChainParam.TxType.FNoteType.ordinal()) {
            this.forumNote = new String(forumNote, UTF_8);
        } else if (txType == ChainParam.TxType.WCoinsType.ordinal()) {
            this.receiverPubkey = ByteUtil.byteArrayToSignLongArray(receiver, ChainParam.PubkeyLongArrayLength);
            this.amount = amount;
        }

        isParsed = true;
    }

    /**
     * genesis Msg transform 
     * @param genesisMsg
     */
    private HashMap<ArrayList<Long>, ArrayList<Long>> genesisMapTrans(HashMap<ByteArrayWrapper, GenesisItem> genesisMsg) {

        HashMap<ArrayList<Long>, ArrayList<Long>> internalGenesisMap = new HashMap();

        Iterator<ByteArrayWrapper> accountItor = genesisMsg.keySet().iterator();

        while(accountItor.hasNext()) {
            ArrayList<Long> keys = new ArrayList<Long>();
            ArrayList<Long> values = new ArrayList<Long>();

            ByteArrayWrapper key = accountItor.next();
            GenesisItem value = genesisMsg.get(key);

            // key -> arraylist
            keys = ByteUtil.byteArrayToSignLongArray(key.getData(), ChainParam.PubkeyLongArrayLength);

            // value -> arraylist
            values.add(value.getBalance().longValue());
            values.add(value.getPower().longValue());

            internalGenesisMap.put(keys, values);
		}

        return internalGenesisMap;
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
    public Transaction(long version, byte[] chainID, long timestamp, long txFee, long txType, byte[] sender, 
            long nonce, HashMap<ByteArrayWrapper, GenesisItem> genesisMsg, byte[] forumNote, byte[] receiver, long amount){
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
            this.genesisMsg = genesisMapTrans(genesisMsg);
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
            List<Entry> entrylist = entry.list();
            this.version = entrylist.get(TxIndex.Version.ordinal()).integer();
            this.chainID = entrylist.get(TxIndex.ChainID.ordinal()).toString();
            this.timestamp = entrylist.get(TxIndex.Timestamp.ordinal()).integer();
            this.txFee = entrylist.get(TxIndex.TxFee.ordinal()).integer();
            this.txType = entrylist.get(TxIndex.TxType.ordinal()).integer();
            this.senderPubkey = ByteUtil.stringToArrayList(entrylist.get(TxIndex.Sender.ordinal()).toString());
            this.nonce = entrylist.get(TxIndex.Nonce.ordinal()).integer();
            this.signature = ByteUtil.stringToArrayList(entrylist.get(TxIndex.Signature.ordinal()).toString());
            if(txType == ChainParam.TxType.GMsgType.ordinal()) {
                //Todo, string -> HashMap
            } else if (txType == ChainParam.TxType.FNoteType.ordinal()) {
                this.forumNote= entrylist.get(TxIndex.TxData.ordinal()).toString();
            } else if (txType == ChainParam.TxType.WCoinsType.ordinal()) {
                this.receiverPubkey = ByteUtil.stringToArrayList(entrylist.get(TxIndex.TxData.ordinal()).toString());
                this.amount = entrylist.get(TxIndex.TxData.ordinal()+ 1).integer();
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
     * sign transaction with sender prikey.
     * @param prikey
     * @return
     */
    public byte[] signTransaction(byte[] prikey) {
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
     * construct genesis msg K-V
     * @param ed25519pub
     * @param item
     * @return
     */
    public void appendAccount(String ed25519pub, GenesisItem item) {
        if(txType != ChainParam.TxType.GMsgType.ordinal()) {
            logger.error("Genesis msg transaction append error, tx type is {}", txType);
        } 
        /*
        if(!accountKV.containsKey(ed25519pub)){
            accountKV.put(ed25519pub,item);
        */
        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * get genesis msg K-V state.
     * @return
     */
    public HashMap<ByteArrayWrapper, GenesisItem> getAccountKV() {

        HashMap<ByteArrayWrapper, GenesisItem> accountKV = new HashMap<ByteArrayWrapper, GenesisItem>();

        Iterator<ArrayList<Long>> accountItor = this.genesisMsg.keySet().iterator();

        while(accountItor.hasNext()) {

            ArrayList<Long> keys = accountItor.next();
            ArrayList<Long> values = genesisMsg.get(keys);

            // key -> ByteArrayWrapper
            byte[] longbyte0 = ByteUtil.longToBytes(keys.get(0));
            byte[] longbyte1 = ByteUtil.longToBytes(keys.get(1));
            byte[] longbyte2 = ByteUtil.longToBytes(keys.get(2));
            byte[] longbyte3 = ByteUtil.longToBytes(keys.get(3));
            byte[] keybytes = new byte[ChainParam.PubkeyLength];
            System.arraycopy(longbyte0, 0, keybytes, 0, 8);
            System.arraycopy(longbyte1, 0, keybytes, 8, 8);
            System.arraycopy(longbyte2, 0, keybytes, 16, 8);
            System.arraycopy(longbyte3, 0, keybytes, 24, 8);
            ByteArrayWrapper key = new ByteArrayWrapper(keybytes);

            // value -> GenesisItem
            BigInteger balance = new BigInteger(values.get(0).toString());
            BigInteger power = new BigInteger(values.get(1).toString());
            GenesisItem value = new GenesisItem(balance, power);

            accountKV.put(key, value);
		}

        return accountKV;
    }

    /**
     * set forum note.
     * @return
     */
    public void setForumNote(String forumNote){
        if(txType != ChainParam.TxType.FNoteType.ordinal()) {
            logger.error("Forum note transaction set note error, tx type is {}", txType);
        } 
        this.forumNote = forumNote;
        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * get forum message.
     * @return
     */
    public String getForumNote(){
        if(txType != ChainParam.TxType.FNoteType.ordinal()) {
            logger.error("Forum note transaction get note error, tx type is {}", txType);
        } 
        if(!isParsed) parseEncodedBytes();
        return this.forumNote;
    }

    /**
     * set receiver pubkey.
     * @return
     */
    public void setReceiver(byte[] receiver){
        if(txType != ChainParam.TxType.WCoinsType.ordinal()) {
            logger.error("Forum note transaction set receiver error, tx type is {}", txType);
        } 
        this.receiverPubkey = ByteUtil.byteArrayToSignLongArray(receiver, ChainParam.PubkeyLongArrayLength);
        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * set wiring amount.
     * @return
     */
    public void setAmount(long amount){
        if(txType != ChainParam.TxType.WCoinsType.ordinal()) {
            logger.error("Forum note transaction set amount error, tx type is {}", txType);
        } 
        this.amount = amount;
        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * get receiver pubkey in transaction.
     * @return
     */
    public byte[] getReceiver(){
        if(txType != ChainParam.TxType.WCoinsType.ordinal()) {
            logger.error("Wiring transaction get pubkey error, tx type is {}", txType);
        } 
        if(!isParsed) parseEncodedBytes();

        byte[] longbyte0 = ByteUtil.longToBytes(receiverPubkey.get(0));
        byte[] longbyte1 = ByteUtil.longToBytes(receiverPubkey.get(1));
        byte[] longbyte2 = ByteUtil.longToBytes(receiverPubkey.get(2));
        byte[] longbyte3 = ByteUtil.longToBytes(receiverPubkey.get(3));

        byte[] receiver = new byte[ChainParam.PubkeyLength];
        System.arraycopy(longbyte0, 0, receiver, 0, 8);
        System.arraycopy(longbyte1, 0, receiver, 8, 8);
        System.arraycopy(longbyte2, 0, receiver, 16, 8);
        System.arraycopy(longbyte3, 0, receiver, 24, 8);

        return receiver;
    }

    /**
     * get wire amount.
     * @return
     */
    public long getAmount(){
        if(txType != ChainParam.TxType.WCoinsType.ordinal()) {
            logger.error("Wiring transaction get amount error, tx type is {}", txType);
        } 
        if(!isParsed) parseEncodedBytes();
        return this.amount;
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
           txHash = HashUtil.sha1hash(this.getEncodedBytes());
        }
        return txHash;
    }

}
