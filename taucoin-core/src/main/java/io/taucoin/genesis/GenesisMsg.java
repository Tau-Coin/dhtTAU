package io.taucoin.genesis;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.taucoin.util.*;

public class GenesisMsg {
    private String description;
    private HashMap<String,GenesisItem> accountKV;
    private byte[] rlpEncoded;
    private boolean isParse;

    public GenesisMsg(){
        accountKV = new HashMap<>();
    }

    public GenesisMsg(byte[] bytes){
        accountKV = new HashMap<>();
        this.rlpEncoded = bytes;
        this.isParse = false;
    }

    public int appendAccount(String ed25519pub, GenesisItem item){
        if(!accountKV.containsKey(ed25519pub)){
            accountKV.put(ed25519pub,item);
        }
        return accountKV.size();
    }

    public void setDescription(String vision){
        this.description = vision;
    }

    public byte[] getEncoded(){
        if(rlpEncoded == null){
            byte[][] genesisMsg = new byte[this.accountKV.size() + 1][];
            byte[] description = RLP.encodeElement(this.description.getBytes());
            genesisMsg[0]= description;
            Iterator<Map.Entry<String,GenesisItem>> it = this.accountKV.entrySet().iterator();
            int i= 1;
            while (it.hasNext()){
                Map.Entry<String,GenesisItem> entry = it.next();
                String state = entry.getKey() + ":" +ByteUtil.toHexString(entry.getValue().getEncoded());
                genesisMsg[i] = RLP.encodeElement(state.getBytes());
                i++;
            }
            this.rlpEncoded = RLP.encode(genesisMsg);
        }
        return rlpEncoded;
    }

    private void parseRLP(){
        if(isParse){
           return;
        }else{
            RLPList msg =RLP.decode2(this.rlpEncoded);
            RLPList list = (RLPList) msg.get(0);
            byte[] descByte = RLP.decode2(list.get(0).getRLPData()).get(0).getRLPData();
            this.description = new String(descByte);
            String kvItem;
            String[] spliteKV;
            for(int i=1;i<list.size();i++){
                byte[] state = RLP.decode2(list.get(i).getRLPData()).get(0).getRLPData();
                kvItem = new String(state);
                spliteKV = kvItem.split(":");
                this.accountKV.put(spliteKV[0], new GenesisItem(ByteUtil.toByte(spliteKV[1])));
            }
            isParse = true;
        }
    }
    public String getDescription(){
        if(!isParse) parseRLP();
        return description;
    }
    public HashMap<String,GenesisItem> getAccountKV(){
        if(!isParse) parseRLP();
        return accountKV;
    }
}