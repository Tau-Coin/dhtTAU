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
        if (params.size() != 2) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
			// get pubkey
            byte[] chainid = ((String)(params.get(0))).getBytes();
            byte[] pubkey = jsToBytes((String)(params.get(1)));

			// get account state
			ChainManager chainmanager = tauController.getChainManager();

            // make response
            ArrayList<String> result = new ArrayList<>();

            try {
                AccountState account= chainmanager.getStateDB().getAccount(chainid, pubkey);

				if (null != account) {
                    BigInteger balance = account.getBalance();
                    BigInteger nonce = account.getNonce();
                    result.add("Balance: " + balance.toString());
                    result.add("Nonce: " + nonce.toString());
                } else {
				    result.add("Cannot get account info.");
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
            }

            JSONRPC2Response response = new JSONRPC2Response(result, req.getID());
            return response;
        }

    }
}
