package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.ReplyAndTx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
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
        if(StringUtil.isNotEmpty(user.localName)){
            return user.localName;
        }else{
            return getDefaultName(user.publicKey);
        }
    }

    /**
     * 获取显示名字
     * @param tx 当前交易
     * @return 显示名字
     */
    public static String getShowName(@NonNull ReplyAndTx tx) {
        if(StringUtil.isNotEmpty(tx.nickName)){
            return tx.nickName;
        }else{
            return getDefaultName(tx.senderPk);
        }
    }

    /**
     * 获取被回复的用户显示名字
     * @param tx 当前交易
     * @return 显示名字
     */
    public static String getShowReplyName(@NonNull ReplyAndTx tx) {
        if(StringUtil.isNotEmpty(tx.replyName)){
            return tx.replyName;
        }else{
            return getDefaultName(tx.replyTx.senderPk);
        }
    }

    /**
     * 获取显示coin name
     * @param community 当前社区
     * @return 显示名字
     */
    public static String getCoinName(Community community) {
        if(null == community){
            return null;
        }
        String coin;
        if(StringUtil.isNotEmpty(community.coinName)){
            coin = community.coinName;
        }else {
            coin = StringUtil.getFirstLettersOfName(community.communityName) + "Coin";
        }
        return coin;
    }

    /**
     * 获取社区邀请链接
     * @param community 当前社区
     * @return 显示名字
     */
    public static String getCommunityInviteLink(Community community) {
        Context context = MainApplication.getInstance();
        String bs = context.getString(R.string.community_invite_link_bs, community.publicKey);
        return context.getString(R.string.community_invite_link_form, bs, community.chainID);
    }
}