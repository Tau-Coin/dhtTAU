package io.taucoin.jtau.rpc.method;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;

import org.spongycastle.util.encoders.Hex;


import io.taucoin.chain.Chains;
import io.taucoin.controller.TauController;
import io.taucoin.jtau.rpc.JsonRpcServerMethod;

public class test_getDemandSalt extends JsonRpcServerMethod {

    public test_getDemandSalt(TauController tauController) {
        super(tauController);
    }

    @Override
    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {
        // get salt
        byte[] salt = Chains.makeBlockDemandSalt("TAUcoin#c84b1332519aa8020e48438eb3caa9b482798c9d".getBytes());

        JSONRPC2Response response = new JSONRPC2Response(Hex.toHexString(salt), req.getID());
        return response;
    }
}
