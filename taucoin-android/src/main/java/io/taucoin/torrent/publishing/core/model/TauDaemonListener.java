package io.taucoin.torrent.publishing.core.model;

import androidx.annotation.NonNull;
import io.taucoin.dht2.SessionStats;
import io.taucoin.listener.TauListener;
import io.taucoin.types.BlockContainer;

public abstract class TauDaemonListener implements TauListener {

    /**
     * 链端业务组件已全部启动
     */
    @Override
    public void onTauStarted(boolean success, String errMsg) {}

    /**
     * 链端业务组件已全部停止
     */
    @Override
    public void onTauStopped() {}

    /**
     * 链端业务运行出错
     */
    public void onTauError(@NonNull String errorMsg) {}

    @Override
    public void onUPNPMapped(int index, int externalPort) {}

    @Override
    public void onUPNPUnmapped(int index) {}

    @Override
    public void onNATPMPMapped(int index, int externalPort) {}

    @Override
    public void onNATPMPUnmapped(int index) {}

    /**
     * torrent SessionStats
     */
    @Override
    public void onSessionStats(@NonNull SessionStats newStats){}

    @Override
    public void onClearChainAllState(byte[] chainID) {

    }

    /**
     * 新的区块
     * @param blockContainer 区块
     */
    @Override
    public void onNewBlock(byte[] chainID, BlockContainer blockContainer) {}

    /**
     * 区块回滚
     * @param blockContainer 区块
     */
    @Override
    public void onRollBack(byte[] chainID, BlockContainer blockContainer) {}

    /**
     * 同步区块
     * @param blockContainer 区块
     */
    @Override
    public void onSyncBlock(byte[] chainID,  BlockContainer blockContainer) {}

    @Override
    public void onDHTStarted(boolean success, String errMsg) {
        // Ignore this event
    }

    @Override
    public void onChainManagerStarted(boolean success, String errMsg) {
        // Ignore this event
    }

    @Override
    public void onDHTStopped() {
        // Ignore this event
    }

    @Override
    public void onChainManagerStopped() {
        // Ignore this event
    }
}
