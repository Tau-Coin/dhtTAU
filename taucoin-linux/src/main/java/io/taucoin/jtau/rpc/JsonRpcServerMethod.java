package io.taucoin.jtau.rpc;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import io.taucoin.chain.Chain;
import io.taucoin.controller.TauController;
import io.taucoin.types.Transaction;
import io.taucoin.util.ByteUtil;

import java.math.BigInteger;

/**
 * Method handler for json rpc.
 */
public abstract class JsonRpcServerMethod implements RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger("rpc");

    // Method name which is the class simple name.
    private String name = "";

    // TauController through which all blockchain components can be accessed.
    protected TauController tauController;

    /**
     * JsonRpcServerMethod constrctor.
     *
     * @param tauController TauController
     */
    public JsonRpcServerMethod(TauController tauController) {
        this.tauController = tauController;
        this.name = this.getClass().getSimpleName();
    }

    /**
     * Indicate which requests this method handles.
     */
    @Override
    public String[] handledRequests() {
        return new String[]{name};
    }

    /**
     * Process the corresponding rpc reqeust.
     *
     * @param req json rpc request.
     * @param ctx request context
     * @return JSONRPC2Response
     */
    @Override
    public JSONRPC2Response process(JSONRPC2Request req, MessageContext ctx) {
        if (req.getMethod().equals(name)) {
            return worker(req, ctx);
        } else {
            return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
        }
    }

    protected abstract JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx);

    protected String clearJSString(String data) {
        if (data.substring(0, 2).equals("0x"))
            return data.substring(2);
        return data;
    }

    protected byte[] jsToBytes(String data) {
        return Hex.decode(clearJSString(data));
    }

    /**
     * send transaction from rpc.
     * @param obj
     * @return
     * @throws Exception
     */
    protected Transaction jsToTransaction(JSONObject obj) throws Exception {

        logger.info("json to tx: {}", obj);

        if ((!obj.containsKey("to") || ((String)obj.get("to")).equals(""))   || (!obj.containsKey("value") || ((long)obj.get("value")) <= 0)
                || (!obj.containsKey("privkey") || ((String)obj.get("privkey")).equals(""))) {
            throw new Exception("Invalid params");
        }

        //different type refer to different tx.
        long type= 0;
        if (obj.containsKey("type") && ((long)obj.get("type")) >= 0) {
            type= (long) obj.get("type");
        }

        byte[] to = null;
        if (obj.containsKey("to") && !((String)obj.get("to")).equals("")) {
            if(type==0){
                to = jsToBytes((String) obj.get("to"));
            }
            logger.info("json to address: {}", Hex.toHexString(to));
        }

        BigInteger value = BigInteger.ZERO;
        if (obj.containsKey("value") && ((long)obj.get("value")) > 0) {
            value = BigInteger.valueOf((long) obj.get("value"));
        }

        BigInteger fee = BigInteger.ZERO;
        if (obj.containsKey("fee") && ((long)obj.get("fee")) > 0) {
            fee = BigInteger.valueOf((long) obj.get("fee"));
        }

        byte[] senderPrivkey = null;
        if (obj.containsKey("privkey") && !((String)obj.get("privkey")).equals("")) {
            String prikey = (String) obj.get("privkey");
            logger.info("privkey is {}",prikey);

        }

        // Check account balance
        /**
         * todo:from taucontroller---->accountmanager---->by private---->public key.
         * todo:from taucontroller---->by chainid--->chain---->state db--->balance.
         */

//        logger.info("Sender address: {}, balance: {}", Hex.toHexString(accountAddress), balance);
//        if (value.add(fee).compareTo(balance) > 0) {
//            logger.error("Not enough balance");
//            throw new Exception("Not enough balance");
//        }

        long timeStamp = System.currentTimeMillis() / 1000;

        /**
         * todo:create transaction by chain.
         */
		Transaction tx = null;
		//tx =chain.createTransaction();

        tx.signTransaction(senderPrivkey);
        return tx;
    }
}
