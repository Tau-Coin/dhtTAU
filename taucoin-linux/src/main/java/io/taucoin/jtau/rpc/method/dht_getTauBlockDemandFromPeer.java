package io.taucoin.jtau.rpc.method;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;

import org.spongycastle.util.encoders.Hex;

import java.util.List;

import io.taucoin.chain.Chains;
import io.taucoin.controller.TauController;
import io.taucoin.jtau.rpc.JsonRpcServerMethod;
import io.taucoin.torrent.DHT;
import io.taucoin.torrent.TorrentDHTEngine;
import io.taucoin.util.ByteUtil;

public class dht_getTauBlockDemandFromPeer extends JsonRpcServerMethod {

    public dht_getTauBlockDemandFromPeer(TauController tauController) {
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
            // get salt
            byte[] salt = Chains.makeBlockDemandSalt("TAUcoin#c84b1332519aa8020e48438eb3caa9b482798c9d".getBytes());

            // get immutable item
            byte[] item = TorrentDHTEngine.getInstance().dhtGet(
                    new DHT.GetMutableItemSpec(pubkey, salt, 20));

            // make response
            String result = "";

            if (item == null) {
                result= "Get mutable item, nothing !";
            } else {
                try {
                    byte[] hash = ByteUtil.getHashFromEncode(item);
                    if (null != hash) {
                        result = "Hash: " + Hex.toHexString(hash);
                    } else {
                        result = "empty";
                    }
                } catch (Exception e) {
                    result = e.toString();
                    e.printStackTrace();
                }
            }

            JSONRPC2Response response = new JSONRPC2Response(result, req.getID());
            return response;
        }
    }
}
