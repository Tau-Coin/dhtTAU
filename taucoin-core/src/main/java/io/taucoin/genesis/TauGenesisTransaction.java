package io.taucoin.genesis;

import io.taucoin.types.GenesisTx;
import io.taucoin.util.ByteArrayWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.HashMap;

public final class TauGenesisTransaction extends GenesisTx {

    private static final Logger logger = LoggerFactory.getLogger("TauGenesisTx");

    // TAU genesis tx public key
    public static final String Sender
            = "3e87c35d2079858d88dcb113edadaf1b339fcd4f74c539faa9a9bd59e787f124";

    // TAU genesis tx timestamp
    public static final long TimeStamp = 1596554530;

    public static final String CommunityName = "TAUcoin";

    public static final byte[] ChainID;

    static {
        ChainID = GenesisConfig.chainID(CommunityName, Hex.decode(Sender), TimeStamp);
    }

    private static final String seed
            = "95cd9f12598163a604c01f746bb6f80235c0a1938d70d50c72b7eef3fc158e0c";

    // TAU genesis tx signature
    public static final String Signature
            = "9f0c546cceb1932f01d3fe99029038663fe75f2e65377f88093ae2c30ee31f27";

    private static final HashMap<ByteArrayWrapper, GenesisItem> sGensisItems
            = new HashMap<ByteArrayWrapper, GenesisItem>();

    static {
        sGensisItems.put(
            new ByteArrayWrapper(
                    Hex.decode("63ec42130442c91e23d56dc73708e06eb164883ab74c9813764c3fd0e2042dc4")),
            new GenesisItem(new BigInteger("100000", 10), new BigInteger("100", 10))
        );

        sGensisItems.put(
            new ByteArrayWrapper(
                    Hex.decode("809df518ee450ded0a659aeb4bc5bec636e2cff012fc88d343b7419af974bb81")),
            new GenesisItem(new BigInteger("100000", 10), new BigInteger("100", 10))
        );

        sGensisItems.put(
            new ByteArrayWrapper(
                    Hex.decode("2a62868271f3d3455e4b1ea0c1f96263732d0347349f9daa3247107ce1b2b2f9")),
            new GenesisItem(new BigInteger("100000", 10), new BigInteger("100", 10))
        );

        sGensisItems.put(
            new ByteArrayWrapper(
                    Hex.decode("3e87c35d2079858d88dcb113edadaf1b339fcd4f74c539faa9a9bd59e787f124")),
            new GenesisItem(new BigInteger("100000", 10), new BigInteger("100", 10))
        );
    }

    private static volatile TauGenesisTransaction INSTANCE;

    /**
     * Get TauGenesisTransaction instance.
     *
     * @return TauGenesisTransaction instance
     */
    public static TauGenesisTransaction getInstance() {
        if (INSTANCE == null) {
            synchronized (TauGenesisTransaction.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TauGenesisTransaction();
                }
            }
        }

        return INSTANCE;
    }

    private TauGenesisTransaction() {

        super(1L, ChainID, TimeStamp, 0L, 0L, Hex.decode(Sender), 1L, sGensisItems, null);

        signTransactionWithSeed(Hex.decode(seed));
        logger.info("signature:" + Hex.toHexString(getSignature()));
    }
}
