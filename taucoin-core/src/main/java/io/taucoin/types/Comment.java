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
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class Comment {
    /**
     * @param reference: reference block hash.
     * @param comment:your option about reference history message.
     */
    private byte[] reference;
    private String comment;

    private byte[]  rlpEncode;
    private boolean isParsed;

    /**
     * construct from user device.
     * @param reference
     * @param comment
     */
    public Comment(byte[] reference, String comment){
        this.reference = reference;
        this.comment = comment;
        isParsed = true;
    }

    /**
     * construct forum comment from Txdata.
     * in TxData this is contract code.
     * @param rlpEncode
     */
    public Comment(byte[] rlpEncode){
        this.rlpEncode = rlpEncode;
        isParsed = false;
    }

    /**
     * encode forum comment into bytes.
     * @return
     */
    public byte[] getEncode(){
        if(rlpEncode == null){
            byte[] reference = RLP.encodeElement(this.reference);
            byte[] comment = RLP.encodeString(this.comment);
            this.rlpEncode = RLP.encodeList(reference,comment);
        }
        return rlpEncode;
    }

    /**
     * parse encode forum comment into flat message.
     */
    public void parseRLP(){
      if(isParsed){
          return;
      }else {
          RLPList list = RLP.decode2(this.rlpEncode);
          RLPList fmt = (RLPList) list.get(0);
          this.reference = fmt.get(0).getRLPData();
          this.comment = new String(fmt.get(1).getRLPData());
          isParsed = true;
      }
    }

    /**
     * get comment reference hash.
     * @return
     */
    public byte[] getReference(){
        if(!isParsed) parseRLP();
        return this.reference;
    }

    /**
     * get comment content.
     * @return
     */
    public String getComment(){
        if(!isParsed) parseRLP();
        return this.comment;
    }

    /**
     * validate forum param.
     * @return
     */
    public boolean isValidateParamMsg(){
        if(!isParsed) parseRLP();
        if(this.reference.length != ChainParam.HashLength || this.comment.getBytes().length > ChainParam.DescriptionLength){
            return false;
        }
        return true;
    }

}
