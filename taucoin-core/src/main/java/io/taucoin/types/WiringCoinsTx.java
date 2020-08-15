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

import com.frostwire.jlibtorrent.Entry;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public class WiringCoinsTx extends Transaction {

    private static final Logger logger = LoggerFactory.getLogger("WiringCoinsTx");

    private ArrayList<Long> receiverPubkey;   // wiring coins tx, pubkey - 32 bytes, 4 longs
    private long amount;      // Wiring coins tx

    /**
     * construct complete tx with signature.
     * @param version
     * @param chainID
     * @param timestamp
     * @param txFee
     * @param txType
     * @param sender
     * @param nonce
     * @param receiver
     * @param amount
     * @param signature
     */
    public WiringCoinsTx(long version, byte[] chainID, long timestamp, long txFee, long txType,
                        byte[] sender, long nonce, byte[] receiver, long amount, byte[] signature) {
        if(sender.length != ChainParam.SenderLength) {
            throw new IllegalArgumentException("Sender should be : " + ChainParam.SenderLength + " bytes");
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
        this.receiverPubkey = ByteUtil.byteArrayToSignLongArray(receiver, ChainParam.PubkeyLongArrayLength);
        this.amount = amount;

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
     * @param receiver
     * @param amount
     */
    public WiringCoinsTx(long version, byte[] chainID, long timestamp, long txFee, long txType,
                         byte[] sender, long nonce, byte[] receiver, long amount) {
        if(sender.length != ChainParam.SenderLength) {
            throw new IllegalArgumentException("Sender address should be : " + ChainParam.SenderLength + " bytes");
        }
        this.version = version;
        this.chainID = new String(chainID);
        this.timestamp = timestamp;
        this.txFee = txFee;
        this.txType = txType;
        this.senderPubkey = ByteUtil.byteArrayToSignLongArray(sender, ChainParam.PubkeyLongArrayLength);
        this.nonce = nonce;
        this.receiverPubkey = ByteUtil.byteArrayToSignLongArray(receiver, ChainParam.PubkeyLongArrayLength);
        this.amount = amount;

        isParsed = true;
    }

    /**
     * construct transaction from complete byte encoding.
     * @param encodedBytes:complete byte encoding.
     */
    public WiringCoinsTx(byte[] encodedBytes) {
        this.encodedBytes = encodedBytes;
        this.isParsed = false;
    }

    /**
     * encoding transaction to bytes.
     * @return
     */
    @Override
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
            list.add(this.receiverPubkey);
            list.add(this.amount);
            Entry entry = Entry.fromList(list);
            this.encodedBytes = entry.bencode();
        }
        return this.encodedBytes;
    }

    /**
     * encoding transaction signature parts which is under protection of cryptographic signature.
     * @return
     */
    @Override
    public byte[] getSigEncodedBytes() {
        if(sigEncodedBytes == null) {
            List list = new ArrayList();
            list.add(this.version);
            list.add(this.chainID);
            list.add(this.timestamp);
            list.add(this.txFee);
            list.add(this.senderPubkey);
            list.add(this.nonce);
            list.add(this.receiverPubkey);
            list.add(this.amount);
            Entry entry = Entry.fromList(list);
            this.sigEncodedBytes = entry.bencode();
        }
        return sigEncodedBytes;
    }

    /**
     * encoding transaction to long[].
     * @return
     */
    @Override
    public List getTxLongArray() {
        List list = new ArrayList();
        list.add(this.version);
        list.add(this.chainID);
        list.add(this.timestamp);
        list.add(this.txFee);
        list.add(this.senderPubkey);
        list.add(this.nonce);
        list.add(this.signature);
        list.add(this.receiverPubkey);
        list.add(this.amount);
        return list;
    }

    /**
     * parse transaction bytes field to flat block field.
     */
    @Override
    public void parseEncodedBytes(){
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
            this.senderPubkey = ByteUtil.stringToLongArrayList(entrylist.get(TxIndex.Sender.ordinal()).toString());
            this.nonce = entrylist.get(TxIndex.Nonce.ordinal()).integer();
            this.signature = ByteUtil.stringToLongArrayList(entrylist.get(TxIndex.Signature.ordinal()).toString());
            this.receiverPubkey = ByteUtil.stringToLongArrayList(entrylist.get(TxIndex.TxData.ordinal()).toString());
            this.amount = entrylist.get(TxIndex.TxData.ordinal()+ 1).integer();
            isParsed = true;
        }
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
    public byte[] getReceiverCowTC(){
        if(txType != ChainParam.TxType.WCoinsType.ordinal()) {
            logger.error("Wiring transaction get pubkey error, tx type is {}", txType);
        } 
        if(!isParsed) parseEncodedBytes();

        return ByteUtil.longArrayToBytes(receiverPubkey, ChainParam.PubkeyLength);
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

    @Override
    public String toString(){
        StringBuilder strTx = new StringBuilder();
        strTx.append("transaction: [\n");
        strTx.append("version: ").append(this.getVersion()).append("\n");
        strTx.append("chainID: ").append(new String(this.getChainID())).append("\n");
        strTx.append("timestamp: ").append(this.getTimeStamp()).append("\n");
        strTx.append("txFee: ").append(this.getTxFee()).append("\n");
        strTx.append("txType: ").append(this.getTxType()).append("\n");
        strTx.append("sender: ").append(ByteUtil.toHexString(this.getSenderPubkey())).append("\n");
        strTx.append("nonce: ").append(this.getNonce()).append("\n");
        strTx.append("receiver: ").append(ByteUtil.toHexString(this.getReceiver())).append("\n");
        strTx.append("amount: ").append(this.getAmount()).append("\n");
        strTx.append("signature: ").append(ByteUtil.toHexString(this.getSignature())).append("\n");
        strTx.append("]\n");
        return strTx.toString();
    }
}
