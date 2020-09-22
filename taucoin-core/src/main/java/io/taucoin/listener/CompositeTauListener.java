package io.taucoin.listener;

import io.taucoin.torrent.SessionStats;
import io.taucoin.types.BlockContainer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CompositeTauListener is the registery of TauListener.
 * It's designed for tau blockchain internal component.
 * The core components can publish some event through CompositeTauListener.
 */
public class CompositeTauListener implements TauListener {

    List<TauListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(TauListener listener) {
        listeners.add(listener);
    }

    public void removeListener(TauListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onClearChainAllState(byte[] chainID) {
        for (TauListener listener : listeners) {
            listener.onClearChainAllState(chainID);
        }
    }

    @Override
    public void onTauStarted(boolean success, String errMsg) {
        for (TauListener listener : listeners) {
            listener.onTauStarted(success, errMsg);
        }
    }

    @Override
    public void onTauStopped() {
        for (TauListener listener : listeners) {
            listener.onTauStopped();
        }
    }

    @Override
    public void onTauError(String errMsg) {
        for (TauListener listener : listeners) {
            listener.onTauError(errMsg);
        }
    }

    @Override
    public void onDHTStarted(boolean success, String errMsg) {
        for (TauListener listener : listeners) {
            listener.onDHTStarted(success, errMsg);
        }
    }

    @Override
    public void onChainManagerStarted(boolean success, String errMsg) {
        for (TauListener listener : listeners) {
            listener.onChainManagerStarted(success, errMsg);
        }
    }

    @Override
    public void onDHTStopped() {
        for (TauListener listener : listeners) {
            listener.onDHTStopped();
        }
    }

    @Override
    public void onChainManagerStopped() {
        for (TauListener listener : listeners) {
            listener.onChainManagerStopped();
        }
    }

    @Override
    public void onSessionStats(SessionStats newStats) {
        for (TauListener listener : listeners) {
            listener.onSessionStats(newStats);
        }
    }

    @Override
    public void onNewBlock(byte[] chainID, BlockContainer blockContainer) {
        for (TauListener listener : listeners) {
            listener.onNewBlock(chainID, blockContainer);
        }
    }

    @Override
    public void onRollBack(byte[] chainID, BlockContainer blockContainer) {
        for (TauListener listener : listeners) {
            listener.onRollBack(chainID, blockContainer);
        }
    }

    @Override
    public void onSyncBlock(byte[] chainID, BlockContainer blockContainer) {
        for (TauListener listener : listeners) {
            listener.onSyncBlock(chainID, blockContainer);
        }
    }
}
