package io.taucoin.jtau.rpc.method;

import io.taucoin.controller.TauController;
import io.taucoin.jtau.rpc.JsonRpcServerMethod;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;

/**
 * Count dht alive nodes.
 */
public class dht_nodesCount extends JsonRpcServerMethod {

    /**
     * dht_nodesCount constructor.
     *
     * @param tauController
     */
    public dht_nodesCount (TauController tauController) {
        super(tauController);
    }

    @Override
    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        long pc = tauController.getSessionManager().dhtNodes();
        String tmp = String.valueOf(pc);
        JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
        return res;
    }
}
