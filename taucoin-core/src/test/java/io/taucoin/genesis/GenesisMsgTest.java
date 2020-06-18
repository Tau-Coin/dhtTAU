package io.taucoin.genesis;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Pair;

import java.math.BigInteger;
import java.util.HashMap;

import io.taucoin.util.ByteUtil;
import io.taucoin.param.ChainParam;

public class GenesisMsgTest {
    private static final Logger log = LoggerFactory.getLogger("genesisTest");
    private static final BigInteger defaultValue = new BigInteger("500000000000000");
    private static final BigInteger genesisPower = BigInteger.ZERO;
    private static final String pubkeyA= "18dc4ee9316770f261ca17cd3bbf319301a9f0930be3abcffbb73e73a50d9fb1";
    private static final String pubkeyB= "c798fc91a403bcfc6db20ffb3c1d9f5952efcb8e09b4097338cb7974993a210c";
    private static final String pubkeyC= "ca72fa9c10053142c1302425412d6f6ac1b03ebdc2cbc5fc7676d1c31fddb6e1";
    private static final String pubkeyD= "090139b40cdfbc4e803f0a5293f3005aab5132e088321baf44c276ad3cf4abda";
   @Test
   public void derivKey() {
       byte[] seed = Ed25519.createSeed();
       log.debug("seed size is: "+
               seed.length);
       Pair<byte[], byte[]> keypair = Ed25519.createKeypair(seed);
       byte[] publicKey = keypair.first;
       byte[] privateKey = keypair.second;
       log.debug("test begin....");
       log.debug("prikey size is: "+privateKey.length);
       log.debug("pubkey size is: "+publicKey.length);
       log.debug(ByteUtil.toHexString(publicKey));
       log.debug(ByteUtil.toHexString(privateKey));
   }

   @Test
    public void createGenesisMsg(){
       GenesisMsg msg = new GenesisMsg();
       msg.setDescription("community chain...");
       GenesisItem item = new GenesisItem(defaultValue,genesisPower);
       msg.appendAccount(pubkeyA,item);
       msg.appendAccount(pubkeyB,item);
       msg.appendAccount(pubkeyC,item);
       msg.appendAccount(pubkeyD,item);
       String encodeStr= ByteUtil.toHexString(msg.getEncoded());
       log.debug(encodeStr);
   }

   @Test
    public void decodeGenesisMsg(){
        GenesisMsg msg = new GenesisMsg(ByteUtil.toByte(ChainParam.TauGenesisMsg));
        HashMap<String,GenesisItem> dictory = msg.getAccountKV();
        log.debug(msg.getDescription());
        GenesisItem item = dictory.get(pubkeyA);
        log.debug(item.getBalance().toString());
        log.debug(item.getPower().toString());
   }
}
