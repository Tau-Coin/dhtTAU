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
package io.taucoin.utypes;

import com.frostwire.jlibtorrent.Entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.taucoin.genesis.CheckInfo;
import io.taucoin.genesis.GenesisItem;
import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

/**
 * txData contains transaction/msg/contract type and contract description code.
 */
public class TxData {
    private MsgType msgType;
    //1 means txCode last byte is align
    private byte lastAlign = 1;
    private ArrayList<Long> txCode = new ArrayList<>();

    private byte[] bEncoded;
    private boolean isParsed;

    //boost internal object operating.
    private boolean isInstance = false;
    private Object ob;

    /**
     * construct txdata.
     * @param type tx message type.
     * @param txCode Description of txdata.
     */
    public TxData (MsgType type,byte[] txCode){
        if (type != MsgType.GenesisMsg && type != MsgType.RegularForum && type != MsgType.ForumComment && type != MsgType.DHTBootStrapNodeAnnouncement
            && type != MsgType.Wiring && type != MsgType.CommunityAnnouncement && type != MsgType.IdentityAnnouncement) {
            throw new IllegalArgumentException("illegal tx type");
        }
        if (txCode.length > ChainParam.ForumMsgLength){
            throw  new IllegalArgumentException("announcement size large than: "+ ChainParam.ForumMsgLength);
        }

        this.msgType = type;
        int pieces;
        boolean isAlign = true;
        if(txCode.length % 8 == 0){
            pieces = txCode.length/8;
        }else {
            pieces = txCode.length/8 + 1;
            isAlign = false;
        }
//        System.out.println("pieces is : "+pieces);
        if(isAlign) {
            this.txCode = ByteUtil.byteArrayToSignLongArray(txCode, pieces);
        }else{
//            ArrayList<Long> temp = ByteUtil.unAlignByteArrayToSignLongArray(txCode,pieces);
//            for(int i=0; i < temp.size();++i){
//                System.out.println(temp.get(i));
//                if(i < temp.size()-1){
//                    System.out.println(ByteUtil.toHexString(ByteUtil.longToBytes(temp.get(i))));
//                }else{
//                    System.out.println(ByteUtil.toHexString(ByteUtil.longToBytesNoLeadZeroes(temp.get(i))));
//                }
//                this.txCode.add(temp.get(i));
//            }
            this.lastAlign = 0;
            this.txCode = ByteUtil.unAlignByteArrayToSignLongArray(txCode,pieces);
        }
        isParsed = true;
    }

    public TxData(byte[] bEncoded){
        this.bEncoded = bEncoded;
        isParsed = false;
    }

    /**
     * encode txdata.
     * @return
     */
    public byte[] getEncoded(){
        if(bEncoded == null) {
            List list = new ArrayList();
            list.add((long)this.msgType.getVaLue());
            list.add((long)this.lastAlign);
            list.add(this.txCode);
            Entry entry = Entry.fromList(list);
            this.bEncoded = entry.bencode();
        }
        return this.bEncoded;
    }

    /**
     * parse txdata.
     */
    private void parseRLP(){
        if(isParsed){
             return;
        }else{
            Entry entry = Entry.bdecode(this.bEncoded);
            List entrylist = entry.list();
            this.msgType = MsgType.setValue(ByteUtil.stringToByte(entrylist.get(0).toString()));
            this.lastAlign = ByteUtil.stringToByte(entrylist.get(1).toString());
            this.txCode = ByteUtil.stringToArrayList((entrylist.get(2)).toString());
            isParsed = true;
        }
    }

    /**
     * msgtype: TorrentPublish(0),Wiring(1),BootStrapNodeAnnouncement(2),CommunityAnnouncement(3);
     * @return
     */
    public MsgType getMsgType(){
        if(!isParsed) parseRLP();
        return this.msgType;
    }

    /**
     * get txCode.
     * @return
     */
    public byte[] getTxCode(){
        if(!isParsed) parseRLP();
        int size = this.txCode.size();
        byte[] txCodeBytes = new byte[size*8];
        byte[] longbyte;
        for(int i=0;i<size;++i){
            if(lastAlign == 0){
                if(i < size -1){
                    longbyte = ByteUtil.longToBytes(txCode.get(i));
                    System.arraycopy(longbyte,0,txCodeBytes,8*i,8);
                }else{
                    longbyte = ByteUtil.longToBytesNoLeadZeroes(txCode.get(i));
                    System.arraycopy(longbyte,0,txCodeBytes,8*i,longbyte.length);
                }
            }else {
                longbyte = ByteUtil.longToBytes(txCode.get(i));
                System.arraycopy(longbyte, 0, txCodeBytes, 8 * i, 8);
            }
        }
        return txCodeBytes;
    }

    /**
     * get msg about note tx.
     * @return
     */
    public String getNoteMsg(){
        if(!isParsed) parseRLP();
        if(this.msgType == MsgType.RegularForum){
            if(!isInstance) {
                Note note = new Note(getTxCode());
                ob = note;
                isInstance = true;
                return note.getForumMsg();
            }else {
                Note note = (Note) ob;
                return note.getForumMsg();
            }
        }
        return "";
    }

    /**
     * get reference about Comment tx;
     */
    public byte[] getCommentReference(){
        if(!isParsed) parseRLP();
        if(this.msgType == MsgType.ForumComment){
           if(!isInstance){
               Comment comment = new Comment(getTxCode());
               ob = comment;
               isInstance = true;
               return comment.getReference();
           }else {
               Comment comment = (Comment)ob;
               return comment.getReference();
           }
        }
        return null;
    }

    /**
     * get comment message about Comment tx
     */
    public String getCommentMsg(){
        if(!isParsed) parseRLP();
        if(this.msgType == MsgType.ForumComment){
           if(!isInstance){
               Comment comment = new Comment(getTxCode());
               ob = comment;
               isInstance = true;
               return comment.getComment();
           }else {
               Comment comment = (Comment)ob;
               return comment.getComment();
           }
        }
        return "";
    }

    /**
     * get chainid about CommunityAnnouncement tx.
     */
    public byte[] getCommunityAnnouncementChainID(){
        if(!isParsed) parseRLP();
        if(this.msgType == MsgType.CommunityAnnouncement){
            if(!isInstance) {
                CommunityAnnouncement cma = new CommunityAnnouncement(getTxCode());
                ob = cma;
                isInstance = true;
                return cma.getChainID();
            }else{
                CommunityAnnouncement cma = (CommunityAnnouncement)ob;
                return cma.getChainID();
            }
        }
        return null;
    }

    /**
     * get genesispubkey about CommunityAnnouncement tx.
     */
    public byte[] getCommunityAnnouncementBootstrapPubkey(){
        if(!isParsed) parseRLP();
        if(this.msgType == MsgType.CommunityAnnouncement){
            if(!isInstance){
                CommunityAnnouncement cma = new CommunityAnnouncement(getTxCode());
                ob = cma;
                isInstance = true;
                return cma.getBootstrapPks();
            }else{
                CommunityAnnouncement cma = (CommunityAnnouncement)ob;
                return cma.getBootstrapPks();
            }
        }
        return null;
    }

    /**
     * get community description about CommunityAnnouncement tx.
     */
    public String getCommunityAnnouncementDescription(){
        if(!isParsed) parseRLP();
        if(this.msgType == MsgType.CommunityAnnouncement){
            if(!isInstance){
                CommunityAnnouncement cma = new CommunityAnnouncement(getTxCode());
                ob = cma;
                isInstance = true;
                return cma.getDescription();
            }else{
                CommunityAnnouncement cma = (CommunityAnnouncement)ob;
                return cma.getDescription();
            }
        }
        return "";
    }

    /**
     * get genesis message description.
     * @return
     */
    public String getGenesisMsgDescription(){
        if(!isParsed) parseRLP();
        if(this.msgType == MsgType.GenesisMsg){
            if(!isInstance){
                GenesisMsg msg = new GenesisMsg(getTxCode());
                ob = msg;
                isInstance = true;
                return msg.getDescription();
            }else {
                GenesisMsg msg = (GenesisMsg)ob;
                return msg.getDescription();
            }
        }
        return null;
    }

    /**
     * get genesis message K-V state.
     * @return
     */
    public Map<String, GenesisItem> getGenesisMsgKV(){
        if(!isParsed) parseRLP();
        if(this.msgType == MsgType.GenesisMsg){
            if(!isInstance){
                GenesisMsg msg = new GenesisMsg(getTxCode());
                ob = msg;
                isInstance = true;
                return msg.getAccountKV();
            }else {
                GenesisMsg msg = (GenesisMsg)ob;
                return msg.getAccountKV();
            }
        }
        return null;
    }

    /**
     * get chainid about NodeAnnouncement tx.
     */
    public byte[] getNodeAnnouncementChainID(){
        if(!isParsed) parseRLP();
        return null;
    }

    /**
     * get bootnodes about NodeAnnouncement tx.
     */
    public String[] getNodeAnnouncementBootNodes(){
        if(!isParsed) parseRLP();
        return null;
    }

    /**
     * get receiver about wire tx.
     * @return
     */
    public byte[] getReceiver(){
        if(!isParsed) parseRLP();
        if(this.msgType == MsgType.Wiring){
            if(!isInstance) {
                WireTransaction wtx = new WireTransaction(getTxCode());
                ob = wtx;
                isInstance = true;
                return wtx.getReceiverPubkey();
            }else {
                WireTransaction wtx = (WireTransaction)ob;
                return wtx.getReceiverPubkey();
            }
        }
        return null;
    }

    /**
     * get wire amount.
     * @return
     */
    public long getAmount(){
        if(!isParsed) parseRLP();
        if(this.msgType == MsgType.Wiring){
            if(!isInstance) {
                WireTransaction wtx = new WireTransaction(getTxCode());
                ob = wtx;
                isInstance = true;
                return wtx.getAmount();
            }else{
                WireTransaction wtx = (WireTransaction)ob;
                return wtx.getAmount();
            }
        }
        return 0;
    }

    /**
     * get name about IdentityAnnouncement tx.
     */
    public String getIdentityAnnouncementName(){
        if(!isParsed) parseRLP();
        return null;
    }

    /**
     * get new name about IdentityAnnouncement.
     */
    public String getIdentityAnnouncementDescription(){
        if(!isParsed) parseRLP();
        return "";
    }

    /**
     * validate txData(message type and txCode)
     * @return
     */
    public boolean isTxDataValidate(){
        if(!isParsed) parseRLP();
        boolean retval;
        switch (msgType){
            case GenesisMsg:
                GenesisMsg msg = new GenesisMsg(getTxCode());
                retval = (msg.validateGenesisMsg() == CheckInfo.CheckPassed);
                break;
            case RegularForum:
                Note note = new Note(getTxCode());
                retval = note.isValidateParamMsg();
                break;
            case ForumComment:
                Comment comment = new Comment(getTxCode());
                retval = comment.isValidateParamMsg();
                break;
            case CommunityAnnouncement:
                CommunityAnnouncement commAnnouncement = new CommunityAnnouncement(getTxCode());
                retval = commAnnouncement.isValidateParamMsg();
                break;
            case DHTBootStrapNodeAnnouncement:
                retval = false;
                break;
            case Wiring:
                WireTransaction wtx = new WireTransaction(getTxCode());
                retval = wtx.isValidateParamMsg();
                break;
            case IdentityAnnouncement:
                retval = false;
                break;
            default:
                retval = false;
        }
        return retval;
    }
}
