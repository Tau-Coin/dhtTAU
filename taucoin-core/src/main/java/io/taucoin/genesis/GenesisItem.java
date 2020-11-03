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
package io.taucoin.genesis;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

import io.taucoin.param.ChainParam;
import io.taucoin.util.*;

public class GenesisItem {

    private byte[] account;
    private BigInteger balance;
    private BigInteger power;

    private byte[] rlpEncoded = null;
    private boolean isParse;

    /**
     * genesis account status that community creator can set.
     * @param balance
     * @param power
     */
    public GenesisItem(byte[] account, BigInteger balance, BigInteger power) {
        this.account = account;
        this.balance = balance;
        this.power = power;
        isParse = true;
    }

    /**
     * genesis default account power to ensure block chain smoothly.
     * @param balance
     */
    public GenesisItem(byte[] account, BigInteger balance){
        this.account = account;
        this.balance = balance;
        this.power = ChainParam.DefaultGeneisisPower;
        isParse = true;
    }

    public GenesisItem(byte[] rlpEncoded){
        this.rlpEncoded = rlpEncoded;
        this.isParse = false;
    }

    /**
     * encode genesis item.
     * @return
     */
    public byte[] getEncoded(){
        if(rlpEncoded == null){

            byte[] account = RLP.encodeElement(this.account);
            byte[] balance = RLP.encodeBigInteger(this.balance);
            byte[] power = RLP.encodeBigInteger(this.power);

            rlpEncoded = RLP.encodeList(account, balance, power);
        }

        return rlpEncoded;
    }

    private void parseRLP(){
        if(isParse){
            return;
        } else {

            RLPList items = RLP.decode2(this.rlpEncoded);
            RLPList item = (RLPList)items.get(0);

            this.account = item.get(0).getRLPData();
            this.balance = new BigInteger(1, item.get(1).getRLPData());
            this.power = new BigInteger(1, item.get(2).getRLPData());

            isParse = true;
        }
    }

    public byte[] getAccount() {
        if(!isParse) parseRLP();
        return this.account;
    }


    public BigInteger getBalance() {
        if(!isParse) parseRLP();
        return this.balance;
    }

    public BigInteger getPower(){
        if(!isParse) parseRLP();
        return this.power;
    }

}
