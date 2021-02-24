package io.taucoin.listener;

import io.taucoin.dht2.SessionStats;
import io.taucoin.types.BlockContainer;

/**
 * TauListener is the event listener of the tau blockchain.
 * TauListener implementation can be registered by TauController.
 */
public interface TauListener {

    void onClearChainAllState(byte[] chainID);

    /**
     * When starting TauController sucessfully or not,
     * this event will be notified.
     *
     * @param success true starting sucessfully, or else false
     * @param errMsg error message
     */
    void onTauStarted(boolean success, String errMsg);

    /**
     * This event will be notified after stopping TauController.
     */
    void onTauStopped();

    /**
     * This event will be notified if error happens during running time.
     * Note: this event won't be notified if error happens
     * during starting TauController.
     */
    void onTauError(String errMsg);

    /**
     * When starting DHT Engine sucessfully or not,
     * this event will be notified.
     * Note: this is designed just for blockchain internally and
     * application should ignore this event.
     */
    void onDHTStarted(boolean success, String errMsg);

    /**
     * When starting Chain Manager sucessfully or not,
     * this event will be notified.
     * Note: this is designed just for blockchain internally and
     * application should ignore this event.
     */
    void onChainManagerStarted(boolean success, String errMsg);

    /**
     * This event will be notified after DHT Engine stopped.
     * Note: this is designed just for blockchain internally and
     * application should ignore this event.
     */
    void onDHTStopped();

    /**
     * This event will be notified after Chain Manager stopped.
     * Note: this is designed just for blockchain internally and
     * application should ignore this event.
     */
    void onChainManagerStopped();

    /**
     * This event will be notified when a NAT router was successfully found
     * and a port was successfully mapped on it by UPNP transport.
     */
    void onUPNPMapped(int index, int externalPort);

    /**
     * This event will be notified when a NAT router was successfully found
     * but some part of the port mapping request failed by UPNP transport.
     */
    void onUPNPUnmapped(int index);

    /**
     * This event will be notified when a NAT router was successfully found
     * and a port was successfully mapped on it by NAT-PMP transport.
     */
    void onNATPMPMapped(int index, int externalPort);

    /**
     * This event will be notified when a NAT router was successfully found
     * but some part of the port mapping request failed by NAT-PMP transport.
     */
    void onNATPMPUnmapped(int index);

    /**
     * Session statistics will be notified periodally.
     */
    void onSessionStats(SessionStats newStats);

    void onNewBlock(byte[] chainID, BlockContainer blockContainer);

    void onRollBack(byte[] chainID, BlockContainer blockContainer);

    void onSyncBlock(byte[] chainID, BlockContainer blockContainer);
}
