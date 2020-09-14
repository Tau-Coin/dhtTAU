package io.taucoin.jtau.rpc.method;

import io.taucoin.controller.TauController;
import io.taucoin.jtau.rpc.JsonRpcServerMethod;
import io.taucoin.param.ChainParam;
import io.taucoin.torrent.TorrentDHTEngine;
import io.taucoin.util.ByteUtil;

import com.frostwire.jlibtorrent.Entry;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList;

import static io.taucoin.torrent.DHT.*;

public class dht_getMutableItem extends JsonRpcServerMethod {

    private static final Logger logger = LoggerFactory.getLogger("rpc");

    public dht_getMutableItem(TauController tauController) {
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
            // get salt
            byte[] salt = ((String)(params.get(1))).getBytes();

			// get immutable item
            byte[] item = TorrentDHTEngine.getInstance().dhtGet(
                    new GetMutableItemSpec(pubkey, salt, 20));

			// make response
			String result = "";

            if (item == null) {
                result= "Get mutable item, nothing !";
            } else {
                try {
                    byte[] hash = ByteUtil.getHashFromEncode(item);
                    result = "Hash: " + Hex.toHexString(hash);
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
