package io.taucoin.core;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

public class AccountStateTest {
    private static final Logger logger = LoggerFactory.getLogger("test");

    @Test
    public void CodecTest() {
        AccountState accountState = new AccountState(BigInteger.valueOf(100), BigInteger.valueOf(3));
        byte[] rlp = accountState.getEncoded();
        AccountState accountState1 = new AccountState(rlp);
        Assert.assertEquals(accountState.getBalance(), accountState1.getBalance());
        Assert.assertEquals(accountState.getNonce(), accountState1.getNonce());
    }

//    @Test
//    public void Codec1Test() {
//        AccountState accountState = new AccountState(BigInteger.valueOf(100), BigInteger.valueOf(3), "Jim");
//        byte[] rlp = accountState.getEncoded();
//        AccountState accountState1 = new AccountState(rlp);
//        Assert.assertEquals(accountState.getBalance(), accountState1.getBalance());
//        Assert.assertEquals(accountState.getNonce(), accountState1.getNonce());
//        Assert.assertEquals(accountState.getIdentity(), accountState1.getIdentity());
//    }

    @Test
    public void CodecBalanceZeroTest() {
        AccountState accountState = new AccountState(BigInteger.valueOf(0), BigInteger.valueOf(5));
        byte[] rlp = accountState.getEncoded();
        logger.info("balance:{}, nonce:{}", accountState.getBalance(), accountState.getNonce());
        AccountState accountState1 = new AccountState(rlp);
        Assert.assertEquals(accountState.getBalance(), accountState1.getBalance());
        Assert.assertEquals(accountState.getNonce(), accountState1.getNonce());
    }

    @Test
    public void CodecNonceZeroTest() {
        AccountState accountState = new AccountState(BigInteger.valueOf(120), BigInteger.valueOf(0));
        byte[] rlp = accountState.getEncoded();
        AccountState accountState1 = new AccountState(rlp);
        Assert.assertEquals(accountState.getBalance(), accountState1.getBalance());
        Assert.assertEquals(accountState.getNonce(), accountState1.getNonce());
    }

    @Test
    public void CloneTest() {
        try {
            AccountState accountState = new AccountState(BigInteger.valueOf(120), BigInteger.valueOf(10));
            byte[] rlp = accountState.getEncoded();
            AccountState accountState1 = new AccountState(rlp);

            AccountState accountState2 = accountState.clone();
            AccountState accountState3 = accountState1.clone();

            Assert.assertEquals(accountState2.getBalance(), accountState.getBalance());
            Assert.assertEquals(accountState2.getNonce(), accountState.getNonce());

            Assert.assertEquals(accountState3.getBalance(), accountState1.getBalance());
            Assert.assertEquals(accountState3.getNonce(), accountState1.getNonce());

            accountState2.setBalance(BigInteger.valueOf(130));
            accountState2.setNonce(BigInteger.valueOf(11));

            accountState3.setBalance(BigInteger.valueOf(140));
            accountState3.setNonce(BigInteger.valueOf(12));

            Assert.assertEquals(accountState.getBalance(), BigInteger.valueOf(120));
            Assert.assertEquals(accountState.getNonce(), BigInteger.valueOf(10));
            Assert.assertEquals(accountState1.getBalance(), BigInteger.valueOf(120));
            Assert.assertEquals(accountState1.getNonce(), BigInteger.valueOf(10));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
