package io.taucoin.torrent.publishing.core.utils;

import androidx.annotation.NonNull;
import io.taucoin.param.ChainParam;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;

/**
 * 用户相关逻辑处理类
 */
public class UsersUtil {
    private static final int DEFAULT_NAME_LENGTH = 4;
    private static final int QR_NAME_LENGTH = 3;

    /**
     * 获取默认截取的名字
     * 规则：以T开头，然后取前3个数字
     * @param name 截取前的名字
     * @return 截取后的名字
     */
    public static String getDefaultName(String name) {
        StringBuilder defaultName = new StringBuilder();
        if(StringUtil.isNotEmpty(name)){
            for (int i = 0; i < name.length(); i++) {
                if (!Character.isDigit(name.charAt(i))) {
                    defaultName.append(name.charAt(i));
                    break;
                }
            }
            for (int i = 0; i < name.length(); i++) {
                if (Character.isDigit(name.charAt(i))) {
                    defaultName.append(name.charAt(i));
                    if(defaultName.length() == DEFAULT_NAME_LENGTH) {
                        break;
                    }
                }
            }
        }
        return defaultName.toString().toUpperCase();
    }

    /**
     * 获取中间隐藏的名字
     * @param name 隐藏前的名字
     * @return 隐藏后的名字
     */
    public static String getMidHideName(String name) {
        if(StringUtil.isNotEmpty(name) && name.length() > 11){
            String midHideName = name.substring(0, 3);
            midHideName += "***";
            midHideName += name.substring(name.length() - 8);
            return midHideName;
        }
        return name;
    }

    /**
     * 获取显示名字
     * @param user 当前用户
     * @return 显示名字
     */
    public static String getShowName(@NonNull User user) {
        return getShowName(user.publicKey, user.nickname);
    }

    public static String getShowNameWithYourself(User user, String publicKey) {
        String showName = getShowName(user, publicKey);
        if (StringUtil.isEquals(publicKey, MainApplication.getInstance().getPublicKey())) {
            showName += MainApplication.getInstance().getString(R.string.contacts_yourself);
        }
        return showName;
    }

    /**
     * 获取显示名字
     * @param user 当前用户
     * @return 显示名字
     */
    public static String getShowName(User user, String publicKey) {
        if(null == user){
            return UsersUtil.getDefaultName(publicKey);
        }else{
            return getShowName(user.publicKey, user.nickname);
        }
    }

    public static String getShowName(String publicKey, String localName) {
        String showName = UsersUtil.getDefaultName(publicKey);
        if(StringUtil.isNotEmpty(localName)){
//                && StringUtil.isNotEquals(localName.trim(), showName.trim())){
//            Context context = MainApplication.getInstance();
//            showName = context.getString(R.string.user_show_name, localName, showName);
            showName = localName;
        }
        return showName;
    }

    /**
     * 获取显示当前用户名字
     * @param user 当前用户
     * @return 显示名字
     */
    public static String getCurrentUserName(@NonNull User user) {
        if(StringUtil.isNotEmpty(user.nickname)){
            return user.nickname;
        }else{
            return UsersUtil.getDefaultName(user.publicKey);
        }
    }

    public static String getUserName(User user, String publicKey) {
        if(user != null){
            return getCurrentUserName(user);
        }else{
            return UsersUtil.getDefaultName(publicKey);
        }
    }

    /**
     * 获取显示coin name
     * @param chainID 当前社区ID
     * @return 显示名字
     */
    public static String getCoinName(String chainID) {
        String communityName = getCommunityName(chainID);
        String firstLetters = StringUtil.getFirstLettersOfName(communityName);
        return firstLetters + "coin";
    }

    /**
     * 获取社区名
     * @param chainID 链的chainID
     * @return 社区名
     */
    public static String getCommunityName(@NonNull String chainID) {
       if(StringUtil.isNotEmpty(chainID)){
           String[] splits = chainID.split(ChainParam.ChainidDelimeter);
           if(splits.length > 1){
               return splits[0];
           }
       }
       return "";
    }

    /**
     * 获取balance的显示
     * @param balance 余额
     * @return 余额显示
     */
    public static String getShowBalance(long balance) {
        balance = FmtMicrometer.fmtAmount(balance);
        if (balance >= 1000000) {
            return (int)(balance / 1000000) + "m";
        } else if(balance >= 1000) {
            return (int)(balance / 1000) + "k";
        } else {
            return String.valueOf((int)balance);
        }
    }

    /**
     * 获取二维码上图片的名字
     * @param name
     * @return
     */
    public static String getQRCodeName(String name) {
        if (StringUtil.isNotEmpty(name) && name.length() > QR_NAME_LENGTH) {
            return name.substring(0, QR_NAME_LENGTH);
        }
        return name;
    }
}