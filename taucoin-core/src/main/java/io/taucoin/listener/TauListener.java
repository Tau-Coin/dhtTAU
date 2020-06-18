package io.taucoin.listener;

public interface TauListener {

    void onNewChain(String chainId, String nickName);
}
