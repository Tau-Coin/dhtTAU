package io.taucoin.controller;

import io.taucoin.listener.TauListener;
import io.taucoin.torrent.SessionStats;

public abstract class StartstopListener implements TauListener {

    @Override
    public void onNewChain(String chainId, String nickName) {}

    @Override
    public void onTauStarted(boolean success, String errMsg) {}

    @Override
    public void onTauStopped() {}

    @Override
    public void onTauError(String errMsg) {}

    @Override
    public abstract void onDHTStarted(boolean success, String errMsg);

    @Override
    public abstract void onChainManagerStarted(boolean success, String errMsg);

    @Override
    public abstract void onDHTStopped();

    @Override
    public abstract void onChainManagerStopped();

    @Override
    public void onSessionStats(SessionStats newStats) {}
}
