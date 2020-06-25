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

import io.taucoin.config.ChainConfig;
import io.taucoin.param.ChainParam;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

//txdata stands common parts of transaction
public class TxData {
    private MsgType msgType;
    private String annoucement;
    private String attachment;

    private byte[] rlpEncoded;
    private boolean isParse;

    /**
     * construct txdata.
     * @param type tx message type.
     * @param announcement Description of the magnet link, Receiver, BootStrapNode, Community Announcement.
     * @param attachment Magnet link, Amount, Nil of boostrapnoode and community announcement.
     */
    public TxData (MsgType type,String announcement,String attachment){
        if (type != MsgType.TorrentPublish && type != MsgType.BootStrapNodeAnnouncement
            && type != MsgType.Wiring && type != MsgType.CommunityAnnouncement ) {
            throw new IllegalArgumentException("illegal tx type");
        }
        if (announcement.getBytes().length >= ChainParam.AnnounceLength){
            throw  new IllegalArgumentException("announcement size large than: "+ ChainParam.AnnounceLength);
        }
        if (attachment.getBytes().length >= ChainParam.AttachLength){
            throw new IllegalArgumentException("attachment size large than: "+ ChainParam.AttachLength);
        }

        this.msgType = type;
        this.annoucement = announcement;
        this.attachment = attachment;
        isParse = true;
    }

    public TxData(byte[] rlpEncoded){
        this.rlpEncoded = rlpEncoded;
        isParse = false;
    }

    /**
     * encode txdata.
     * @return
     */
    public byte[] getEncoded(){
        if(rlpEncoded == null) {
            byte[] msgtype = RLP.encodeByte(this.msgType.getVaLue());
            byte[] announcement = RLP.encodeString(this.annoucement);
            byte[] attachment = RLP.encodeString(this.attachment);
            this.rlpEncoded = RLP.encodeList(msgtype,announcement,attachment);
        }
        return this.rlpEncoded;
    }

    /**
     * parse txdata.
     */
    private void parseRLP(){
        if(isParse){
             return;
        }else{
            RLPList list = RLP.decode2(this.rlpEncoded);
            RLPList txdata = (RLPList) list.get(0);
            this.msgType = MsgType.setValue(txdata.get(0).getRLPData()==null?0:txdata.get(0).getRLPData()[0]);
            this.annoucement = new String(txdata.get(1).getRLPData());
            this.attachment = new String(txdata.get(2).getRLPData());
            isParse = true;
        }
    }

    /**
     * msgtype: TorrentPublish(0),Wiring(1),BootStrapNodeAnnouncement(2),CommunityAnnouncement(3);
     * @return
     */
    public MsgType getMsgType(){
        if(!isParse) parseRLP();
        return this.msgType;
    }

    /**
     * get announcement.
     * @return
     */
    public String getAnnoucement(){
        if(!isParse) parseRLP();
        return this.annoucement;
    }

    /**
     * get receiver about wire tx.
     * @return
     */
    public byte[] getReceiver(){
        if(!isParse) parseRLP();
        if(this.msgType == MsgType.Wiring){
            return this.annoucement.getBytes();
        }
        return null;
    }

    /**
     * get attachment.
     * @return
     */
    public String getAttachment(){
        if(!isParse) parseRLP();
        return this.attachment;
    }
}
