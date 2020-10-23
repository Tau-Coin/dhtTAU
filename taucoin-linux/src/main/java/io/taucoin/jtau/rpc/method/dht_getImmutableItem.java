package io.taucoin.jtau.rpc.method;

import io.taucoin.controller.TauController;
import io.taucoin.jtau.rpc.JsonRpcServerMethod;
import io.taucoin.param.ChainParam;
import io.taucoin.types.Block;
import io.taucoin.types.Transaction;
import io.taucoin.types.TransactionFactory;
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

import static io.taucoin.dht.DHT.*;

public class dht_getImmutableItem extends JsonRpcServerMethod {

    private static final Logger logger = LoggerFactory.getLogger("rpc");

    public dht_getImmutableItem(TauController tauController) {
        super(tauController);
    }

    @Override
    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        if (params.size() != 2) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
			// get sha1 hash
            byte[] hash = Hex.decode((String)(params.get(0)));
            String type = (String)(params.get(1));

			// get immutable item
            byte[] item = dhtGet(new GetImmutableItemSpec(hash, 20));

            // make response
            String result = "";

            if (item == null) {
                result= "Get immutable item, nothing !";
            } else {
                try {
                    if ("block".equals(type)) {
                        Block block = new Block(item);
                        result = block.toString();
                    } else if ("tx".equals(type)){
                        Transaction tx = TransactionFactory.parseTransaction(item);
                        result = tx.toString();
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
