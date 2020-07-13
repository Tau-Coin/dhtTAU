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

/**
 * txData contains transaction/msg/contract type and contract description code.
 */
public class TxData {
    private MsgType msgType;
    private byte[] txCode;

    private byte[] rlpEncoded;
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
        if (type != MsgType.RegularForum && type != MsgType.ForumComment && type != MsgType.DHTBootStrapNodeAnnouncement
            && type != MsgType.Wiring && type != MsgType.CommunityAnnouncement && type != MsgType.IdentityAnnouncement) {
            throw new IllegalArgumentException("illegal tx type");
        }
        if (txCode.length > ChainParam.ForumMsgLength){
            throw  new IllegalArgumentException("announcement size large than: "+ ChainParam.ForumMsgLength);
        }

        this.msgType = type;
        this.txCode = txCode;
        isParsed = true;
    }

    public TxData(byte[] rlpEncoded){
        this.rlpEncoded = rlpEncoded;
        isParsed = false;
    }

    /**
     * encode txdata.
     * @return
     */
    public byte[] getEncoded(){
        if(rlpEncoded == null) {
            byte[] msgtype = RLP.encodeByte(this.msgType.getVaLue());
            byte[] txcode = RLP.encodeElement(this.txCode);
            this.rlpEncoded = RLP.encodeList(msgtype,txcode);
        }
        return this.rlpEncoded;
    }

    /**
     * parse txdata.
     */
    private void parseRLP(){
        if(isParsed){
             return;
        }else{
            RLPList list = RLP.decode2(this.rlpEncoded);
            RLPList txdata = (RLPList) list.get(0);
            this.msgType = MsgType.setValue(txdata.get(0).getRLPData()==null?0:txdata.get(0).getRLPData()[0]);
            this.txCode = txdata.get(1).getRLPData();
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
        return this.txCode;
    }

    /**
     * get msg about note tx.
     * @return
     */
    public String getNoteMsg(){
        if(!isParsed) parseRLP();
        if(this.msgType == MsgType.RegularForum){
            if(!isInstance) {
                Note note = new Note(this.txCode);
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
               Comment comment = new Comment(this.txCode);
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
               Comment comment = new Comment(this.txCode);
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
                CommunityAnnouncement cma = new CommunityAnnouncement(this.txCode);
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
    public byte[] getCommunityAnnouncementGenesisPubkey(){
        if(!isParsed) parseRLP();
        if(this.msgType == MsgType.CommunityAnnouncement){
            if(!isInstance){
                CommunityAnnouncement cma = new CommunityAnnouncement(this.txCode);
                ob = cma;
                isInstance = true;
                return cma.getGenesisMinerPubkey();
            }else{
                CommunityAnnouncement cma = (CommunityAnnouncement)ob;
                return cma.getGenesisMinerPubkey();
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
                CommunityAnnouncement cma = new CommunityAnnouncement(this.txCode);
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
     * get chainid about NodeAnnouncement tx.
     */
    public byte[] getNodeAnnouncementChainID(){
        if(!isParsed) parseRLP();
        if(this.msgType == MsgType.DHTBootStrapNodeAnnouncement){
            if(!isInstance){
                DHTbootstrapNodeAnnouncement dsna = new DHTbootstrapNodeAnnouncement(this.txCode);
                ob = dsna;
                isInstance = true;
                return dsna.getChainID();
            }else{
                DHTbootstrapNodeAnnouncement dsna = (DHTbootstrapNodeAnnouncement)ob;
                return dsna.getChainID();
            }
        }
        return null;
    }

    /**
     * get bootnodes about NodeAnnouncement tx.
     */
    public String[] getNodeAnnouncementBootNodes(){
        if(!isParsed) parseRLP();
        if(this.msgType == MsgType.DHTBootStrapNodeAnnouncement){
            if(!isInstance){
                DHTbootstrapNodeAnnouncement dsna = new DHTbootstrapNodeAnnouncement(this.txCode);
                ob = dsna;
                isInstance = true;
                return dsna.getBootNodes();
            }else{
                DHTbootstrapNodeAnnouncement dsna = (DHTbootstrapNodeAnnouncement)ob;
                return dsna.getBootNodes();
            }
        }
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
                WireTransaction wtx = new WireTransaction(this.txCode);
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
                WireTransaction wtx = new WireTransaction(this.txCode);
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
     * get pubkey about IdentityAnnouncement tx.
     */
    public byte[] getIdentityAnnouncementPubkey(){
        if(!isParsed) parseRLP();
        if(this.msgType == MsgType.IdentityAnnouncement){
            if(!isInstance){
                IdentityAnnouncement ian = new IdentityAnnouncement(this.txCode);
                ob = ian;
                isInstance = true;
                return ian.getRenamePubkey();
            }else {
                IdentityAnnouncement ian  = (IdentityAnnouncement) ob;
                return ian.getRenamePubkey();
            }
        }
        return null;
    }

    /**
     * get new name about IdentityAnnouncement.
     */
    public String getIdentityAnnouncementNewName(){
        if(!isParsed) parseRLP();
        if(this.msgType == MsgType.IdentityAnnouncement){
            if(!isInstance){
                IdentityAnnouncement ian = new IdentityAnnouncement(this.txCode);
                ob = ian;
                isInstance = true;
                return ian.getNewName();
            }else {
                IdentityAnnouncement ian  = (IdentityAnnouncement) ob;
                return ian.getNewName();
            }
        }
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
            case RegularForum:
                Note note = new Note(this.txCode);
                retval = note.isValidateParamMsg();
                break;
            case ForumComment:
                Comment comment = new Comment(this.txCode);
                retval = comment.isValidateParamMsg();
                break;
            case CommunityAnnouncement:
                CommunityAnnouncement commAnnouncement = new CommunityAnnouncement(this.txCode);
                retval = commAnnouncement.isValidateParamMsg();
                break;
            case DHTBootStrapNodeAnnouncement:
                DHTbootstrapNodeAnnouncement dhtAnnouncement = new DHTbootstrapNodeAnnouncement(this.txCode);
                retval = dhtAnnouncement.isValidateParamMsg();
                break;
            case Wiring:
                WireTransaction wtx = new WireTransaction(this.txCode);
                retval = wtx.isValidateParamMsg();
                break;
            case IdentityAnnouncement:
                IdentityAnnouncement identityAnnouncement = new IdentityAnnouncement(this.txCode);
                retval = identityAnnouncement.isValidateParamMsg();
                break;
            default:
                retval = false;
        }
        return retval;
    }
}
