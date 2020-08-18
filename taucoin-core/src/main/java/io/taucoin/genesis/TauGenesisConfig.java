package io.taucoin.genesis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

public final class TauGenesisConfig extends GenesisConfig {

    private static final Logger logger = LoggerFactory.getLogger("TauGenesisConfig");

    // TAU genesis block public key
    public static final String PubKey
            = "3e87c35d2079858d88dcb113edadaf1b339fcd4f74c539faa9a9bd59e787f124";

    // TAU genesis block timestamp
    public static final long TimeStamp = 1596554530;

    // TAU genesis tx signature
    public static final String Signature
            = "9f0c546cceb1932f01d3fe99029038663fe75f2e65377f88093ae2c30ee31f27";

    private static volatile TauGenesisConfig INSTANCE;

    /**
     * Get TauGenesisConfig instance.
     *
     * @return TauGenesisConfig instance
     */
    public static TauGenesisConfig getInstance() {
        if (INSTANCE == null) {
            synchronized (TauGenesisConfig.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TauGenesisConfig();
                }
            }
        }

        return INSTANCE;
    }

    private TauGenesisConfig() {

        super(1L, TimeStamp, GenesisConfig.DefaultBaseTarget,
                GenesisConfig.DefaultCummulativeDifficulty,
                Hex.decode(PubKey), Hex.decode(Signature),
                TauGenesisTransaction.getInstance());
    }
}
