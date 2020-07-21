package io.taucoin.core;

import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

import java.math.BigInteger;

public class AccountState implements Cloneable {
    private BigInteger balance;
    private BigInteger nonce;
    private String identity;

    public AccountState(byte[] rlp) {
        RLPList decodedTxList = RLP.decode2(rlp);
        RLPList accountState = (RLPList) decodedTxList.get(0);
        byte[] balanceBytes = accountState.get(0).getRLPData();
        this.balance = balanceBytes == null ? BigInteger.ZERO :
                new BigInteger(1, balanceBytes);
        byte[] nonceBytes = accountState.get(1).getRLPData();
        this.nonce = nonceBytes == null ? BigInteger.ZERO :
                new BigInteger(1, nonceBytes);
        byte[] identityBytes = accountState.get(2).getRLPData();
        this.identity = identityBytes == null ? null : new String(identityBytes);
    }

    public AccountState(BigInteger balance, BigInteger nonce) {
        this(balance, nonce, null);
    }

    public AccountState(BigInteger balance, BigInteger nonce, String identity) {
        this.balance = balance;
        this.nonce = nonce;
        this.identity = identity;
    }

    public BigInteger getBalance() {
        return balance;
    }

    public void setBalance(BigInteger balance) {
        this.balance = balance;
    }

    public void addBalance(BigInteger value) {
        this.balance = this.balance.add(value);
    }

    public void subBalance(BigInteger value) {
        this.balance = this.balance.subtract(value);
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public BigInteger getNonce() {
        return nonce;
    }

    public void setNonce(BigInteger nonce) {
        this.nonce = nonce;
    }

    public void increaseNonce() {
        this.nonce = this.nonce.add(BigInteger.ONE);
    }

    public void reduceNonce() {
        this.nonce = this.nonce.subtract(BigInteger.ONE);
    }

    public byte[] getEncoded() {
        byte[] balance = RLP.encodeBigInteger(this.balance);
        byte[] nonce = RLP.encodeBigInteger(this.nonce);
        byte[] identity = this.identity == null ? RLP.encodeElement(null): RLP.encodeString(this.identity);
        return RLP.encodeList(balance, nonce, identity);
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
                ", identity='" + identity + '\'' +
                '}';
    }
}
