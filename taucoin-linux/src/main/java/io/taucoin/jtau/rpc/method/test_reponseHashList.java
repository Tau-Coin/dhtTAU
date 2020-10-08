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
import io.taucoin.util.ByteArrayWrapper;

public class test_reponseHashList extends JsonRpcServerMethod {

    public test_reponseHashList(TauController tauController) {
        super(tauController);
    }

    @Override
    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {
        List<Object> params = req.getPositionalParams();
        if (params.size() != 1) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            byte[] blockHash = Hex.decode((String)(params.get(0)));
            // get salt
            byte[] chainID = "TAUcoin#c84b1332519aa8020e48438eb3caa9b482798c9d".getBytes();

            List<byte[]> hashList = new ArrayList<>();
            for(int i = 0; i < 40; i++) {
                hashList.add(Hex.decode("b7b8ccd357ee09cd2905d114f6dcbf718aa0f90a"));
            }
            Chains.publishBlockResponse(new ByteArrayWrapper(chainID), blockHash, hashList);

            JSONRPC2Response response = new JSONRPC2Response("ok", req.getID());
            return response;
        }
    }
}
