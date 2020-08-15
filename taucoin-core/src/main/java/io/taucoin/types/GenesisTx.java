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

import com.frostwire.jlibtorrent.Entry;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GenesisTx extends Transaction {

    private static final Logger logger = LoggerFactory.getLogger("GenesisTx");

    /*
     * 1. jlibtorrent, libtorrent仅支持:String, Long, List, Map结构
     * 2. genesisMsg本质上需要记录多个账户(pubkey, item: balance + power)
     * 3. pubkey本质上是long list，但是jlibtorrent和libtorrent的map仅支持String类型
     * 4. 为了保证pubkey的本质，还是采用list来处理,一个账户共6个long
     * 5. 前4个long代表一个账户的pubkey，最后两个long代表一个账户的状态
     */
    private ArrayList<ArrayList<Long>> genesisMsg; // Genesis msg tx -> Tau QA

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
     * @param signature
     */
    public GenesisTx(long version, byte[] chainID, long timestamp, long txFee, long txType, byte[] sender, 
            long nonce, HashMap<ByteArrayWrapper, GenesisItem> genesisMsg, byte[] signature){

        //父类构造函数
        super(version, chainID, timestamp, txFee, txType, sender, nonce, signature);

        this.genesisMsg = genesisMapTrans(genesisMsg);

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
     */
    public GenesisTx(long version, byte[] chainID, long timestamp, long txFee, long txType, byte[] sender, 
            long nonce, HashMap<ByteArrayWrapper, GenesisItem> genesisMsg){

        //父类构造函数
        super(version, chainID, timestamp, txFee, txType, sender, nonce);

        this.genesisMsg = genesisMapTrans(genesisMsg);

        isParsed = true;
    }

    /**
     * construct transaction from complete byte encoding.
     * @param encodedBytes:complete byte encoding.
     */
    public GenesisTx(byte[] encodedBytes) {
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
            list.add(this.genesisMsg);
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
            list.add(this.genesisMsg);
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
        list.add(this.genesisMsg);
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
            //TODO, string -> ArrayList<ArrayList<Long>>
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

            accounts.add(account);
		}

        return accounts;
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
    public HashMap<ByteArrayWrapper, GenesisItem> getGenesisAccountsCowTC() {

        HashMap<ByteArrayWrapper, GenesisItem> accounts = new HashMap<ByteArrayWrapper, GenesisItem>();

        for(ArrayList<Long> account: this.genesisMsg) {

            // key -> ByteArrayWrapper
            byte[] keybytes = ByteUtil.longArrayToBytes(account, ChainParam.PubkeyLength);
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
        HashMap<ByteArrayWrapper, GenesisItem> accounts = getGenesisAccounts();
        Iterator<ByteArrayWrapper> accountItor = accounts.keySet().iterator();
        while(accountItor.hasNext()) {
            ByteArrayWrapper key = accountItor.next();
            GenesisItem value = accounts.get(key);
            strTx.append("account: ").append(ByteUtil.toHexString(key.getData())).append("\n");
            strTx.append("balance: ").append(value.getBalance().longValue());
            strTx.append("power: ").append(value.getPower().longValue()).append("\n");
		}
        strTx.append("signature: ").append(ByteUtil.toHexString(this.getSignature())).append("\n");
        strTx.append("]\n");
        return strTx.toString();
    }

}
