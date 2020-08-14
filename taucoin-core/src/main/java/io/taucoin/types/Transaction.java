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

import io.taucoin.genesis.GenesisItem;
import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.HashUtil;

import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Entry;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
    private String chainID;    //String encoding: UTF-8
    private long timestamp;
    private long txFee;
    private long txType;
    private ArrayList<Long> senderPubkey; //Pubkey - 32 bytes, 4 longs
    private long nonce;
    private ArrayList<Long> signature;    //Signature - 64 bytes, 8 longs
    /*
     * 1. jlibtorrent, libtorrent仅支持:String, Long, List, Map结构
     * 2. genesisMsg本质上需要记录多个账户(pubkey, item: balance + power)
     * 3. pubkey本质上是long list，但是jlibtorrent和libtorrent的map仅支持String类型
     * 4. 为了保证pubkey的本质，还是采用list来处理,一个账户共6个long
     * 5. 前4个long代表一个账户的pubkey，最后两个long代表一个账户的状态
     */
    private ArrayList<ArrayList<Long>> genesisMsg; // Genesis msg tx -> Tau QA
    private ArrayList<Long> forumNoteHash;    // Forum note tx - 20 bytes, 3 longs
    private ArrayList<Long> receiverPubkey;   // wiring coins tx, pubkey - 32 bytes, 4 longs
    private long amount;      // Wiring coins tx

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
     * @param forumNoteHash
     * @param receiver
     * @param amount
     * @param signature
     */
    public Transaction(long version, byte[] chainID, long timestamp, long txFee, long txType, byte[] sender, 
            long nonce, HashMap<ByteArrayWrapper, GenesisItem> genesisMsg, byte[] forumNoteHash, byte[] receiver, long amount, byte[] signature){
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
            this.forumNoteHash = ByteUtil.unAlignByteArrayToSignLongArray(forumNoteHash, ChainParam.HashLongArrayLength);
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
     * @param forumNoteHash
     * @param receiver
     * @param amount
     */
    public Transaction(long version, byte[] chainID, long timestamp, long txFee, long txType, byte[] sender, 
            long nonce, HashMap<ByteArrayWrapper, GenesisItem> genesisMsg, byte[] forumNoteHash, byte[] receiver, long amount){
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
        if(txType == ChainParam.TxType.GMsgType.ordinal()) {
            this.genesisMsg = genesisMapTrans(genesisMsg);
        } else if (txType == ChainParam.TxType.FNoteType.ordinal()) {
            this.forumNoteHash = ByteUtil.unAlignByteArrayToSignLongArray(forumNoteHash, ChainParam.HashLongArrayLength);
        } else if (txType == ChainParam.TxType.WCoinsType.ordinal()) {
            this.receiverPubkey = ByteUtil.byteArrayToSignLongArray(receiver, ChainParam.PubkeyLongArrayLength);
            this.amount = amount;
        }

        isParsed = true;
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
     * encoding transaction to bytes.
     * @return
     */
    public byte[] getEncoded() {
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
                list.add(this.forumNoteHash);
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
                list.add(this.forumNoteHash);
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
            list.add(this.forumNoteHash);
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
                this.forumNoteHash = ByteUtil.stringToArrayList(entrylist.get(TxIndex.TxData.ordinal()).toString());
            } else if (txType == ChainParam.TxType.WCoinsType.ordinal()) {
                this.receiverPubkey = ByteUtil.stringToArrayList(entrylist.get(TxIndex.TxData.ordinal()).toString());
                this.amount = entrylist.get(TxIndex.TxData.ordinal()+ 1).integer();
            }

            isParsed = true;
        }
    }

    /**
     * genesis Msg transform 
     * @param genesisMsg
     */
    private ArrayList<ArrayList<Long>> genesisMapTrans(HashMap<ByteArrayWrapper, GenesisItem> genesisMsg) {

        ArrayList<ArrayList<Long>> accounts = new ArrayList<>();
        Iterator<ByteArrayWrapper> accountItor = genesisMsg.keySet().iterator();

        while(accountItor.hasNext()) {

            ByteArrayWrapper key = accountItor.next();
            GenesisItem value = genesisMsg.get(key);

            // key -> arraylist
            ArrayList<Long> account = ByteUtil.byteArrayToSignLongArray(key.getData(), ChainParam.PubkeyLongArrayLength);

            account.add(value.getBalance().longValue());
            account.add(value.getPower().longValue());
            System.out.println(account);

            accounts.add(account);
		}

        return accounts;
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
    public void appendGenesisAccount(ByteArrayWrapper pubkey, GenesisItem item) {

        if(txType != ChainParam.TxType.GMsgType.ordinal()) {
            logger.error("Genesis msg transaction append error, tx type is {}", txType);
        } 

        ArrayList<Long> account = ByteUtil.byteArrayToSignLongArray(pubkey.getData(), ChainParam.PubkeyLongArrayLength);
        account.add(item.getBalance().longValue());
        account.add(item.getPower().longValue());
        this.genesisMsg.add(account);

        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * get genesis msg K-V state.
     * @return
     */
    public HashMap<ByteArrayWrapper, GenesisItem> getGenesisAccounts() {

        HashMap<ByteArrayWrapper, GenesisItem> accounts = new HashMap<ByteArrayWrapper, GenesisItem>();

        for(ArrayList<Long> account: this.genesisMsg) {

            // key -> ByteArrayWrapper
            byte[] longbyte0 = ByteUtil.longToBytes(account.get(0));
            byte[] longbyte1 = ByteUtil.longToBytes(account.get(1));
            byte[] longbyte2 = ByteUtil.longToBytes(account.get(2));
            byte[] longbyte3 = ByteUtil.longToBytes(account.get(3));
            byte[] keybytes = new byte[ChainParam.PubkeyLength];
            System.arraycopy(longbyte0, 0, keybytes, 0, 8);
            System.arraycopy(longbyte1, 0, keybytes, 8, 8);
            System.arraycopy(longbyte2, 0, keybytes, 16, 8);
            System.arraycopy(longbyte3, 0, keybytes, 24, 8);
            ByteArrayWrapper key = new ByteArrayWrapper(keybytes);

            // value -> GenesisItem
            BigInteger balance = new BigInteger(account.get(4).toString());
            BigInteger power = new BigInteger(account.get(5).toString());
            GenesisItem value = new GenesisItem(balance, power);

            accounts.put(key, value);
		}

        return accounts;
    }

    /**
     * set forum note.
     * @return
     */
    public void setForumNoteHash(byte[] forumNoteHash){
        if(txType != ChainParam.TxType.FNoteType.ordinal()) {
            logger.error("Forum note transaction set note error, tx type is {}", txType);
        } 
        this.forumNoteHash = ByteUtil.unAlignByteArrayToSignLongArray(forumNoteHash, ChainParam.HashLongArrayLength);
        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * get forum message.
     * @return
     */
    public byte[] getForumNoteHash(){
        if(txType != ChainParam.TxType.FNoteType.ordinal()) {
            logger.error("Forum note transaction get note error, tx type is {}", txType);
        } 
        if(!isParsed) parseEncodedBytes();
        byte[] longbyte0 = ByteUtil.longToBytes(forumNoteHash.get(0));
        byte[] longbyte1 = ByteUtil.longToBytes(forumNoteHash.get(1));
        byte[] longbyte2 = ByteUtil.keep4bytesOfLong(forumNoteHash.get(2));
        byte[] fnHash = new byte[ChainParam.HashLength];
        System.arraycopy(longbyte0, 0, fnHash, 0, 8);
        System.arraycopy(longbyte1, 0, fnHash, 8, 8);
        System.arraycopy(longbyte2, 0, fnHash, 16, 4);
        return fnHash;
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
           txHash = HashUtil.sha1hash(this.getEncoded());
        }
        return txHash;
    }

    @Override
    public String toString(){
        StringBuilder strTx = new StringBuilder();
        strTx.append("transaction: [\n");
        strTx.append("version: ").append(this.getVersion()).append("\n");
        strTx.append("chainID: ").append(new String(this.getChainID())).append("\n");
        strTx.append("timestamp: ").append(this.getTimeStamp()).append("\n");
        strTx.append("txFee: ").append(this.getTxFee()).append("\n");
        strTx.append("txType: ").append(this.getTxType()).append("\n");
        strTx.append("senderpubkey: ").append(ByteUtil.toHexString(this.getSenderPubkey())    ).append("\n");
        strTx.append("nonce: ").append(this.getNonce()).append("\n");
        if(this.txType == ChainParam.TxType.GMsgType.ordinal()) {
            //TODO
        } else if (this.txType == ChainParam.TxType.FNoteType.ordinal()) {
            //TODO
        } else if (this.txType == ChainParam.TxType.WCoinsType.ordinal()) {
            //TODO
        }
        strTx.append("signature: ").append(ByteUtil.toHexString(this.getSignature())).append("\n");
        strTx.append("]\n");
        return strTx.toString();
    }

}
