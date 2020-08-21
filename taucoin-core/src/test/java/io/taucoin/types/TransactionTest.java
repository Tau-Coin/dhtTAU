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
    private static final byte[] chainid = "TAUcoin#c84b1332519aa8020e48438eb3caa9b482798c9d".getBytes();
    private static final long timestamp = 1597998963;
    private static final long txFee = 200L;
    private static final long txType = 2;
    private static final byte[] sender = ByteUtil.toByte("3e87c35d2079858d88dcb113edadaf1b339fcd4f74c539faa9a9bd59e787f124");
    private static final long nonce = 104;
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
        byte[] receiver = ByteUtil.toByte("2a62868271f3d3455e4b1ea0c1f96263732d0347349f9daa3247107ce1b2b2f9");
        long amount = 10000L;
        String memo = "test";
        WiringCoinsTx tx = new WiringCoinsTx(version, chainid, timestamp, txFee, txType, sender, nonce, receiver, amount, memo);
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
        System.out.println("Signature bool: " + tx.verifyTransactionSig());

    }
}
