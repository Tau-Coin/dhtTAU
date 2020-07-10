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

/**
 * transaction code about regular forum.
 */
public class Note {
    private String forumMsg;
    private byte[] byteMsg;
    private boolean isParsed;

    /**
     * construct forum msg from user device.
     * @param forumMsg
     */
    public Note(String forumMsg){
        this.forumMsg = forumMsg;
        isParsed = true;
    }

    /**
     * construct forum msg from TxData.
     * in TxData this is contract code.
     * @param forumMsg
     */
    public Note(byte[] forumMsg){
       this.byteMsg = forumMsg;
    }

    /**
     * validate object param size.
     * @return
     */
    public boolean isValidateParamMsg(){
        if(forumMsg != null && forumMsg.getBytes().length > ChainParam.ForumMsgLength){
            return false;
        }
        if(byteMsg!= null && byteMsg.length > ChainParam.ForumMsgLength){
            return false;
        }
        return true;
    }

    /**
     * encode forumMsg into msg code.
     * @return
     */
    public byte[] getTxCode(){
        if(byteMsg == null){
            byteMsg = this.forumMsg.getBytes();
        }
        return byteMsg;
    }

    /**
     * parse tx bytes code into flat msg
     */
    public void parseTxCode(){
        if(!isParsed){
            forumMsg = new String(this.byteMsg);
        }
    }

    /**
     * get forum message.
     * @return
     */
    public String getForumMsg(){
        if(!isParsed) parseTxCode();
        return this.forumMsg;
    }
}
