package io.taucoin.jtau.rpc.method;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.List;

import io.taucoin.account.AccountManager;
import io.taucoin.controller.TauController;
import io.taucoin.dht.DHT;
import io.taucoin.jtau.rpc.JsonRpcServerMethod;
import io.taucoin.param.ChainParam;
import io.taucoin.types.HashList;
import io.taucoin.util.ByteUtil;

public class dht_getMutableChatMessage extends JsonRpcServerMethod {

    private static final Logger logger = LoggerFactory.getLogger("rpc");
    private final int SHORT_ADDRESS_LENGTH = 4;

    public dht_getMutableChatMessage(TauController tauController) {
        super(tauController);
    }

    /**
     * 获取聊天接收频道salt
     * @param friend 对方public key
     * @return salt
     */
    public byte[] getReceivingSalt(byte[] friend) {
        byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

        byte[] salt = new byte[SHORT_ADDRESS_LENGTH * 2];
        System.arraycopy(friend, 0, salt, 0, SHORT_ADDRESS_LENGTH);
        System.arraycopy(pubKey, 0, salt, SHORT_ADDRESS_LENGTH, SHORT_ADDRESS_LENGTH);

        return salt;
    }

    /**
     * 获取上一个聊天接收频道salt
     * @param friend 对方public key
     * @return salt
     */
    public byte[] getPreviousReceivingSalt(byte[] friend) {
        byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

        long time = System.currentTimeMillis() / 1000 / ChainParam.COMMUNICATION_CHANNEL_TIME - 1;
        byte[] timeBytes = ByteUtil.longToBytes(time);

        byte[] salt = new byte[SHORT_ADDRESS_LENGTH * 2 + timeBytes.length];
        System.arraycopy(friend, 0, salt, 0, SHORT_ADDRESS_LENGTH);
        System.arraycopy(pubKey, 0, salt, SHORT_ADDRESS_LENGTH, SHORT_ADDRESS_LENGTH);
        System.arraycopy(timeBytes, 0, salt, SHORT_ADDRESS_LENGTH * 2, timeBytes.length);

        return salt;
    }

    @Override
    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {
        List<Object> params = req.getPositionalParams();
        if (params.size() != 1) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            // get pubkey
            byte[] pubkey = Hex.decode((String)(params.get(0)));

            byte[] salt = getReceivingSalt(pubkey);
            // get mutable item
            byte[] item = dhtGet(new DHT.GetMutableItemSpec(pubkey, salt, 20));

            // make response
            String result = "";

            if (item == null) {
                result= "Get mutable item, nothing !";
            } else {
                try {
                    byte[] hash = new HashList(item).getFirstHash();
                    if (null != hash) {
                        result = "Hash: " + Hex.toHexString(hash);
                    } else {
                        result = "empty";
                    }
                } catch (Exception e) {
                    result = e.toString();
                    e.printStackTrace();
                }
            }

            JSONRPC2Response response = new JSONRPC2Response(result, req.getID());
            return response;
        }
    }
}
