package io.taucoin.genesis;

import io.taucoin.types.GenesisTx;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.ByteUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
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
            = "425f9b4c998500cafd10f52d3f9156525f3c90a16b80ac32f0e691ef2a8482faf2aa2de1e4f8808b1e9140468ca63eefac62b979e31ef8a03da443846448af09";

    // This private key is just for test to generate signature.
    private static final String sPrivKey
            = "f008065e3ff567d4471231a4a0609e118b28f0639f9768d3f8bb123f8f0b38706ade0527cb0dd1e57ad0003fbf8e5af51c0bf0471e639b4920ab49ac17ff88f1";

    private static final ArrayList<GenesisItem> sGensisItems
            = new ArrayList<>();

    static {
        sGensisItems.add(
            new GenesisItem(ByteUtil.toByte("63ec42130442c91e23d56dc73708e06eb164883ab74c9813764c3fd0e2042dc4"), BigInteger.valueOf(10000000L), new BigInteger("100", 10))
        );

        sGensisItems.add(
            new GenesisItem(ByteUtil.toByte("809df518ee450ded0a659aeb4bc5bec636e2cff012fc88d343b7419af974bb81"), BigInteger.valueOf(10000000L), new BigInteger("100", 10))
        );

        sGensisItems.add(
            new GenesisItem(ByteUtil.toByte("2a62868271f3d3455e4b1ea0c1f96263732d0347349f9daa3247107ce1b2b2f9"), BigInteger.valueOf(10000000L), new BigInteger("100", 10))
        );

        sGensisItems.add(
            new GenesisItem(ByteUtil.toByte("3e87c35d2079858d88dcb113edadaf1b339fcd4f74c539faa9a9bd59e787f124"), BigInteger.valueOf(10000000L), new BigInteger("100", 10))
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

        /*
        super(1L, ChainID, TimeStamp, BigInteger.ZERO, Hex.decode(Sender),
                BigInteger.ONE, sGensisItems, Hex.decode(Signature));
         */

        // The following code is used to generate tx signature

        super(1L, ChainID, TimeStamp, BigInteger.ZERO, Hex.decode(Sender),
                BigInteger.ONE, sGensisItems);
        signTransactionWithPriKey(Hex.decode(sPrivKey));
        logger.info(" 3 genesis tx:" + Hex.toHexString(getEncoded()));

        //logger.info("verify signature result:" + verifyTransactionSig());
    }
}
