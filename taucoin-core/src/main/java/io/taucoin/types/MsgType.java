package io.taucoin.types;

//transaction type used to create transaction.
public enum MsgType {
    TorrentPublish(0),
    Wiring(1),
    BootStrapNodeAnnouncement(2),
    CommunityAnnouncement(3);
    int index;
    MsgType(int value){
        this.index = value;
    }
    public byte getVaLue(){
       return (byte)index;
    }
    public static MsgType setValue(byte value){
        if (value == 0){
            return TorrentPublish;
        }else if (value == 1){
            return Wiring;
        }else if (value == 2){
            return BootStrapNodeAnnouncement;
        }else if (value == 3){
            return CommunityAnnouncement;
        }
        return null;
    }
}
