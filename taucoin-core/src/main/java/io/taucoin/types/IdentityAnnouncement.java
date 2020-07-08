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
 * peer Identity Announcement.
 */
public class IdentityAnnouncement {
    private byte[] renamePubkey;
    private String newName;

    private byte[] rlpEncode;
    private boolean isParsed;

    /**
     * construct Identity Announcement from user devices.
     * @param renamePubkey
     * @param newName
     */
    public IdentityAnnouncement(byte[] renamePubkey,String newName){
        this.renamePubkey = renamePubkey;
        this.newName = newName;
        isParsed = true;
    }

    /**
     * construct Identity Announcement in txData.
     * @param rlpEncode
     */
    public IdentityAnnouncement(byte[] rlpEncode){
        this.rlpEncode = rlpEncode;
        isParsed = false;
    }

    /**
     * encode IdentityAnnouncement into txCode.
     * @return
     */
    public byte[] getEncode(){
        if(this.rlpEncode == null){
            byte[] pubkey = RLP.encodeElement(this.renamePubkey);
            byte[] name = RLP.encodeString(this.newName);
            this.rlpEncode = RLP.encodeList(pubkey,name);
        }
        return this.rlpEncode;
    }

    /**
     * parse encode byte into flat IdentityAnnouncement.
     */
    public void parseRLP(){
        if(isParsed){
            return;
        }else {
            RLPList list = RLP.decode2(this.rlpEncode);
            RLPList IDannouncement = (RLPList) list.get(0);
            this.renamePubkey = IDannouncement.get(0).getRLPData();
            this.newName = new String(IDannouncement.get(1).getRLPData());
            isParsed = true;
        }
    }

    /**
     * get reference renamepubkey.
     * @return
     */
    public byte[] getRenamePubkey(){
        if(!isParsed) parseRLP();
        return this.renamePubkey;
    }

    /**
     * get newName;
     * @return
     */
    public String getNewName(){
        if(!isParsed) parseRLP();
        return this.newName;
    }

    /**
     * validate object parameters.
     * @return
     */
    public boolean isValidateParamMsg(){
        if(!isParsed) parseRLP();
        if(this.renamePubkey.length != ChainParam.PubkeyLength) return false;
        if(this.newName.getBytes().length > ChainParam.NewNameLength) return false;
        return true;
    }
}
