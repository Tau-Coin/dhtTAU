package io.taucoin.jtau.rpc.method;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;

import java.util.ArrayList;
import java.util.List;

import io.taucoin.controller.TauController;
import io.taucoin.jtau.rpc.JsonRpcServerMethod;

public class chain_followChain extends JsonRpcServerMethod {

    public chain_followChain(TauController tauController) {
        super(tauController);
    }

    @Override
    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {
        List<Object> params = req.getPositionalParams();

        if (params.size() != 1) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            // get chain ID
            String chainID = (String) (params.get(0));

            List<byte[]> peerList = new ArrayList<>(4);
            peerList.add(jsToBytes("0x63ec42130442c91e23d56dc73708e06eb164883ab74c9813764c3fd0e2042dc4"));
            peerList.add(jsToBytes("0x809df518ee450ded0a659aeb4bc5bec636e2cff012fc88d343b7419af974bb81"));
            peerList.add(jsToBytes("0x2a62868271f3d3455e4b1ea0c1f96263732d0347349f9daa3247107ce1b2b2f9"));
            peerList.add(jsToBytes("0x3e87c35d2079858d88dcb113edadaf1b339fcd4f74c539faa9a9bd59e787f124"));

            tauController.followChain(chainID.getBytes(), peerList);

            // make response
            ArrayList<String> result = new ArrayList<>();
            result.add("Chain followed.");
            return new JSONRPC2Response(result, req.getID());
        }
    }
}
