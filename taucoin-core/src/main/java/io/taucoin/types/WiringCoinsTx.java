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

public class WiringCoinsTx extends Transaction {

    private static final Logger logger = LoggerFactory.getLogger("WiringCoinsTx");

    private byte[] receiverPubkey;  // wiring coins tx, pubkey - 32 bytes
    private BigInteger amount;      // Wiring coins tx
    private byte[] memo;            // Wiring coins tx

    /**
     * construct complete tx with signature.
     * @param version
     * @param chainID
     * @param timestamp
     * @param txFee
     * @param sender
     * @param nonce
     * @param receiver
     * @param amount
     * @param memo
     * @param signature
     */
    public WiringCoinsTx(long version, byte[] chainID, long timestamp, BigInteger txFee,
                        byte[] sender, BigInteger nonce, byte[] receiver, BigInteger amount, byte[] memo, byte[] signature) {

        //父类构造函数
        super(version, timestamp, chainID, txFee, TypesConfig.TxType.WCoinsType.ordinal(), sender, nonce, signature);

        //Check
        if(receiver.length != ChainParam.SenderLength) {
            throw new IllegalArgumentException("Receiver should be : " + ChainParam.SenderLength + " bytes");
        }

        this.receiverPubkey = receiver;
        this.amount = amount;
        this.memo = memo;

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
     * @param receiver
     * @param amount
     * @param memo
     */
    public WiringCoinsTx(long version, byte[] chainID, long timestamp, BigInteger txFee,
                         byte[] sender, BigInteger nonce, byte[] receiver, BigInteger amount, byte[] memo) {

        //父类构造函数
        super(version, timestamp, chainID, txFee, TypesConfig.TxType.WCoinsType.ordinal(), sender, nonce);

        //Check
        if(receiver.length != ChainParam.SenderLength) {
            throw new IllegalArgumentException("Receiver should be : " + ChainParam.SenderLength + " bytes");
        }

        this.receiverPubkey = receiver;
        this.amount = amount;
        this.memo = memo;

        isParsed = true;
    }

    /**
     * construct transaction from complete byte encoding.
     * @param encodedBytes:complete byte encoding.
     */
    public WiringCoinsTx(byte[] encodedBytes) {
        //父类构造函数
        super(encodedBytes);
    }

    /**
     * encoding transaction to bytes.
     * @return
     */
    @Override
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

            byte[] recevierPubkey = RLP.encodeElement(this.receiverPubkey);
            byte[] amount = RLP.encodeBigInteger(this.amount);
            byte[] memo = RLP.encodeElement(this.memo);

            this.encodedBytes = RLP.encodeList(version, timestamp, chainID,
                    txFee, txType,
                    senderPubkey, nonce, signature,
                    recevierPubkey, amount, memo);
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

            byte[] version = RLP.encodeElement(ByteUtil.longToBytes(this.version));
            byte[] timestamp = RLP.encodeElement(ByteUtil.longToBytes(this.timestamp));
            byte[] chainID = RLP.encodeElement(this.chainID);

            byte[] txFee = RLP.encodeBigInteger(this.txFee);
            byte[] txType = RLP.encodeElement(ByteUtil.longToBytes(this.txType));

            byte[] senderPubkey = RLP.encodeElement(this.senderPubkey);
            byte[] nonce = RLP.encodeBigInteger(this.nonce);

            byte[] recevierPubkey = RLP.encodeElement(this.receiverPubkey);
            byte[] amount = RLP.encodeBigInteger(this.amount);
            byte[] memo = RLP.encodeElement(this.memo);

            this.sigEncodedBytes = RLP.encodeList(version, timestamp, chainID,
                    txFee, txType,
                    senderPubkey, nonce,
                    recevierPubkey, amount, memo);
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
            RLPList wiringCoinsTx = (RLPList) txList.get(0);

            this.version = ByteUtil.byteArrayToLong(wiringCoinsTx.get(TxIndex.Version.ordinal()).getRLPData());
            this.timestamp = ByteUtil.byteArrayToLong(wiringCoinsTx.get(TxIndex.Timestamp.ordinal()).getRLPData());
            this.chainID = wiringCoinsTx.get(TxIndex.ChainID.ordinal()).getRLPData();

            this.txFee = wiringCoinsTx.get(TxIndex.TxFee.ordinal()).getRLPData() == null ? BigInteger.ZERO
                    : new BigInteger(1, wiringCoinsTx.get(TxIndex.TxFee.ordinal()).getRLPData());
            this.txType = ByteUtil.byteArrayToLong(wiringCoinsTx.get(TxIndex.TxType.ordinal()).getRLPData());

            this.senderPubkey = wiringCoinsTx.get(TxIndex.Sender.ordinal()).getRLPData();
            this.nonce = wiringCoinsTx.get(TxIndex.Nonce.ordinal()).getRLPData() == null ? BigInteger.ZERO
                    : new BigInteger(1, wiringCoinsTx.get(TxIndex.Nonce.ordinal()).getRLPData());

            this.signature = wiringCoinsTx.get(TxIndex.Signature.ordinal()).getRLPData();

            this.receiverPubkey = wiringCoinsTx.get(TxIndex.TxData.ordinal()).getRLPData();
            this.amount = new BigInteger(1, wiringCoinsTx.get(TxIndex.TxData.ordinal() + 1).getRLPData());
            this.memo = wiringCoinsTx.get(TxIndex.TxData.ordinal() + 2).getRLPData();

            isParsed = true;
        }
    }

    /**
     * set receiver pubkey.
     * @return
     */
    public void setReceiver(byte[] receiver){
        if(txType != TypesConfig.TxType.WCoinsType.ordinal()) {
            logger.error("Forum note transaction set receiver error, tx type is {}", txType);
        }
        //Check
        if(receiver.length != ChainParam.SenderLength) {
            throw new IllegalArgumentException("Receiver should be : " + ChainParam.SenderLength + " bytes");
        }
        this.receiverPubkey = receiver;
        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * set wiring amount.
     * @return
     */
    public void setAmount(BigInteger amount){
        if(txType != TypesConfig.TxType.WCoinsType.ordinal()) {
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
        if(txType != TypesConfig.TxType.WCoinsType.ordinal()) {
            logger.error("Wiring transaction get pubkey error, tx type is {}", txType);
        } 
        if(!isParsed) parseEncodedBytes();

        return this.receiverPubkey;
    }

    /**
     * get wire amount.
     * @return
     */
    public BigInteger getAmount(){
        if(txType != TypesConfig.TxType.WCoinsType.ordinal()) {
            logger.error("Wiring transaction get amount error, tx type is {}", txType);
        } 
        if(!isParsed) parseEncodedBytes();
        return this.amount;
    }

    /**
     * get wire memo.
     * @return
     */
    public byte[] getMemo(){
        if(txType != TypesConfig.TxType.WCoinsType.ordinal()) {
            logger.error("Wiring transaction get memo error, tx type is {}", txType);
        } 
        if(!isParsed) parseEncodedBytes();
        return this.memo;
    }

    @Override
    public String toString() {

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

        strTx.append(" Receiver: ").append(ByteUtil.toHexString(this.getReceiver()));
        strTx.append(" Amount: ").append(this.getAmount());
        strTx.append(" Memo: ").append(this.getMemo());

        strTx.append(" Signature: ").append(ByteUtil.toHexString(this.getSignature()));
        strTx.append("]");

        return strTx.toString();

    }
}
