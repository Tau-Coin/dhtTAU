package io.taucoin.jtau.rpc.method;

import io.taucoin.chain.ChainManager;
import io.taucoin.controller.TauController;
import io.taucoin.core.AccountState;
import io.taucoin.jtau.rpc.JsonRpcServerMethod;
import io.taucoin.util.ByteArrayWrapper;

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
import java.util.Set;

public class chain_getAllFollowedChainID extends JsonRpcServerMethod {
    private static final Logger logger = LoggerFactory.getLogger("rpc");

    public chain_getAllFollowedChainID (TauController tauController) {
        super(tauController);
    }

    @Override
    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {
		ChainManager chainmanager = tauController.getChainManager();
		Set<ByteArrayWrapper> chainsID = chainmanager.getAllChainsID();
		logger.info("Size of all chain is {}", chainsID.size());
		ArrayList<String> results= new ArrayList<String>();
		
		for (ByteArrayWrapper chainid : chainsID) {
			String result = new String(chainid.getData());
			results.add(result);
		}

		JSONRPC2Response response = new JSONRPC2Response(results, req.getID());
		return response;
    }
}
