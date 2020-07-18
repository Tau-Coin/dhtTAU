package io.taucoin.torrent.publishing.core.utils;

/**
 * 联系方式平台类型
 */
public enum PlatformType {
    Telegram( 0),
    WhatsApp(1),
    Facebook(2),
    WeChat(3),
    Others(4);

    private int code;
    PlatformType(int code){
        this.code = code;
    }

    public int getCode(){
        return code;
    }

    public static PlatformType getPlatformType(int code){
        for(PlatformType type : PlatformType.values()){
            if(type.getCode() == code){
                return type;
            }
        }
        return null;
    }
}
