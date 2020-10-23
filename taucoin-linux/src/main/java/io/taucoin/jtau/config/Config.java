package io.taucoin.jtau.config;

import io.taucoin.jtau.util.Repo;

import org.spongycastle.util.encoders.Hex;

public class Config {

    // default http json rpc server listening port.
    public static final int DEFAULT_RPC_PORT= 9088;

    // default dht sessions quota
    public static final int DEFAULT_SESSIONS_QUOTA = 32;

    // json rpc server listening port.
    private int rpcPort;

    // root directory for storing data.
    private String dataDir;

    // key seed to generate key pair.
    private byte[] keySeed;

    // sessions quota
    private int sessionsQuota;

    /**
     * Config constructor.
     */
    public Config() {

        this.rpcPort = DEFAULT_RPC_PORT;
        this.keySeed = null;
        this.dataDir = Repo.getDefaultDataDir();
        this.sessionsQuota = DEFAULT_SESSIONS_QUOTA;
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

    /**
     * Get data root directory.
     *
     * @return String
     */
    public String getDataDir() {
        return this.dataDir;
    }

    /**
     * Set data root directory.
     *
     * @param dir directory
     */
    public void setDataDir(String dir) {
        this.dataDir = dir;
    }

    /**
     * Get seed to generate key pair.
     *
     * @return byte[]
     */
    public byte[] getKeySeed() {
        return this.keySeed;
    }

    /**
     * Set seed to generate key pair.
     *
     * @param seed
     */
    public void setKeySeed(byte[] seed) {
        this.keySeed = seed;
    }

    /**
     * Get dht sessions quota.
     *
     * @return int
     */
    public int getSessionsQuota() {
        return this.sessionsQuota;
    }

    /**
     * Set dht sessions quota.
     *
     * @param quota
     */
    public void setSessionsQuota(int quota) {
        this.sessionsQuota = quota;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("\t -dataDir:" + this.dataDir + "\n");
        sb.append("\t -rpcPort:" + this.rpcPort + "\n");
        sb.append("\t -keySeed:" + Hex.toHexString(this.keySeed) + "\n");

        return sb.toString();
    }
}
