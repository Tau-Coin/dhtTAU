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
import io.taucoin.jtau.rpc.JsonRpcServerMethod;
import io.taucoin.types.Block;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class chain_getBlockByNumber extends JsonRpcServerMethod {

    private static final Logger logger = LoggerFactory.getLogger("rpc");

    public chain_getBlockByNumber (TauController tauController) {
        super(tauController);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {
        List<Object> params = req.getPositionalParams();
        if (params.size() != 2) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {

			// get chainid, block number
            byte[] chainid = ((String)(params.get(0))).getBytes();
            long blockNum = (long)(params.get(1));

		    ChainManager chainmanager = tauController.getChainManager();
            String result = "";
            try {
                logger.info("chainid: {}, blockNum: {}", chainid, blockNum);
                Block block = chainmanager.getBlockStore().getMainChainBlockByNumber(chainid, blockNum);
                if(null == block) {
                    result = "No "+ blockNum+ " block is empty";
                } else {
                    result = block.toString();
                }

            } catch (Exception e) {
                return new JSONRPC2Response(JSONRPC2Error.INTERNAL_ERROR, req.getID());
            }
		    JSONRPC2Response response = new JSONRPC2Response(result, req.getID());
		    return response;
        }
    }
}
