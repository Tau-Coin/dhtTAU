package io.taucoin.jtau.rpc.method;

import com.frostwire.jlibtorrent.Entry;
import com.frostwire.jlibtorrent.Sha1Hash;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;

import net.minidev.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.List;

import io.taucoin.chain.ChainManager;
import io.taucoin.controller.TauController;
import io.taucoin.dht.util.Utils;
import io.taucoin.util.HashUtil;
import io.taucoin.jtau.rpc.JsonRpcServerMethod;
import io.taucoin.types.Block;
import io.taucoin.types.Transaction;
import io.taucoin.util.ByteUtil;

import static io.taucoin.dht.DHT.*;

public class chain_sendRawBlock extends JsonRpcServerMethod {
    private static final Logger logger = LoggerFactory.getLogger("rpc");

    public chain_sendRawBlock (TauController tauController) {
        super(tauController);
    }

    @Override
    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {
        List<Object> params = req.getPositionalParams();
        if (params.size() != 1) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            JSONObject obj = (JSONObject)params.get(0);
            Block block;
            try {
                block = new Block(ByteUtil.toByte(obj.getAsString("block")));
            } catch (Exception e) {
                e.printStackTrace();
                return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
            }

            ImmutableItem item = new ImmutableItem(block.getEncoded());
            dhtPut(item);
            // compute entry sha1hash
            Entry entry = Utils.fromPreformattedBytes(block.getEncoded());
            String tmp = new Sha1Hash(HashUtil.sha1hash(entry.bencode())).toHex();
            JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
            return res;
        }

    }
}
