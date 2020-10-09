package io.taucoin.jtau.rpc.method;

import com.frostwire.jlibtorrent.Pair;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;

import org.spongycastle.util.encoders.Hex;

import io.taucoin.account.AccountManager;
import io.taucoin.controller.TauController;
import io.taucoin.jtau.rpc.JsonRpcServerMethod;
import io.taucoin.torrent.DHT;
import io.taucoin.torrent.TorrentDHTEngine;
import io.taucoin.util.ByteUtil;

public class dht_putGenesisHashInTipBlockChannel extends JsonRpcServerMethod {

    public dht_putGenesisHashInTipBlockChannel(TauController tauController) {
        super(tauController);
    }

    @Override
    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();
        byte[] encode = ByteUtil.getHashEncoded(Hex.decode("9738450c31228d0e4b8c29e4677515e30c2e64e6"));
        byte[] salt = "TAUcoin#c84b1332519aa8020e48438eb3caa9b482798c9d#blkTip".getBytes();
        if (null != encode) {
            DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first, keyPair.second, encode, salt);
            TorrentDHTEngine.getInstance().dhtPut(mutableItem);
            return new JSONRPC2Response("ok", req.getID());
        } else {
            return new JSONRPC2Response("Null", req.getID());
        }
    }
}
