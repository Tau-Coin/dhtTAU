package io.taucoin.rpc;

import io.taucoin.manager.TauController;

/**
 * RpcServer is the http server for rpc.
 */
public class RpcServer {

    // TauController from which all the components can be got.
    private TauController tauController;

    /**
     * Construct RpcServer
     *
     * @param tauController TauController
     */
    public RpcServer(TauController tauController) {
        this.tauController = tauController;
    }

    /**
     * Start rpc server.
     */
    public void start() {
    }

    /**
     * Stop rpc server.
     */
    public void stop() {
    }
}
