package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;
import io.taucoin.param.ChainParam;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.MsgAndReply;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;

/**
 * 用户相关逻辑处理类
 */
public class UsersUtil {
    private static final int DEFAULT_NAME_LENGTH = 8;

    /**
     * 获取默认截取的名字
     * @param name 截取前的名字
     * @return 截取后的名字
     */
    public static String getDefaultName(String name){
        return StringUtil.subStringLater(name, DEFAULT_NAME_LENGTH);
    }

    /**
     * 获取中国隐藏的名字
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
     * @param tx 当前交易
     * @return 显示名字
     */
    public static String getShowName(@NonNull UserAndTx tx) {
        if(tx.sender != null && StringUtil.isNotEmpty(tx.sender.localName)){
            return getShowName(tx.senderPk, tx.sender.localName);
        }else{
            return getShowName(tx.senderPk, null);
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

    public static String getUserName(@NonNull UserAndTx tx) {
        if(tx.sender != null && StringUtil.isNotEmpty(tx.sender.localName)){
            return tx.sender.localName;
        }else{
            return UsersUtil.getDefaultName(tx.senderPk);
        }
    }

    public static String getUserName(@NonNull MsgAndReply msg) {
        if(msg.sender != null && StringUtil.isNotEmpty(msg.sender.localName)){
            return msg.sender.localName;
        }else{
            return UsersUtil.getDefaultName(msg.senderPk);
        }
    }

    public static String getShowName(@NonNull MsgAndReply msg) {
        if(msg.sender != null){
            return getShowName(msg.sender);
        }else{
            return UsersUtil.getDefaultName(msg.senderPk);
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
     * 获取社区邀请链接
     * @param chainID 链的chainID
     * @param publicKeys 社区成员的公钥
     * @return 显示名字
     */
    public static String getCommunityInviteLink(@NonNull String chainID, @NonNull List<String> publicKeys) {
        Context context = MainApplication.getInstance();
        StringBuilder bs = new StringBuilder();
        for(String publicKey : publicKeys){
            bs.append(context.getString(R.string.community_invite_link_bs, publicKey));
        }
        return context.getString(R.string.community_invite_link_form, bs.toString(), chainID);
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
}