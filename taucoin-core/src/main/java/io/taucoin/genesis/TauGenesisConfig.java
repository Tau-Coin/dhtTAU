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
    public static final long TimeStamp = 1597735687;

   // This private key is just for test to generate signature.
    private static final String sPrivKey
            = "f008065e3ff567d4471231a4a0609e118b28f0639f9768d3f8bb123f8f0b38706ade0527cb0dd1e57ad0003fbf8e5af51c0bf0471e639b4920ab49ac17ff88f1";

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

        // The following code is used to generate block signature

        super(1L, TimeStamp, GenesisConfig.DefaultBaseTarget,
                GenesisConfig.DefaultCummulativeDifficulty,
                Hex.decode(PubKey), null,
                TauGenesisTransaction.getInstance());

        this.genesisBlock.signBlock(Hex.decode(sPrivKey));

        logger.info("blk signature:" + Hex.toHexString(this.genesisBlock.getSignature()));

        logger.info("verify signature result:" + this.genesisBlock.verifyBlockSig());
    }

}
