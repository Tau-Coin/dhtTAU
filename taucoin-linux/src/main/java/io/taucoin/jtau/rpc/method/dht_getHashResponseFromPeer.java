package io.taucoin.jtau.rpc.method;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;

import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

import io.taucoin.chain.Chains;
import io.taucoin.controller.TauController;
import io.taucoin.jtau.rpc.JsonRpcServerMethod;
import io.taucoin.torrent.DHT;
import io.taucoin.torrent.TorrentDHTEngine;
import io.taucoin.types.HashList;

public class dht_getHashResponseFromPeer extends JsonRpcServerMethod {
    public dht_getHashResponseFromPeer(TauController tauController) {
        super(tauController);
    }

    @Override
    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        if (params.size() != 2) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            // get pubkey
            byte[] pubkey = Hex.decode((String)(params.get(0)));
            byte[] blockHash = Hex.decode((String)(params.get(1)));
            // get salt
            byte[] salt = Chains.makeBlockResponseSalt("TAUcoin#c84b1332519aa8020e48438eb3caa9b482798c9d".getBytes(), blockHash);

            // get mutable item
            byte[] item = TorrentDHTEngine.getInstance().dhtGet(
                    new DHT.GetMutableItemSpec(pubkey, salt, 20));

            // make response
            ArrayList<String> results= new ArrayList<String>();

            if (item == null) {
                results.add("Get mutable item, nothing !");
            } else {
                try {
                    HashList hashList = new HashList(item);
                    List<byte[]> list = hashList.getHashList();
                    if (null != list) {
                        for (byte[] h: list) {
                            results.add(Hex.toHexString(h));
                        }
                    } else {
                        results.add("Empty!");
                    }
                } catch (Exception e) {
                    results.add(e.toString());
                    e.printStackTrace();
                }
            }

            JSONRPC2Response response = new JSONRPC2Response(results, req.getID());
            return response;
        }
    }
}
