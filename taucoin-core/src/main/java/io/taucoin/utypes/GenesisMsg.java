/**
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
package io.taucoin.utypes;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.taucoin.genesis.CheckInfo;
import io.taucoin.genesis.GenesisItem;
import io.taucoin.param.ChainParam;
import io.taucoin.util.*;

public class GenesisMsg {
    private HashMap<String, GenesisItem> accountKV;
    private byte[] bEncodedBytes;
    private boolean isParsed;

    /**
     * construct genesis msg.
     */
    public GenesisMsg(){
        accountKV = new HashMap<>();
        this.isParsed = true;
    }

    public GenesisMsg(byte[] bytes){
        accountKV = new HashMap<>();
        this.bEncodedBytes = bytes;
        this.isParsed = false;
    }

    /**
     * construct genesis msg K-V
     * @param ed25519pub
     * @param item
     * @return
     */
    public int appendAccount(String ed25519pub, GenesisItem item){
        if(!accountKV.containsKey(ed25519pub)){
            accountKV.put(ed25519pub,item);
        }
        return accountKV.size();
    }

    /**
     * get genesis msg encoded.
     * @return
     */
    public byte[] getEncoded(){
        if(bEncodedBytes == null){
            byte[][] genesisMsg = new byte[this.accountKV.size()][];
            Iterator<Map.Entry<String,GenesisItem>> it = this.accountKV.entrySet().iterator();
            int i= 1;
            while (it.hasNext()){
                Map.Entry<String,GenesisItem> entry = it.next();
                byte[] addr = RLP.encodeElement(entry.getKey().getBytes());
                byte[] value = RLP.encodeElement(entry.getValue().getEncoded());
                genesisMsg[i] = RLP.encodeList(addr,value);
                i++;
            }
            this.bEncodedBytes = RLP.encodeList(genesisMsg);
        }
        return bEncodedBytes;
    }

    /**
     * parse encoded genesis.
     */
    private void parseRLP(){
        if(isParsed){
           return;
        }else{
            RLPList msg = RLP.decode2(this.bEncodedBytes);
            RLPList list = (RLPList) msg.get(0);
            //byte[] descByte = RLP.decode2(list.get(0).getRLPData()).get(0).getRLPData();
            for(int i= 0; i< list.size(); i++){
                RLPList state = (RLPList) list.get(i);
                String addr = new String(state.get(0).getRLPData());
                GenesisItem item = new GenesisItem(state.get(1).getRLPData());
                this.accountKV.put(addr, item);
            }
            isParsed = true;
        }
    }

    /**
     * get genesis msg K-V state.
     * @return
     */
    public HashMap<String,GenesisItem> getAccountKV(){
        if(!isParsed) parseRLP();
        return accountKV;
    }

    /**
     * check genesis msg KV items. when create.
     * @return
     */
    public CheckInfo validateGenesisMsg(){
        if(!isParsed) parseRLP();
        if(this.accountKV.size() >= ChainParam.MaxGenesisMsgItems) {
            return CheckInfo.TooMuchKVitem;
        }
        Iterator<Map.Entry<String,GenesisItem>> iterator = accountKV.entrySet().iterator();
        long supply=0;
        while (iterator.hasNext()){
            Map.Entry<String,GenesisItem> entry = iterator.next();
            supply += entry.getValue().getBalance().longValue();
        }
        if(supply > ChainParam.MaxTotalSupply) return CheckInfo.BigAmoutSupply;
        return CheckInfo.CheckPassed;
    }
}
