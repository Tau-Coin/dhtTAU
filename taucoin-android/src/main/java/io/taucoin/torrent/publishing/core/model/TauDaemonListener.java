package io.taucoin.torrent.publishing.core.model;

import com.frostwire.jlibtorrent.SessionStats;

import androidx.annotation.NonNull;
import io.taucoin.listener.TauListener;
import io.taucoin.types.Block;

public abstract class TauDaemonListener implements TauListener {

    /**
     * 链端业务组件已全部启动
     */
    public void onTauStarted() {}

    /**
     * 链端业务组件已全部停止
     */
    public void onTauStopped() {}

    /**
     * 链端业务运行出错
     */
    public void onTauError(@NonNull String errorMsg) {}

    /**
     * torrent SessionStats
     */
    public void onSessionStats(@NonNull SessionStats newStats){}

    /**
     * 新的社区链
     * @param chainId 链ID
     * @param nickName 链名
     */
    @Override
    public void onNewChain(String chainId, String nickName) {}

    /**
     * 新的区块
     * @param block 区块
     */
    public void onNewBlock(Block block) {}

    /**
     * 区块回滚
     * @param block 区块
     */
    public void onRollBack(Block block) {}

    /**
     * 同步区块
     * @param block 区块
     */
    public void onSyncBlock(Block block) {}
}
