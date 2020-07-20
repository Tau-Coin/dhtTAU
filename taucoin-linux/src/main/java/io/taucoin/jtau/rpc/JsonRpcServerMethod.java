package io.taucoin.jtau.rpc;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.taucoin.controller.TauController;

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
}
