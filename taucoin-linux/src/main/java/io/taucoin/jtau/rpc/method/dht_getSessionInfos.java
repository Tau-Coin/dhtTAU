package io.taucoin.jtau.rpc.method;

import io.taucoin.controller.TauController;
import io.taucoin.dht.DHTEngine;
import io.taucoin.dht.session.SessionInfo;
import io.taucoin.jtau.rpc.JsonRpcServerMethod;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.List;

public class dht_getSessionInfos extends JsonRpcServerMethod {

    private static final Logger logger = LoggerFactory.getLogger("rpc");

    public dht_getSessionInfos(TauController tauController) {
        super(tauController);
    }

    @Override
    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

		StringBuilder sb = new StringBuilder();
        List<SessionInfo> sis = DHTEngine.getInstance().getSessionInfos();

        for (SessionInfo si : sis) {
            sb.append(si.toString());
            sb.append(",");
        }

        JSONRPC2Response response = new JSONRPC2Response(sb.toString(), req.getID());
        return response;
    }
}
