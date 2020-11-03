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
import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Pair;

import io.taucoin.genesis.GenesisItem;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;

import io.taucoin.util.ByteUtil;
import io.taucoin.util.HashUtil;

public class TransactionTest {
    private static final long version = 1;
    private static final byte[] chainid = "TAUcoin#c84b1332519aa8020e48438eb3caa9b482798c9d".getBytes();
    private static final long timestamp = 1597998963;
    private static final BigInteger txFee = BigInteger.valueOf(210L);
    private static final BigInteger nonce = BigInteger.valueOf(10000000L);

    private  static final byte[] seed;
    private  static final byte[] sender;

    static {
        seed = Ed25519.createSeed();
        Pair<byte[], byte[]> keys = Ed25519.createKeypair(seed);
        sender = keys.first;
    }

    @Test
    public void createGenesisTx(){

        BigInteger abalance = BigInteger.valueOf(10000000L);
        BigInteger anonce = BigInteger.valueOf(1000000L);
        GenesisItem item = new GenesisItem(sender, abalance, anonce);

        ArrayList<GenesisItem> genesisMsg = new ArrayList<>();
        genesisMsg.add(item);

        GenesisTx tx = new GenesisTx(version, chainid, timestamp, txFee, sender, nonce, genesisMsg);
        tx.signTransactionWithSeed(seed);

        System.out.println("verison: " + tx.getVersion());
        System.out.println("chainid: " + new String(tx.getChainID()));
        System.out.println("timestamp: " + tx.getTimeStamp());
        System.out.println("txfee: " + tx.getTxFee().toString());
        System.out.println("txType: " + tx.getTxType());
        System.out.println("sender: " + ByteUtil.toHexString(tx.getSenderPubkey()));
        System.out.println("nonce: " + tx.getNonce());

        ArrayList<GenesisItem> genesisMsgReturned = tx.getGenesisAccounts();
        int index = 0;
        while(index < genesisMsgReturned.size()) {
            GenesisItem value = genesisMsgReturned.get(index);
            System.out.println("Account: " + Hex.toHexString(value.getAccount()));
            System.out.println("Balance: " + value.getBalance());
            System.out.println("Power: " + value.getPower());
            index++;
        }
        System.out.println("Signature: " + ByteUtil.toHexString(tx.getSignature()));
        System.out.println("Signature bool: " + tx.verifyTransactionSig());
        System.out.println("GenesisTx String: "+ Hex.toHexString(tx.getEncoded()));

        System.out.println("=======================================================");

        GenesisTx dtx = new GenesisTx(tx.getEncoded());
        System.out.println("verison: " + dtx.getVersion());
        System.out.println("chainid: " + new String(dtx.getChainID()));
        System.out.println("timestamp: " + dtx.getTimeStamp());
        System.out.println("txfee: " + dtx.getTxFee().toString());
        System.out.println("txType: " + dtx.getTxType());
        System.out.println("sender: " + ByteUtil.toHexString(dtx.getSenderPubkey()));
        System.out.println("nonce: " + dtx.getNonce());

        ArrayList<GenesisItem> genesisMsgDecode = dtx.getGenesisAccounts();
        index = 0;
        while(index < genesisMsgDecode.size()) {
            GenesisItem value = genesisMsgDecode.get(index);
            System.out.println("Account: " + Hex.toHexString(value.getAccount()));
            System.out.println("Balance: " + value.getBalance());
            System.out.println("Power: " + value.getPower());
            index++;
        }

        System.out.println("Signature: " + ByteUtil.toHexString(dtx.getSignature()));
    }

    @Test
    public void createForumNoteTx(){
        byte[] forumNote = "Hello, Taucoin!".getBytes();
        byte[] forumNoteHash = HashUtil.sha1hash(forumNote);
        ForumNoteTx tx = new ForumNoteTx(version, chainid, timestamp, txFee, sender, nonce, forumNoteHash);
        tx.signTransactionWithSeed(seed);
        System.out.println("verison: " + tx.getVersion());
        System.out.println("chainid: " + new String(tx.getChainID()));
        System.out.println("timestamp: " + tx.getTimeStamp());
        System.out.println("txfee: " + tx.getTxFee());
        System.out.println("txType: " + tx.getTxType());
        System.out.println("sender: " + ByteUtil.toHexString(tx.getSenderPubkey()));
        System.out.println("nonce: " + tx.getNonce());
        System.out.println("forum msg hash: " + ByteUtil.toHexString(tx.getForumNoteHash()));
        System.out.println("Signature: " + ByteUtil.toHexString(tx.getSignature()));
        System.out.println("Signature bool: " + tx.verifyTransactionSig());
        System.out.println("ForumNoteTx String: "+ Hex.toHexString(tx.getEncoded()));

        System.out.println("=======================================================");

        ForumNoteTx dtx = new ForumNoteTx(tx.getEncoded());
        System.out.println("verison: " + dtx.getVersion());
        System.out.println("chainid: " + new String(dtx.getChainID()));
        System.out.println("timestamp: " + dtx.getTimeStamp());
        System.out.println("txfee: " + dtx.getTxFee());
        System.out.println("txType: " + dtx.getTxType());
        System.out.println("sender: " + ByteUtil.toHexString(dtx.getSenderPubkey()));
        System.out.println("nonce: " + dtx.getNonce());
        System.out.println("forum msg hash: " + ByteUtil.toHexString(dtx.getForumNoteHash()));
        System.out.println("Signature: " + ByteUtil.toHexString(dtx.getSignature()));
    }

    @Test
    public void createWiringCoinsTx(){
        byte[] receiver = ByteUtil.toByte("2a62868271f3d3455e4b1ea0c1f96263732d0347349f9daa3247107ce1b2b2f9");
        BigInteger amount = BigInteger.valueOf(10000000L);
        byte[] memo = "test".getBytes();
        WiringCoinsTx tx = new WiringCoinsTx(version, chainid, timestamp, txFee, sender, nonce, receiver, amount, memo);
        tx.signTransactionWithSeed(seed);
        System.out.println("verison: " + tx.getVersion());
        System.out.println("chainid: " + new String(tx.getChainID()));
        System.out.println("timestamp: " + tx.getTimeStamp());
        System.out.println("txfee: " + tx.getTxFee());
        System.out.println("txType: " + tx.getTxType());
        System.out.println("sender: " + ByteUtil.toHexString(tx.getSenderPubkey()));
        System.out.println("nonce: " + tx.getNonce());
        System.out.println("txHash: " + ByteUtil.toHexString(tx.getTxID()));
        System.out.println("receiver: " + ByteUtil.toHexString(tx.getReceiver()));
        System.out.println("amount: " + tx.getAmount());
        System.out.println("Signature: " + ByteUtil.toHexString(tx.getSignature()));
        System.out.println("Sigmsg: " + ByteUtil.toHexString(tx.getTransactionSigMsg()));

        System.out.println("WiringCoinsTx String: "+ Hex.toHexString(tx.getEncoded()));
        System.out.println("Signature bool: " + tx.verifyTransactionSig());

        System.out.println("=======================================================");

        WiringCoinsTx dtx = new WiringCoinsTx(tx.getEncoded());

        System.out.println("verison: " + dtx.getVersion());
        System.out.println("chainid: " + new String(dtx.getChainID()));
        System.out.println("timestamp: " + dtx.getTimeStamp());
        System.out.println("txfee: " + dtx.getTxFee());
        System.out.println("txType: " + dtx.getTxType());
        System.out.println("sender: " + ByteUtil.toHexString(dtx.getSenderPubkey()));
        System.out.println("nonce: " + dtx.getNonce());
        System.out.println("txHash: " + ByteUtil.toHexString(dtx.getTxID()));
        System.out.println("receiver: " + ByteUtil.toHexString(dtx.getReceiver()));
        System.out.println("amount: " + dtx.getAmount());
        System.out.println("Signature: " + ByteUtil.toHexString(dtx.getSignature()));
        System.out.println("Sigmsg: " + ByteUtil.toHexString(dtx.getTransactionSigMsg()));

        System.out.println("WiringCoinsTx String: "+ Hex.toHexString(dtx.getEncoded()));
        System.out.println("Signature bool: " + dtx.verifyTransactionSig());
    }
}
