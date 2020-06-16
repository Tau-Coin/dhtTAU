package io.taucoin.genesis;

import java.math.BigInteger;
import io.taucoin.util.*;

public class GenesisItem {
    private BigInteger balance;
    private BigInteger power;
    private byte[] rlpEncoded = null;
    private boolean isParse;
    public GenesisItem(BigInteger balance,BigInteger power) {
        this.balance = balance;
        this.power = power;
        isParse = true;
    }
    public GenesisItem(byte[] rlpEncoded){
        this.rlpEncoded = rlpEncoded;
        this.isParse = false;
    }

    public byte[] getEncoded(){
        if(rlpEncoded == null){
            byte[] balance = RLP.encodeBigInteger(this.balance);
            byte[] power = RLP.encodeBigInteger(this.power);
            rlpEncoded = RLP.encodeList(balance,power);
        }
        return rlpEncoded;
    }

    public BigInteger getBalance() {
        if(!isParse) parseRLP();
        return balance;
    }

    public BigInteger getPower(){
        if(!isParse) parseRLP();
        return power;
    }

    private void parseRLP(){
        if(isParse){
            return;
        }else {
            RLPList items = RLP.decode2(this.rlpEncoded);
            RLPList item = (RLPList)items.get(0);
            this.balance = new BigInteger(item.get(0).getRLPData());
            byte[] powers = item.get(1).getRLPData()==null? BigInteger.ZERO.toByteArray():item.get(1).getRLPData();
            this.power = new BigInteger(powers);
            isParse = true;
        }
    }
}
