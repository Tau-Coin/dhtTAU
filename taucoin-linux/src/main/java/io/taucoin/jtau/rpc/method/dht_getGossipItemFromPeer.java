package io.taucoin.jtau.rpc.method;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.List;

import io.taucoin.controller.TauController;
import io.taucoin.dht.DHT;
import io.taucoin.jtau.rpc.JsonRpcServerMethod;
import io.taucoin.param.ChainParam;
import io.taucoin.types.GossipMutableData;

public class dht_getGossipItemFromPeer extends JsonRpcServerMethod {

    private static final Logger logger = LoggerFactory.getLogger("rpc");

    public dht_getGossipItemFromPeer(TauController tauController) {
        super(tauController);
    }

    @Override
    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();

        if (params.size() != 1) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            // get pubkey
            byte[] pubkey = Hex.decode((String)(params.get(0)));
            byte[] salt = ChainParam.GOSSIP_CHANNEL;

            DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(pubkey, salt);

            byte[] item = dhtGet(spec);

            // make response
            String result = "";

            if (item == null) {
                result= "Get mutable item, nothing !";
            } else {
                GossipMutableData gossip = new GossipMutableData(item);

                result = gossip.toString();
            }

            JSONRPC2Response response = new JSONRPC2Response(result, req.getID());
            return response;
        }
    }

}
