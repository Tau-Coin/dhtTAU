package io.taucoin.jtau.rpc.method;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.taucoin.chain.ChainManager;
import io.taucoin.controller.TauController;
import io.taucoin.core.AccountState;
import io.taucoin.jtau.rpc.JsonRpcServerMethod;
import io.taucoin.jtau.util.Repo;

import static io.taucoin.jtau.util.Repo.RepoException;

public class chain_updateKey extends JsonRpcServerMethod {
    private static final Logger logger = LoggerFactory.getLogger("rpc");

    public chain_updateKey(TauController tauController) {
        super(tauController);
    }

    @Override
    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {
        List<Object> params = req.getPositionalParams();
        if (params.size() != 1) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            // get seed
            byte[] seed = jsToBytes((String)(params.get(0)));

            tauController.updateKey(seed);

            // update key seed into wallet
            try {
                Repo.getInstance().updateKeySeed(seed);
            } catch (RepoException e) {
                e.printStackTrace();
            }

            // make response
            ArrayList<String> result = new ArrayList<>();
            result.add("Update seed succeed.");
            return new JSONRPC2Response(result, req.getID());
        }
    }
}
