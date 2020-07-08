/**
 * Copyright 2020 taucoin developer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
 * SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.taucoin.types;

import io.taucoin.param.ChainParam;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

/**
 * community announcement transaction code used to announcement a community in b community.
 */
public class CommunityAnnouncement {
    private byte[] chainID;
    private byte[] genesisMinerPubkey;
    private String description;

    private byte[] rlpEncode;
    private boolean isParsed;

    /**
     * construct community announcement message from user device.
     */
    public CommunityAnnouncement(byte[] chainID,byte[] genesisMinerPubkey,String description){
        this.chainID = chainID;
        this.genesisMinerPubkey = genesisMinerPubkey;
        this.description = description;
        this.isParsed = true;
    }

    /**
     * construct community announcement message from txData object.
     * @param rlpEncode
     */
    public CommunityAnnouncement(byte[] rlpEncode){
        this.rlpEncode = rlpEncode;
        this.isParsed = false;
    }

    /**
     * encode community announcement into byte code.
     * @return
     */
    public byte[] getEncode(){
       if(rlpEncode == null) {
           byte[] chainid = RLP.encodeElement(this.chainID);
           byte[] genesisminerpubkey = RLP.encodeElement(this.genesisMinerPubkey);
           byte[] description = RLP.encodeString(this.description);

           this.rlpEncode = RLP.encodeList(chainid,genesisminerpubkey,description);
       }
       return rlpEncode;
    }

    /**
     * parse encoded community announcement into flat message.
     */
    public void parseRLP(){
        if(isParsed){
            return;
        }else {
            RLPList list = RLP.decode2(this.rlpEncode);
            RLPList commannounce = (RLPList) list.get(0);
            this.chainID = commannounce.get(0).getRLPData();
            this.genesisMinerPubkey = commannounce.get(1).getRLPData();
            this.description = new String(commannounce.get(2).getRLPData());
            isParsed = true;
        }
    }

    /**
     * get announced chainID that others will follow.
     * @return
     */
    public byte[] getChainID(){
        if(!isParsed) parseRLP();
        return this.chainID;
    }

    /**
     * get miner pubkey that dht peers can use as communication slot.
     * @return
     */
    public byte[] getGenesisMinerPubkey(){
        if(!isParsed) parseRLP();
        return this.genesisMinerPubkey;
    }

    /**
     * get description to this community.
     * @return
     */
    public String getDescription(){
        if(!isParsed) parseRLP();
        return this.description;
    }

    /**
     * validate community announcement message parameters.
     * @return
     */
    public boolean isValidateParamMsg(){
        if(!isParsed) parseRLP();
        if(this.chainID.length > ChainParam.ChainIDlength) return false;
        if(this.genesisMinerPubkey.length != ChainParam.PubkeyLength) return false;
        if(this.description.getBytes().length > ChainParam.DescriptionLength) return false;
        return true;
    }
}
