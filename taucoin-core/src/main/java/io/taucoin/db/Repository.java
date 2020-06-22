package io.taucoin.db;

import io.taucoin.core.AccountState;
import io.taucoin.types.Transaction;

import java.util.Set;

public interface Repository {
    /*************************state interface*************************/
    // Chains
    void followChain(byte[] chainID);

    Set<byte[]> getChains();

    void deleteChain(byte[] chainID);

    // Current Block Hash
    void setBestBlockHash(byte[] chainID, byte[] hash);

    byte[] getBestBlockHash(byte[] chainID);

    void deleteBestBlockHash(byte[] chainID);

    // Mutable Range
    void setMutableRange(byte[] chainID, int number);

    int getMutableRange(byte[] chainID);

    void deleteMutableRange(byte[] chainID);

    // peers
    void addPeer(byte[] chainID, byte[] pubkey);

    Set<byte[]> getPeers(byte[] chainID);

    void deletePeer(byte[] chainID, byte[] pubkey);

    void deleteAllPeers(byte[] chainID);

    // TAU self Txs Pool
    Set<Transaction> getTAUSelfTxPool(byte[] chainID);

    void updateTAUSelfTxPool(byte[] chainID, Set<Transaction> txs);

    void deleteTAUSelfTxPool(byte[] chainID);

    // Total Used Data

    // ImmutablePointBlockHash
    void setImmutablePointBlockHash(byte[] chainID, byte[] hash);

    byte[] getImmutablePointBlockHash(byte[] chainID);

    byte[] deleteImmutablePointBlockHash(byte[] chainID);

    // VotesCountingPointBlockHash
    void setVotesCountingPointBlockHash(byte[] chainID, byte[] hash);

    byte[] getVotesCountingPointBlockHash(byte[] chainID);

    byte[] deleteVotesCountingPointBlockHash(byte[] chainID);


    /*************************state interface*************************/
    void updateAccounts(Set<AccountState> accountStateSet);

    AccountState getAccount(byte[] chainID, byte[] pubkey);

    void deleteAccount(byte[] chainID, byte[] pubkey);
}
