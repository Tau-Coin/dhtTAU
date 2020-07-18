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
    private String name;
    private String description;

    private byte[] rlpEncode;
    private boolean isParsed;

    /**
     * construct Identity Announcement from user devices.
     * @param name
     * @param description
     */
    public IdentityAnnouncement(String name,String description){
        this.name = name;
        this.description = description;
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
            byte[] name = RLP.encodeString(this.name);
            byte[] description = RLP.encodeString(this.description);
            this.rlpEncode = RLP.encodeList(name,description);
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
            this.name = new String(IDannouncement.get(0).getRLPData());
            this.description = new String(IDannouncement.get(1).getRLPData());
            isParsed = true;
        }
    }

    /**
     * get name.
     * @return
     */
    public String getName(){
        if(!isParsed) parseRLP();
        return this.name;
    }

    /**
     * get newName;
     * @return
     */
    public String getDescription(){
        if(!isParsed) parseRLP();
        return this.description;
    }

    /**
     * validate object parameters.
     * @return
     */
    public boolean isValidateParamMsg(){
        if(!isParsed) parseRLP();
        if(this.name.getBytes().length > ChainParam.IDnameLength) return false;
        if(this.description.getBytes().length > ChainParam.IDdescLength) return false;
        return true;
    }
}
