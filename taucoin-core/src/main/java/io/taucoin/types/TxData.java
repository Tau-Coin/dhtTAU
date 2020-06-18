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

    public byte[] getEncoded(){
        if(rlpEncoded == null) {
            byte[] msgtype = RLP.encodeByte(this.msgType.getVaLue());
            byte[] announcement = RLP.encodeString(this.annoucement);
            byte[] attachment = RLP.encodeString(this.attachment);
            this.rlpEncoded = RLP.encodeList(msgtype,announcement,attachment);
        }
        return this.rlpEncoded;
    }

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

    public MsgType getMsgType(){
        if(!isParse) parseRLP();
        return this.msgType;
    }

    public String getAnnoucement(){
        if(!isParse) parseRLP();
        return this.annoucement;
    }

    public String getAttachment(){
        if(!isParse) parseRLP();
        return this.attachment;
    }
}
