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
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForumNoteTx extends Transaction {

    private static final Logger logger = LoggerFactory.getLogger("ForumNoteTx");

    private byte[] forumNoteHash;    // Forum note tx - 20 bytes

    /**
     * construct complete tx with signature.
     * @param version
     * @param chainID
     * @param timestamp
     * @param txFee
     * @param sender
     * @param nonce
     * @param forumNoteHash
     * @param signature
     */
    public ForumNoteTx(long version, byte[] chainID, long timestamp, BigInteger txFee, byte[] sender,
                       BigInteger nonce, byte[] forumNoteHash, byte[] signature){

        //父类构造函数
        super(version, timestamp, chainID, txFee, TypesConfig.TxType.FNoteType.ordinal(), sender, nonce, signature);

        //Check
        if(forumNoteHash.length != ChainParam.HashLength) {
            throw new IllegalArgumentException("ForumNoteHashshould be : "+ChainParam.HashLength + " bytes");
        }

        this.forumNoteHash = forumNoteHash;

        isParsed = true;
    }

    /**
     * construct complete tx without signature.
     * @param version
     * @param chainID
     * @param timestamp
     * @param txFee
     * @param sender
     * @param nonce
     * @param forumNoteHash
     */
    public ForumNoteTx(long version, byte[] chainID, long timestamp, BigInteger txFee, byte[] sender,
                       BigInteger nonce, byte[] forumNoteHash){

        //父类构造函数
        super(version, timestamp, chainID, txFee, TypesConfig.TxType.FNoteType.ordinal(), sender, nonce);

        //Check
        if(forumNoteHash.length != ChainParam.HashLength) {
            throw new IllegalArgumentException("ForumNoteHashshould be : "+ChainParam.HashLength + " bytes");
        }

        this.forumNoteHash = forumNoteHash;

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

            byte[] version = RLP.encodeElement(ByteUtil.longToBytes(this.version));
            byte[] timestamp = RLP.encodeElement(ByteUtil.longToBytes(this.timestamp));
            byte[] chainID = RLP.encodeElement(this.chainID);

            byte[] txFee = RLP.encodeBigInteger(this.txFee);
            byte[] txType = RLP.encodeElement(ByteUtil.longToBytes(this.txType));

            byte[] senderPubkey = RLP.encodeElement(this.senderPubkey);
            byte[] nonce = RLP.encodeBigInteger(this.nonce);
            byte[] signature = RLP.encodeElement(this.signature);

            byte[] forumNoteHash = RLP.encodeElement(this.forumNoteHash);

            this.encodedBytes = RLP.encodeList(version, timestamp, chainID,
                    txFee, txType,
                    senderPubkey, nonce, signature,
                    forumNoteHash);
        }

        return this.encodedBytes;
    }

    /**
     * encoding transaction signature parts which is under protection of cryptographic signature.
     * @return
     */
    public byte[] getSigEncodedBytes() {

        if(sigEncodedBytes == null) {

            byte[] version = RLP.encodeElement(ByteUtil.longToBytes(this.version));
            byte[] timestamp = RLP.encodeElement(ByteUtil.longToBytes(this.timestamp));
            byte[] chainID = RLP.encodeElement(this.chainID);

            byte[] txFee = RLP.encodeBigInteger(this.txFee);
            byte[] txType = RLP.encodeElement(ByteUtil.longToBytes(this.txType));

            byte[] senderPubkey = RLP.encodeElement(this.senderPubkey);
            byte[] nonce = RLP.encodeBigInteger(this.nonce);

            byte[] forumNoteHash = RLP.encodeElement(this.forumNoteHash);

            this.sigEncodedBytes = RLP.encodeList(version, timestamp, chainID,
                    txFee, txType,
                    senderPubkey, nonce,
                    forumNoteHash);

        }

        return sigEncodedBytes;
    }

    /**
     * parse transaction bytes field to flat block field.
     */
    @Override
    public void parseEncodedBytes(){
        if(isParsed) {
            return;
        } else {
            RLPList txList = RLP.decode2(this.encodedBytes);
            RLPList forumNoteTx = (RLPList) txList.get(0);

            this.version = ByteUtil.byteArrayToLong(forumNoteTx.get(TxIndex.Version.ordinal()).getRLPData());
            this.timestamp = ByteUtil.byteArrayToLong(forumNoteTx.get(TxIndex.Timestamp.ordinal()).getRLPData());
            this.chainID = forumNoteTx.get(TxIndex.ChainID.ordinal()).getRLPData();

            this.txFee = forumNoteTx.get(TxIndex.TxFee.ordinal()).getRLPData() == null ? BigInteger.ZERO
                    : new BigInteger(1, forumNoteTx.get(TxIndex.TxFee.ordinal()).getRLPData());
            this.txType = ByteUtil.byteArrayToLong(forumNoteTx.get(TxIndex.TxType.ordinal()).getRLPData());

            this.senderPubkey = forumNoteTx.get(TxIndex.Sender.ordinal()).getRLPData();
            this.nonce = forumNoteTx.get(TxIndex.Nonce.ordinal()).getRLPData() == null ? BigInteger.ZERO
                    : new BigInteger(1, forumNoteTx.get(TxIndex.Nonce.ordinal()).getRLPData());

            this.signature = forumNoteTx.get(TxIndex.Signature.ordinal()).getRLPData();

            this.forumNoteHash = forumNoteTx.get(TxIndex.TxData.ordinal()).getRLPData();

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
        //Check
        if(forumNoteHash.length != ChainParam.HashLength) {
            throw new IllegalArgumentException("ForumNoteHashshould be : "+ChainParam.HashLength + " bytes");
        }

        this.forumNoteHash = forumNoteHash;
        encodedBytes = null;
        sigEncodedBytes = null;
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
        return this.forumNoteHash;
    }

    @Override
    public String toString(){

        StringBuilder strTx = new StringBuilder();

        strTx.append("Transaction: [");

        strTx.append(" Txhash: ").append(ByteUtil.toHexString(this.getTxID()));

        strTx.append(" Version: ").append(this.getVersion());
        strTx.append(" Timestamp: ").append(this.getTimeStamp());
        strTx.append(" ChainID: ").append(ByteUtil.toHexString(this.getChainID()));

        strTx.append(" TxFee: ").append(this.getTxFee());
        strTx.append(" TxType: ").append(this.getTxType());

        strTx.append(" Sender: ").append(ByteUtil.toHexString(this.getSenderPubkey()));
        strTx.append(" Nonce: ").append(this.getNonce());

        strTx.append(" ForumNoteHash: ").append(this.getNonce());

        strTx.append(" Signature: ").append(ByteUtil.toHexString(this.getSignature()));
        strTx.append("]");

        return strTx.toString();

    }
}
