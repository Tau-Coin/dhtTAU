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
package io.taucoin.core.types;
import com.frostwire.jlibtorrent.Ed25519;

import io.taucoin.genesis.GenesisItem;
import  io.taucoin.types.ForumNoteTx;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;

import io.taucoin.types.GenesisTx;
import io.taucoin.types.WiringCoinsTx;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.HashUtil;

public class TransactionTest {
    private static final Logger log = LoggerFactory.getLogger("transactionTest");
    private static final long version = 1;
    private static final byte[] chainid = "TAUcoin#300#3938383036366633393364383365393338373434".getBytes();
    private static final long timestamp = 1597062314;
    private static final long txFee = 3010000000L;
    private static final long txType = 1;
    private static final byte[] sender = ByteUtil.toByte("c5897865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c");
    private static final long nonce = 1234;
    private static final byte[] signature = ByteUtil.toByte("281f3c2fe309683c74762f965f38bd8f8910d8dbeca1da904d6821e8101075776243379a4efdfdc8c10ae34be767a825f770e6a62b5430c030f179b74057e747");
    private static final byte[] seed = Ed25519.createSeed();

    @Test
    public void createGenesisTx(){
        BigInteger abalance = new BigInteger("ffffffffffff", 16);
        BigInteger anonce = new BigInteger("ff", 16);
        ByteArrayWrapper account = new ByteArrayWrapper(sender);
        GenesisItem item = new GenesisItem(abalance, anonce);
        HashMap<ByteArrayWrapper, GenesisItem> genesisMsg = new HashMap<>();
        genesisMsg.put(account, item);
        GenesisTx tx = new GenesisTx(version, chainid, timestamp, txFee, txType, sender, nonce, genesisMsg);
        tx.signTransactionWithSeed(seed);
        System.out.println("verison: " + tx.getVersion());
        System.out.println("chainid: " + new String(tx.getChainID()));
        System.out.println("timestamp: " + tx.getTimeStamp());
        System.out.println("txfee: " + tx.getTxFee());
        System.out.println("txType: " + tx.getTxType());
        System.out.println("sender: " + ByteUtil.toHexString(tx.getSenderPubkey()));
        System.out.println("nonce: " + tx.getNonce());
        HashMap<ByteArrayWrapper, GenesisItem> genesisMsgReturned = tx.getGenesisAccounts();
        Iterator<ByteArrayWrapper> accountItor = genesisMsgReturned.keySet().iterator();
        while(accountItor.hasNext()) {

            ByteArrayWrapper key = accountItor.next();
            GenesisItem value = genesisMsgReturned.get(key);
            System.out.println("Account: " + ByteUtil.toHexString(key.getData()));
            System.out.println("Balance: " + value.getBalance());
            System.out.println("Power: " + value.getPower());

        }
        System.out.println("Signature: " + ByteUtil.toHexString(tx.getSignature()));
    }

    @Test
    public void createForumNoteTx(){
        byte[] forumNote = "Hello, Taucoin!".getBytes();
        byte[] forumNoteHash = HashUtil.sha1hash(forumNote);
        ForumNoteTx tx = new ForumNoteTx(version, chainid, timestamp, txFee, txType, sender, nonce, forumNoteHash);
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
    }

    @Test
    public void createWiringCoinsTx(){
        byte[] receiver = ByteUtil.toByte("c5ef7865e8cd75d4aec7fe9583a869c8b962921cc6aef2bf5ed3ff2aed0eb23c");
        long amount = 100000000000L;
        WiringCoinsTx tx = new WiringCoinsTx(version, chainid, timestamp, txFee, txType, sender, nonce, receiver, amount);
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
    }
}
