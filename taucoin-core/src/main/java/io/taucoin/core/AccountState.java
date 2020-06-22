package io.taucoin.core;

import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

import java.math.BigInteger;

public class AccountState implements Cloneable {
    private BigInteger balance;
    private BigInteger nonce;

    public AccountState(byte[] rlp) {
        RLPList decodedTxList = RLP.decode2(rlp);
        RLPList accountState = (RLPList) decodedTxList.get(0);
        byte[] balanceBytes = accountState.get(0).getRLPData();
        this.balance = balanceBytes == null ? BigInteger.ZERO :
                new BigInteger(1, balanceBytes);
        byte[] nonceBytes = accountState.get(1).getRLPData();
        this.nonce = nonceBytes == null ? BigInteger.ZERO :
                new BigInteger(1, nonceBytes);
    }

    public AccountState(BigInteger balance, BigInteger nonce) {
        this.balance = balance;
        this.nonce = nonce;
    }

    public BigInteger getBalance() {
        return balance;
    }

    public void setBalance(BigInteger balance) {
        this.balance = balance;
    }

    public BigInteger getNonce() {
        return nonce;
    }

    public void setNonce(BigInteger nonce) {
        this.nonce = nonce;
    }

    public byte[] getEncoded() {
        byte[] balance = RLP.encodeBigInteger(this.balance);
        byte[] nonce = RLP.encodeBigInteger(this.nonce);
        return RLP.encodeList(balance, nonce);
    }

    @Override
    protected AccountState clone() throws CloneNotSupportedException {
        return (AccountState)super.clone();
    }

    @Override
    public String toString() {
        return "AccountState{" +
                "balance=" + balance +
                ", nonce=" + nonce +
                '}';
    }
}
