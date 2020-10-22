package io.taucoin.jtau.rpc.method;

import com.frostwire.jlibtorrent.Pair;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;

import org.spongycastle.util.encoders.Hex;

import io.taucoin.account.AccountManager;
import io.taucoin.chain.Salt;
import io.taucoin.controller.TauController;
import io.taucoin.jtau.rpc.JsonRpcServerMethod;
import io.taucoin.torrent.DHT;
import io.taucoin.torrent.TorrentDHTEngine;
import io.taucoin.types.HashList;

public class dht_putTauGenesisBlockHashInDemand extends JsonRpcServerMethod {

    public dht_putTauGenesisBlockHashInDemand(TauController tauController) {
        super(tauController);
    }

    @Override
    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();
        byte[] encode = HashList.with(Hex.decode("9738450c31228d0e4b8c29e4677515e30c2e64e6")).getEncoded();
        byte[] salt = Salt.makeBlockDemandSalt("TAUcoin#c84b1332519aa8020e48438eb3caa9b482798c9d".getBytes());
        if (null != encode) {
            DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first, keyPair.second, encode, salt);
            TorrentDHTEngine.getInstance().dhtPut(mutableItem);
            return new JSONRPC2Response("ok", req.getID());
        } else {
            return new JSONRPC2Response("Null", req.getID());
        }
    }
}
