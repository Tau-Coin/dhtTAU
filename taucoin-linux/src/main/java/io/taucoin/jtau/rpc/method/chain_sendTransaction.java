/*
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
import io.taucoin.types.GenesisTx;
import io.taucoin.types.Transaction;
import io.taucoin.types.ForumNoteTx;
import io.taucoin.types.WiringCoinsTx;
import io.taucoin.types.TypesConfig;

import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Pair;
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
import java.util.Iterator;
import java.util.List;

/**
 * eg:
 *curl --data-binary '{"jsonrpc": "2.0", "id":"1", "method": "chain_sendTransaction", "params": [{"type": 1, "to":"ac1508fd291d91adf0f0fb9403eac94e837f205f81e4b17a45d88783b234f498", "value": 10, "fee": 1, "seed":"ddcfe7cb9c6395deec34d639f0b5237364fa1f1c00afd678ada44a8403042894", "version": 1, "notes":"test", "chainid":"taucoinX#300#edf23"}] }'  http://127.0.0.1:9088/
 */
public class chain_sendTransaction extends JsonRpcServerMethod {

    private static final Logger logger = LoggerFactory.getLogger("rpc");

    public chain_sendTransaction (TauController tauController) {
        super(tauController);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        if (params.size() != 1) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {

            JSONObject obj = (JSONObject)params.get(0);

            ChainManager chainmanager = tauController.getChainManager();
			Transaction tx = null;

			byte[] chainID = obj.getAsString("chainid").getBytes();
			long timeStamp = System.currentTimeMillis() / 1000;
			byte version = obj.getAsNumber("version").byteValue();

			// seed operation
        	byte[] senderSeed = null;
	        if (obj.containsKey("seed") && !((String)obj.get("seed")).equals("")) {
            	String prikey = (String) obj.get("seed");
            	senderSeed = jsToBytes(prikey);
            	logger.info("seed is {}",prikey);
        	} else {
            	logger.error("seed is needed");
			}
			// to do: seed -> private+ pubkey -> accountstate(balance, power)
			Pair<byte[], byte[]> keypair = Ed25519.createKeypair(senderSeed);
			byte[] publicKey = keypair.first;
			byte[] privateKey = keypair.second;

        	long balance = 0;
			BigInteger nonce = BigInteger.ZERO;
        	try{
				balance = chainmanager.getStateDB().getAccount(chainID,publicKey).getBalance().longValue();
				nonce = chainmanager.getStateDB().getAccount(chainID,publicKey).getNonce().add(BigInteger.ONE);
			}catch (Exception e){

			}

        	//different type refer to different tx.
        	long type= -1;
        	if (obj.containsKey("type") && ((long)obj.get("type")) >= 0) {
            	type= (long) obj.get("type");
        	} else {
            	logger.error("Please add a valid transaction type");
			}

			// txFee
        	BigInteger fee = BigInteger.ZERO;
        	if (obj.containsKey("fee") && ((long)obj.get("fee")) > 0) {
            	fee = BigInteger.valueOf((long) obj.get("fee"));
        	} else {
            	logger.error("Please add a valid transaction type");
			}

			//to do: txFee check
            if(fee.longValue() > balance){
				String result = "no enough balance pay txfee , current balance is: "+balance;
				JSONRPC2Response response = new JSONRPC2Response(result, req.getID());
				return response;
			}

			BigInteger txfee = fee;

			// type = 0: genesis tx, 1: forum note transaction, 2: wiring coins transaction
			if(TypesConfig.TxType.GenesisType.ordinal() == type){
				logger.info("geneis tx");
				// genesismsg
				ArrayList<GenesisItem> genesisMsg = new ArrayList<>();
				List<String> gadds = new ArrayList<String>();
				if (obj.containsKey("gadds") && !((List)obj.get("gadds")).equals("")) {
					gadds = (List) obj.get("gadds");
				}
				logger.info("geneis account list: {}", gadds);

				Iterator ita = gadds.iterator();
				while(ita.hasNext()){
					logger.info("geneis account: {}", (String)ita.next());
					byte[] account = ((String)ita.next()).getBytes();
					GenesisItem genesisItem = new GenesisItem(account, BigInteger.valueOf(10000000), BigInteger.valueOf(100));
					genesisMsg.add(genesisItem);
				}

				tx = new GenesisTx(version, chainID, timeStamp, txfee, publicKey, nonce, genesisMsg);

				tx.signTransactionWithPriKey(privateKey);

			} else if(TypesConfig.TxType.FNoteType.ordinal() == type){
				// msg
				// tx construct
				String forumNoteHash = obj.getAsString("fnhash");
				tx = new ForumNoteTx(version, chainID, timeStamp, txfee, publicKey, nonce, forumNoteHash.getBytes());
				tx.signTransactionWithPriKey(privateKey);

			} else if(TypesConfig.TxType.WCoinsType.ordinal() == type) {

            	if((fee.longValue()+ obj.getAsNumber("value").longValue()) > balance){
					String result = "No enough balance pay txfee and amount, current balance is: "+ balance;
					JSONRPC2Response response = new JSONRPC2Response(result, req.getID());
					return response;
				}

				// receiver
        		byte[] to = null;
        		if (obj.containsKey("to") && !((String)obj.get("to")).equals("")) {
                	to = jsToBytes((String) obj.get("to"));
            		logger.info("json to address: {}", Hex.toHexString(to));
				}

				// amount
        		BigInteger value = BigInteger.ZERO;
        		if (obj.containsKey("value") && ((long)obj.get("value")) > 0) {
            		value = (BigInteger) obj.get("value");
        		}

				// memo
        		byte[] memo = null;
        		if (obj.containsKey("memo") && !((byte [])obj.get("memo")).equals(null)) {
            		memo = (byte []) obj.get("memo");
        		}

				// tx construct
				tx = new WiringCoinsTx(version, chainID, timeStamp, txfee, publicKey, nonce, to, value, memo);
				tx.signTransactionWithPriKey(privateKey);
        	}

			// get chainmanager and send tx	
			chainmanager.sendTransaction(tx);

            String result = "0x" + Hex.toHexString(tx.getTxID());
            JSONRPC2Response response = new JSONRPC2Response(result, req.getID());
            return response;
    	}
	}
}
