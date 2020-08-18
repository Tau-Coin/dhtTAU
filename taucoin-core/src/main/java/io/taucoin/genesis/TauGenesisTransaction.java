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
    public static final long TimeStamp = 1597735687;

    public static final String CommunityName = "TAUcoin";

    public static final byte[] ChainID;

    static {
        ChainID = GenesisConfig.chainID(CommunityName, Hex.decode(Sender), TimeStamp);
    }

    // TAU genesis tx signature
    public static final String Signature
            = "f5dfac335aecc50b9b49d2fb5422add6348bbd6efdc0c9047f913dc53652eb1b1a125344c91d7efe62b9e1f9e4a5babafdca210d18502425855790f191991009";

    // This private key is just for test to generate signature.
    private static final String sPrivKey
            = "f008065e3ff567d4471231a4a0609e118b28f0639f9768d3f8bb123f8f0b38706ade0527cb0dd1e57ad0003fbf8e5af51c0bf0471e639b4920ab49ac17ff88f1";

    private static final HashMap<ByteArrayWrapper, GenesisItem> sGensisItems
            = new HashMap<ByteArrayWrapper, GenesisItem>();

    static {
        sGensisItems.put(
            new ByteArrayWrapper(
                    Hex.decode("63ec42130442c91e23d56dc73708e06eb164883ab74c9813764c3fd0e2042dc4")),
            new GenesisItem(new BigInteger("10000000", 10), new BigInteger("100", 10))
        );

        sGensisItems.put(
            new ByteArrayWrapper(
                    Hex.decode("809df518ee450ded0a659aeb4bc5bec636e2cff012fc88d343b7419af974bb81")),
            new GenesisItem(new BigInteger("10000000", 10), new BigInteger("100", 10))
        );

        sGensisItems.put(
            new ByteArrayWrapper(
                    Hex.decode("2a62868271f3d3455e4b1ea0c1f96263732d0347349f9daa3247107ce1b2b2f9")),
            new GenesisItem(new BigInteger("10000000", 10), new BigInteger("100", 10))
        );

        sGensisItems.put(
            new ByteArrayWrapper(
                    Hex.decode("3e87c35d2079858d88dcb113edadaf1b339fcd4f74c539faa9a9bd59e787f124")),
            new GenesisItem(new BigInteger("10000000", 10), new BigInteger("100", 10))
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

        super(1L, ChainID, TimeStamp, 0L, 0L, Hex.decode(Sender), 1L,
                sGensisItems, Hex.decode(Signature));

        // The following code is used to generate tx signature
        /*
        super(1L, ChainID, TimeStamp, 0L, 0L, Hex.decode(Sender), 1L, sGensisItems);
        signTransactionWithPriKey(Hex.decode(sPrivKey));
        logger.info("signature:" + Hex.toHexString(getSignature()));
         */
        logger.info("verify signature result:" + verifyTransactionSig());
    }
}
