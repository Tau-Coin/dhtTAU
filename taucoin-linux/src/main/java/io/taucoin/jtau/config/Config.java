package io.taucoin.jtau.config;

public class Config {

    // default http json rpc server listening port.
    public static final int DEFAULT_RPC_PORT= 9088;

    // json rpc server listening port.
    private int rpcPort;

    /**
     * Config constructor.
     */
    public Config() {

        this.rpcPort = DEFAULT_RPC_PORT;
    }

    /**
     * Get rpc server listening port.
     *
     * @return int
     */
    public int getRPCPort() {
        return this.rpcPort;
    }

    /**
     * Set rpc server listening port.
     *
     * @param port rpc server listening port.
     */
    public void setRPCPort(int port) {
        this.rpcPort = port;
    }
}
