/**
 * Copyright 2020 taucoin developer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
 * SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.taucoin.jtau.rpc.method;

import io.taucoin.chain.ChainManager;
import io.taucoin.controller.TauController;
import io.taucoin.genesis.GenesisItem;
import io.taucoin.jtau.rpc.JsonRpcServerMethod;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * eg:
 *curl --data-binary '{"jsonrpc": "2.0", "id":"1", "method": "chain_sendTransaction", "params": [{"type": 1, "to":"ac1508fd291d91adf0f0fb9403eac94e837f205f81e4b17a45d88783b234f498", "value": 10, "fee": 1, "seed":"ddcfe7cb9c6395deec34d639f0b5237364fa1f1c00afd678ada44a8403042894", "version": 1, "notes":"test", "chainid":"taucoinX#300#edf23"}] }'  http://127.0.0.1:9088/
 */
public class chain_createNewCommunity extends JsonRpcServerMethod {

    private static final Logger logger = LoggerFactory.getLogger("rpc");

    public chain_createNewCommunity (TauController tauController) {
        super(tauController);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        if (params.size() != 1) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {

            JSONObject obj = (JSONObject)params.get(0);

            ChainManager chainmanager = tauController.getChainManager();

			// coins
			int coins = obj.getAsNumber("coins").intValue();
			// power
			int power = obj.getAsNumber("power").intValue();

            // name
			String name = null;
			if (obj.containsKey("name") && !(obj.get("name")).equals("")) {
				name = (String) obj.get("name");
			}

			logger.info("name: {}, coins: {}, power: {}", name, coins, power);

            // genesismsg
			ArrayList<GenesisItem> genesisMsg = new ArrayList<>();
			List<String> gadds = new ArrayList<String>();
			if (obj.containsKey("gadds") && !((List)obj.get("gadds")).equals("")) {
				gadds = (List) obj.get("gadds");
			}
			logger.info("geneis account list: {}", gadds);

			Iterator ita = gadds.iterator();
			while(ita.hasNext()){
				String account = (String)ita.next();
				logger.info("geneis account: {}", account);
				GenesisItem state = new GenesisItem(Hex.decode(account), BigInteger.valueOf(coins), BigInteger.valueOf(power));
				genesisMsg.add(state);
			}

			// get chainmanager and send tx	
			boolean result = chainmanager.createNewCommunity(name, genesisMsg);

            JSONRPC2Response response = new JSONRPC2Response(result, req.getID());
            return response;
    	}
	}
}
