package io.taucoin.jtau.rpc.method;

import com.frostwire.jlibtorrent.Pair;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;

import org.spongycastle.util.encoders.Hex;

import java.util.List;

import io.taucoin.account.AccountManager;
import io.taucoin.chain.Chains;
import io.taucoin.controller.TauController;
import io.taucoin.jtau.rpc.JsonRpcServerMethod;
import io.taucoin.dht.DHT;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.ByteUtil;

public class dht_putBlockHashDemand extends JsonRpcServerMethod {
    public dht_putBlockHashDemand(TauController tauController) {
        super(tauController);
    }

    @Override
    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        if (params.size() != 1) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            // get sha1 hash
            byte[] hash = Hex.decode((String) (params.get(0)));
            Chains.publishBlockDemand(new ByteArrayWrapper("TAUcoin#c84b1332519aa8020e48438eb3caa9b482798c9d".getBytes()), hash);
            JSONRPC2Response response = new JSONRPC2Response("ok", req.getID());
            return response;
        }
    }
}
