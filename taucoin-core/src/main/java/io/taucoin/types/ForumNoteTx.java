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

import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Entry;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ForumNoteTx extends Transaction {

    private static final Logger logger = LoggerFactory.getLogger("ForumNoteTx");

    private ArrayList<Long> forumNoteHash;    // Forum note tx - 20 bytes, 3 longs

    /**
     * construct complete tx with signature.
     * @param version
     * @param chainID
     * @param timestamp
     * @param txFee
     * @param txType
     * @param sender
     * @param nonce
     * @param forumNoteHash
     * @param signature
     */
    public ForumNoteTx(long version, byte[] chainID, long timestamp, long txFee, long txType, byte[] sender, 
            long nonce, byte[] forumNoteHash, byte[] signature){

        //父类构造函数
        super(version, chainID, timestamp, txFee, txType, sender, nonce, signature);

        this.forumNoteHash = ByteUtil.unAlignByteArrayToSignLongArray(forumNoteHash, ChainParam.HashLongArrayLength);

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
     * @param forumNoteHash
     */
    public ForumNoteTx(long version, byte[] chainID, long timestamp, long txFee, long txType, byte[] sender,
            long nonce, byte[] forumNoteHash){

        //父类构造函数
        super(version, chainID, timestamp, txFee, txType, sender, nonce);

        this.forumNoteHash = ByteUtil.unAlignByteArrayToSignLongArray(forumNoteHash, ChainParam.HashLongArrayLength);

        isParsed = true;
    }

    /**
     * construct transaction from complete byte encoding.
     * @param encodedBytes:complete byte encoding.
     */
    public ForumNoteTx(byte[] encodedBytes) {
        super(encodedBytes);
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
            list.add(this.forumNoteHash);
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
            list.add(this.forumNoteHash);
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
        list.add(this.forumNoteHash);
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
            this.forumNoteHash = ByteUtil.stringToLongArrayList(entrylist.get(TxIndex.TxData.ordinal()).toString());

            isParsed = true;
        }
    }

    /**
     * set forum note.
     * @return
     */
    public void setForumNoteHash(byte[] forumNoteHash){
        if(txType != TypesConfig.TxType.FNoteType.ordinal()) {
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
    public byte[] getForumNoteHashCowTC(){
        if(txType != TypesConfig.TxType.FNoteType.ordinal()) {
            logger.error("Forum note transaction get note error, tx type is {}", txType);
        } 
        if(!isParsed) parseEncodedBytes();
        return ByteUtil.longArrayToBytes(forumNoteHash, ChainParam.HashLength);
    }

    /**
     * get forum message.
     * @return
     */
    public byte[] getForumNoteHash(){
        if(txType != TypesConfig.TxType.FNoteType.ordinal()) {
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
        strTx.append("forumNoteHash: ").append(this.getNonce()).append("\n");
        strTx.append("signature: ").append(ByteUtil.toHexString(this.getSignature())).append("\n");
        strTx.append("]\n");
        return strTx.toString();
    }
}
