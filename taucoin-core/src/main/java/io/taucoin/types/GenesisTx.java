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
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

import java.math.BigInteger;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

public class GenesisTx extends Transaction {

    private static final Logger logger = LoggerFactory.getLogger("GenesisTx");

    private ArrayList<GenesisItem> genesisMsg;

    /**
     * construct complete tx with signature.
     * @param version
     * @param chainID
     * @param timestamp
     * @param txFee
     * @param sender
     * @param nonce
     * @param genesisMsg
     * @param signature
     */
    public GenesisTx(long version, byte[] chainID, long timestamp, BigInteger txFee, byte[] sender,
            BigInteger nonce, ArrayList<GenesisItem> genesisMsg, byte[] signature){

        //父类构造函数
        super(version, timestamp, chainID, txFee, TypesConfig.TxType.GenesisType.ordinal(), sender, nonce, signature);

        //this.genesisMsg = genesisMapTrans(genesisMsg);
        this.genesisMsg = genesisMsg;

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
     * @param genesisMsg
     */
    public GenesisTx(long version, byte[] chainID, long timestamp, BigInteger txFee, byte[] sender,
            BigInteger nonce, ArrayList<GenesisItem> genesisMsg){

        //父类构造函数
        super(version, timestamp, chainID, txFee, TypesConfig.TxType.GenesisType.ordinal(), sender, nonce);

        //this.genesisMsg = genesisMapTrans(genesisMsg);
        this.genesisMsg = genesisMsg;

        isParsed = true;
    }

    /**
     * construct transaction from complete byte encoding.
     * @param encodedBytes:complete byte encoding.
     */
    public GenesisTx(byte[] encodedBytes) {
        super(encodedBytes);
    }

    /**
     * encoding transaction to bytes.
     * @return
     */
    @Override
    public byte[] getEncoded() {

        if(this.encodedBytes == null) {

            byte[] version = RLP.encodeElement(ByteUtil.longToBytes(this.version));
            byte[] timestamp = RLP.encodeElement(ByteUtil.longToBytes(this.timestamp));
            byte[] chainID = RLP.encodeElement(this.chainID);

            byte[] txFee = RLP.encodeBigInteger(this.txFee);
            byte[] txType = RLP.encodeElement(ByteUtil.longToBytes(this.txType));

            byte[] senderPubkey = RLP.encodeElement(this.senderPubkey);
            byte[] nonce = RLP.encodeBigInteger(this.nonce);
            byte[] signature = RLP.encodeElement(this.signature);

            byte[] genesisMsg = rlpEncodedGM(this.genesisMsg);

            this.encodedBytes = RLP.encodeList(version, timestamp, chainID,
                                txFee, txType,
                                senderPubkey, nonce, signature,
                                genesisMsg);
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

            byte[] genesisMsg = rlpEncodedGM(this.genesisMsg);

            this.sigEncodedBytes = RLP.encodeList(version, timestamp, chainID,
                                   txFee, txType,
                                   senderPubkey, nonce,
                                   genesisMsg);
        }

        return sigEncodedBytes;

    }

    /**
     * rlp encode genesisMsg
     *
     */

    public static byte[] rlpEncodedGM(ArrayList<GenesisItem> genesisMsg) {

        byte[][] rlpEncodeGMList = new byte[genesisMsg.size()][];

        int index = 0;

        while(index < genesisMsg.size()) {
            rlpEncodeGMList[index] = genesisMsg.get(index).getEncoded();
            index++;
		}

        return RLP.encode(rlpEncodeGMList);
    }

    /**
     * rlp decode genesisMsg
     *
     */

    public static ArrayList<GenesisItem> rlpDecodedGM(byte[] genesisMsgBytes) {

        ArrayList<GenesisItem> genesisMsg =  new ArrayList<>();

        RLPList gmList0 = RLP.decode2(genesisMsgBytes);
        RLPList gmList1 = (RLPList) gmList0.get(0);

        for(int i = 0; i < gmList1.size(); i++) {
            genesisMsg.add(new GenesisItem(gmList1.get(i).getRLPData()));
        }

        return genesisMsg;
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
            RLPList genesisTx = (RLPList) txList.get(0);

            this.version = ByteUtil.byteArrayToLong(genesisTx.get(TxIndex.Version.ordinal()).getRLPData());
            this.timestamp = ByteUtil.byteArrayToLong(genesisTx.get(TxIndex.Timestamp.ordinal()).getRLPData());
            this.chainID = genesisTx.get(TxIndex.ChainID.ordinal()).getRLPData();

            this.txFee = genesisTx.get(TxIndex.TxFee.ordinal()).getRLPData() == null ? BigInteger.ZERO
                    : new BigInteger(1, genesisTx.get(TxIndex.TxFee.ordinal()).getRLPData());
            this.txType = ByteUtil.byteArrayToLong(genesisTx.get(TxIndex.TxType.ordinal()).getRLPData());
            this.senderPubkey = genesisTx.get(TxIndex.Sender.ordinal()).getRLPData();
            this.nonce = genesisTx.get(TxIndex.Nonce.ordinal()).getRLPData() == null ? BigInteger.ZERO
                    : new BigInteger(1, genesisTx.get(TxIndex.Nonce.ordinal()).getRLPData());

            this.signature = genesisTx.get(TxIndex.Signature.ordinal()).getRLPData();
            this.genesisMsg = rlpDecodedGM(genesisTx.get(TxIndex.TxData.ordinal()).getRLPData());

            isParsed = true;
        }
    }

    /**
     * construct genesis msg K-V
     * @param item
     * @return
     */
    public void appendGenesisAccount(GenesisItem item) {

        if(txType != TypesConfig.TxType.GenesisType.ordinal()) {
            logger.error("Genesis msg transaction append error, tx type is {}", txType);
        }

        this.genesisMsg.add(item);

        encodedBytes = null;
        sigEncodedBytes = null;
    }

    /**
     * get genesis msg K-V state.
     * @return
     */
    public ArrayList<GenesisItem> getGenesisAccounts() {
        return  this.genesisMsg;
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

        int index = 0;
        while(index < this.genesisMsg.size()) {
            strTx.append(" account: ").append(ByteUtil.toHexString(this.genesisMsg.get(index).getAccount()));
            strTx.append(" balance: ").append(this.genesisMsg.get(index).getBalance().longValue());
            strTx.append(" power: ").append(this.genesisMsg.get(index).getPower().longValue());
		}

        strTx.append(" signature: ").append(ByteUtil.toHexString(this.getSignature()));
        strTx.append("]");

        return strTx.toString();
    }

}
