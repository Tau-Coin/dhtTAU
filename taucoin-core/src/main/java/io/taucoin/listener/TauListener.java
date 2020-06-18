package io.taucoin.listener;

/**
 * TauListener is the event listener of the tau blockchain.
 * TauListener implementation can be registered by TauController.
 */
public interface TauListener {

    void onNewChain(String chainId, String nickName);
}
