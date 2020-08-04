package io.taucoin.jtau.rpc.method;

import io.taucoin.chain.ChainManager;
import io.taucoin.controller.TauController;
import io.taucoin.core.AccountState;
import io.taucoin.jtau.rpc.JsonRpcServerMethod;

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

public class chain_getAccountState extends JsonRpcServerMethod {
    private static final Logger logger = LoggerFactory.getLogger("rpc");

    public chain_getAccountState (TauController tauController) {
        super(tauController);
    }

    @Override
    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {
        List<Object> params = req.getPositionalParams();
        if (params.size() != 1) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
			// get pubkey
            byte[] chainid = jsToBytes((String)(params.get(0)));
            byte[] pubkey = jsToBytes((String)(params.get(1)));

			// get account state
			ChainManager chainmanager = tauController.getChainManager();
			AccountState account= null;

            try {
				account= chainmanager.getAccountState(chainid, pubkey);
            } catch (Exception e) {
                e.printStackTrace();
                return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
            }

			BigInteger balance= account.getBalance();
			BigInteger nonce= account.getNonce();

			// make response
			ArrayList<String> result = new ArrayList<String>();
			result.add("Balance: "+ balance.toString());
			result.add("Nonce: "+ nonce.toString());
            JSONRPC2Response response = new JSONRPC2Response(result, req.getID());
            return response;
        }

    }
}
