package io.taucoin.listener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CompositeTauListener implements TauListener {

    List<TauListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(TauListener listener) {
        listeners.add(listener);
    }

    public void removeListener(TauListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onNewChain(String chainId, String nickName) {
        for (TauListener listener : listeners) {
	    listener.onNewChain(chainId, nickName);
	}
    }
}
