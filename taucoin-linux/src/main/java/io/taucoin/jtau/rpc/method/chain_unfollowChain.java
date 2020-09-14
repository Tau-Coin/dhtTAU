package io.taucoin.jtau.rpc.method;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;

import java.util.ArrayList;
import java.util.List;

import io.taucoin.controller.TauController;
import io.taucoin.jtau.rpc.JsonRpcServerMethod;

public class chain_unfollowChain extends JsonRpcServerMethod {

    public chain_unfollowChain(TauController tauController) {
        super(tauController);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();

        if (params.size() != 1) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            // get chain ID
            String chainID = (String) (params.get(0));

            tauController.unfollowChain(chainID.getBytes());

            // make response
            ArrayList<String> result = new ArrayList<>();
            result.add("Chain unfollowed.");
            return new JSONRPC2Response(result, req.getID());
        }
    }
}
