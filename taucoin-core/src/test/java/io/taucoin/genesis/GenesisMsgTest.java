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

import io.taucoin.types.GenesisMsg;
import io.taucoin.util.ByteUtil;
import io.taucoin.param.ChainParam;

public class GenesisMsgTest {
    private static final Logger log = LoggerFactory.getLogger("genesisTest");
    private static final BigInteger defaultValue = new BigInteger("250000000000000");
    private static final BigInteger genesisPower = BigInteger.ZERO;
    private static final String pubkeyA= "f89870f606f84306232d3009d59f99a092c0cc6a5508e1a94c0b41119735716c83a9a06503697239f8db65cc757f3123199945c7ecbd98c9cbeece33b443372c";
    private static final String pubkeyB= "78dc6ac127dd8a59bf5ec869f059f71ed21626f2670230321141e07ba472b24690fb0a71db6869d70a0ed30546957aadf4fcc08477c44cc5c30fca6cae312591";
    private static final String pubkeyC= "f008065e3ff567d4471231a4a0609e118b28f0639f9768d3f8bb123f8f0b38706ade0527cb0dd1e57ad0003fbf8e5af51c0bf0471e639b4920ab49ac17ff88f1";
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
       GenesisItem item = new GenesisItem(defaultValue);
       msg.appendAccount(pubkeyA,item);
       msg.appendAccount(pubkeyB,item);
       msg.appendAccount(pubkeyC,item);
       String encodeStr= ByteUtil.toHexString(msg.getEncoded());
       log.debug("genesis msg: "+encodeStr);
       log.debug("msg size is: "+encodeStr.length()/2);
       log.debug("is validate: "+ msg.validateGenesisMsg());
       log.debug("genesis balance is: "+item.getBalance());
       log.debug("genesis power is: "+item.getPower());
   }

   @Test
    public void decodeGenesisMsg(){
        GenesisMsg msg = new GenesisMsg(ByteUtil.toByte(ChainParam.TauGenesisMsg));
        HashMap<String,GenesisItem> dictory = msg.getAccountKV();
        log.debug(msg.getDescription());
        GenesisItem item = dictory.get(pubkeyB);
        log.debug("balance is: "+item.getBalance());
        log.debug(item.getPower().toString());
   }
}
