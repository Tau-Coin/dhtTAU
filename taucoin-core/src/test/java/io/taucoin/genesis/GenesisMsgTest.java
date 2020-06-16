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
    private static final String pubkeyA= "d0dc15e1f6e3f2109d089f11e08790472cd9a39aceef6df45753c3b5bee9c147649c82deab7921492b5d510124318e943462eed47cac0a99825d856aeb4349b3";
    private static final String pubkeyB= "58b8f979a511dc3eb1a42a23881988880016ff46338fccc8b46379695377ed69ba398380c3d3d6e0a3e3e92b23a50c2a6c04f3f872492cb216be840a8f70f100";
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
       msg.setDescription("taucoin chain...");
       GenesisItem item = new GenesisItem(defaultValue,genesisPower);
       msg.appendAccount(pubkeyA,item);
       msg.appendAccount(pubkeyB,item);
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
