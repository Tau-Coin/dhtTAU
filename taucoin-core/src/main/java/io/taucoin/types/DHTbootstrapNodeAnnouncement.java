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
 * DHT bootstrap node Announcement message.
 */
public class DHTbootstrapNodeAnnouncement {
    private byte[] chainID;
    private String[] bootNodes;

    private byte[] rlpEncode;
    private boolean isParsed;

    /**
     * construct dht bootstrap node announcement from user devices.
     * @param chainID
     * @param bootNodes
     */
    public DHTbootstrapNodeAnnouncement(byte[] chainID,String[] bootNodes){
        this.chainID = chainID;
        this.bootNodes = bootNodes;
        isParsed = true;
    }

    /**
     * construct dht bootstrap node announcement in txData.
     * @param rlpEncode
     */
    public DHTbootstrapNodeAnnouncement(byte[] rlpEncode){
        this.rlpEncode = rlpEncode;
        isParsed = false;
    }

    /**
     * encode DHT bootstrap node announcement into byte code.
     * @return
     */
    public byte[] getEncode(){
      if(rlpEncode == null){
          byte[] chainid = RLP.encodeElement(this.chainID);
          byte[][] bootnodes = new byte[bootNodes.length + 1][];
          bootnodes[0] = chainid;
          for(int i = 0;i<bootNodes.length;i++){
              bootnodes[i+1] = RLP.encodeString(this.bootNodes[i]);
          }
          this.rlpEncode = RLP.encodeList(bootnodes);
      }
      return rlpEncode;
    }

    /**
     * parse encoded DHTbootstrap nodes into flat message.
     */
    public void parseRLP(){
        if(isParsed){
            return;
        }else{
            RLPList list = RLP.decode2(this.rlpEncode);
            RLPList bootnodes = (RLPList) list.get(0);
            this.chainID = bootnodes.get(0).getRLPData();
            int size = bootnodes.size();
            this.bootNodes = new String[size -1];
            for(int i=0; i < size -1;i++){
                this.bootNodes[i] = new String(bootnodes.get(i+1).getRLPData());
            }
            isParsed = true;
        }
    }

    /**
     * get chainid in txData object.
     * @return
     */
    public byte[] getChainID(){
        if(!isParsed) parseRLP();
        return this.chainID;
    }

    /**
     * get bootnodes in txData object.
     * @return
     */
    public String[] getBootNodes(){
        if(!isParsed) parseRLP();
        return this.bootNodes;
    }

    /**
     * validate object parameters.
     * @return
     */
    public boolean isValidateParamMsg(){
        if(!isParsed) parseRLP();
        if(this.chainID.length > ChainParam.ChainIDlength) return false;
        if(this.bootNodes.length > ChainParam.MaxBootNodesAnnouncement) return false;
        return true;
    }
}
