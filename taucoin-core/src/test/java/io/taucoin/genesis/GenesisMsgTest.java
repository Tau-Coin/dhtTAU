/*
Copyright 2020 taucoin developer

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
(the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
OR OTHER DEALINGS IN THE SOFTWARE.
*/
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
    private static final BigInteger defaultValue = new BigInteger("250000000000000");
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
       msg.setDescription("community chain...uu");
       GenesisItem item = new GenesisItem(defaultValue);
       msg.appendAccount(pubkeyA,item);
       msg.appendAccount(pubkeyB,item);
       msg.appendAccount(pubkeyC,item);
       msg.appendAccount(pubkeyD,item);
       String encodeStr= ByteUtil.toHexString(msg.getEncoded());
       log.debug("msg size is: "+encodeStr.length()/2);
       log.debug("is validate: "+ msg.validateGenesisMsg());
       log.debug("genesis power is: "+item.getPower());
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
