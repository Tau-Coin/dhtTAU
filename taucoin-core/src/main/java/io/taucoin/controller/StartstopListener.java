package io.taucoin.controller;

import io.taucoin.listener.TauListener;
import io.taucoin.torrent.SessionStats;
import io.taucoin.types.Block;
import io.taucoin.types.BlockContainer;

public abstract class StartstopListener implements TauListener {

    @Override
    public void onClearChainAllState(byte[] chainID) {}

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

    @Override
    public void onNewBlock(byte[] chainID, BlockContainer blockContainer) {}

    @Override
    public void onRollBack(byte[] chainID, BlockContainer blockContainer) {}

    @Override
    public void onSyncBlock(byte[] chainID, BlockContainer blockContainer) {}
}
