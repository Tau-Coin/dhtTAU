package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;
import io.taucoin.param.ChainParam;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.ui.constant.Chain;

/**
 * 用户相关逻辑处理类
 */
public class UsersUtil {
    private static final int DEFAULT_NAME_LENGTH = 4;

    /**
     * 获取默认截取的名字
     * 规则：以T开头，然后取前3个数字
     * @param name 截取前的名字
     * @return 截取后的名字
     */
    public static String getDefaultName(String name) {
        StringBuilder defaultName = new StringBuilder();
        if(StringUtil.isNotEmpty(name)){
            defaultName.append("T");
            for (int i = 0; i < name.length(); i++) {
                if (Character.isDigit(name.charAt(i))) {
                    defaultName.append(name.charAt(i));
                    if(defaultName.length() == DEFAULT_NAME_LENGTH) {
                        break;
                    }
                }
            }
        }
        return defaultName.toString();
//        return StringUtil.subStringLater(name, DEFAULT_NAME_LENGTH);
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
            midHideName += getDefaultName(name);
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
        return getShowName(user.publicKey, user.localName);
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
            return getShowName(user.publicKey, user.localName);
        }
    }

    public static String getShowName(String publicKey, String localName) {
        String showName = UsersUtil.getDefaultName(publicKey);
        if(StringUtil.isNotEmpty(localName)
                && StringUtil.isNotEquals(localName.trim(), showName.trim())){
            Context context = MainApplication.getInstance();
            showName = context.getString(R.string.user_show_name, localName, showName);
        }
        return showName;
    }

    /**
     * 获取显示当前用户名字
     * @param user 当前用户
     * @return 显示名字
     */
    public static String getCurrentUserName(@NonNull User user) {
        if(StringUtil.isNotEmpty(user.localName)){
            return user.localName;
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
       return null;
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
}