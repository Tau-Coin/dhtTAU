package io.taucoin.db;

import io.taucoin.core.AccountState;
import io.taucoin.types.Transaction;

import java.util.Set;

public class StateDB implements Repository{

    private KeyValueDataBase db;

    public StateDB(KeyValueDataBase db) {
        this.db = db;
    }

    @Override
    public void followChain(byte[] chainID) {

    }

    @Override
    public Set<byte[]> getChains() {
        return null;
    }

    @Override
    public void deleteChain(byte[] chainID) {

    }

    @Override
    public void setBestBlockHash(byte[] chainID, byte[] hash) {

    }

    @Override
    public byte[] getBestBlockHash(byte[] chainID) {
        return new byte[0];
    }

    @Override
    public void deleteBestBlockHash(byte[] chainID) {

    }

    @Override
    public void setMutableRange(byte[] chainID, int number) {

    }

    @Override
    public int getMutableRange(byte[] chainID) {
        return 0;
    }

    @Override
    public void deleteMutableRange(byte[] chainID) {

    }

    @Override
    public void addPeer(byte[] chainID, byte[] pubkey) {

    }

    @Override
    public Set<byte[]> getPeers(byte[] chainID) {
        return null;
    }

    @Override
    public void deletePeer(byte[] chainID, byte[] pubkey) {

    }

    @Override
    public void deleteAllPeers(byte[] chainID) {

    }

    @Override
    public Set<Transaction> getTAUSelfTxPool(byte[] chainID) {
        return null;
    }

    @Override
    public void updateTAUSelfTxPool(byte[] chainID, Set<Transaction> txs) {

    }

    @Override
    public void deleteTAUSelfTxPool(byte[] chainID) {

    }

    @Override
    public void setImmutablePointBlockHash(byte[] chainID, byte[] hash) {

    }

    @Override
    public byte[] getImmutablePointBlockHash(byte[] chainID) {
        return new byte[0];
    }

    @Override
    public byte[] deleteImmutablePointBlockHash(byte[] chainID) {
        return new byte[0];
    }

    @Override
    public void setVotesCountingPointBlockHash(byte[] chainID, byte[] hash) {

    }

    @Override
    public byte[] getVotesCountingPointBlockHash(byte[] chainID) {
        return new byte[0];
    }

    @Override
    public byte[] deleteVotesCountingPointBlockHash(byte[] chainID) {
        return new byte[0];
    }

    @Override
    public void updateAccounts(Set<AccountState> accountStateSet) {

    }

    @Override
    public AccountState getAccount(byte[] chainID, byte[] pubkey) {
        return null;
    }

    @Override
    public void deleteAccount(byte[] chainID, byte[] pubkey) {

    }
}
