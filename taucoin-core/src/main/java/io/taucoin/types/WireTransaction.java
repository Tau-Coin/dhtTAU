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
package io.taucoin.types;

import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

/**
 * wire transaction
 */
public class WireTransaction {
    private byte[] receiverPubkey;
    private long amount;
    private String notes;

    private byte[] rlpEncode;
    private boolean isParsed;

    /**
     * construct wire transaction from user device.
     * @param receiverPubkey
     * @param amount
     */
    public WireTransaction(byte[] receiverPubkey,long amount,String notes){
        this.receiverPubkey = receiverPubkey;
        this.amount = amount;
        this.notes = notes;
        this.isParsed = true;
    }

    /**
     * construct wire transaction from txData object.
     * @param rlpEncode
     */
    public WireTransaction(byte[] rlpEncode){
        this.rlpEncode = rlpEncode;
        this.isParsed = false;
    }

    /**
     * encode wire transaction into bytes.
     * @return
     */
    public byte[] getEncode(){
        if(rlpEncode == null){
            byte[] receiverpub = RLP.encodeElement(this.receiverPubkey);
            byte[] amount = RLP.encodeElement(ByteUtil.longToBytes(this.amount));
            byte[] notes = RLP.encodeString(this.notes);
            this.rlpEncode = RLP.encodeList(receiverpub,amount,notes);
        }
        return rlpEncode;
    }

    /**
     * parse encode wire transaction into flat information.
     */
    public void parseRLP(){
        if(isParsed){
            return;
        }else {
            RLPList list = RLP.decode2(this.rlpEncode);
            RLPList wtx = (RLPList) list.get(0);
            this.receiverPubkey = wtx.get(0).getRLPData();
            this.amount = ByteUtil.byteArrayToLong(wtx.get(1).getRLPData());
            this.notes = new String(wtx.get(2).getRLPData());
            isParsed = true;
        }
    }

    /**
     * get receiver pubkey in txData object.
     * @return
     */
    public byte[] getReceiverPubkey(){
        if(!isParsed) parseRLP();
        return this.receiverPubkey;
    }

    /**
     * get wire amount.
     * @return
     */
    public long getAmount(){
        if(!isParsed) parseRLP();
        return this.amount;
    }

    /**
     * get Notes about this wire transaction.
     * @return
     */
    public String getNotes(){
        if(!isParsed) parseRLP();
        return this.notes;
    }

    /**
     * validate object param.
     * @return
     */
    public boolean isValidateWireMsg(){
        if(!isParsed) parseRLP();
        if(this.receiverPubkey.length != ChainParam.PubkeyLength) return false;
        if(this.amount <0) return false;
        if(this.notes.getBytes().length > ChainParam.WireNoteLength) return false;

        return true;
    }
}
