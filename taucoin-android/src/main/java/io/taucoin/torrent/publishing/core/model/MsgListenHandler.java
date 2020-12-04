package io.taucoin.torrent.publishing.core.model;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Chat;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Friend;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.CommunityRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.FriendRepository;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.types.Message;
import io.taucoin.util.ByteUtil;

/**
 * MsgListener处理程序
 */
class MsgListenHandler {
    private static final Logger logger = LoggerFactory.getLogger("MsgListenHandler");
    private CompositeDisposable disposables = new CompositeDisposable();
    private ChatRepository chatRepo;
    private CommunityRepository communityRepo;
    private FriendRepository friendRepo;
    private TauDaemon daemon;

    MsgListenHandler(Context appContext, TauDaemon daemon){
        this.daemon = daemon;
        chatRepo = RepositoryHelper.getChatRepository(appContext);
        communityRepo = RepositoryHelper.getCommunityRepository(appContext);
        friendRepo = RepositoryHelper.getFriendsRepository(appContext);
    }
    /**
     * 处理新的消息
     * 0、如果没和朋友建立Chat, 创建Chat
     * 1、更新朋友状态
     * 2、保存Chat的聊天信息
     * @param friendPk byte[] 朋友公钥
     * @param message Message
     */
    void onNewMessage(byte[] friendPk, Message message) {
        onNewMessage(ByteUtil.toHexString(friendPk), message);
    }

    private void onNewMessage(String friendPk, Message message) {
        Disposable disposable = Flowable.create(emitter -> {
            // 处理ChatName，如果为空，取显朋友显示名
            String communityName = UsersUtil.getDefaultName(friendPk);
            Community community = communityRepo.getChatByFriendPk(friendPk);
            if (null == community) {
                community = new Community(friendPk, communityName);
                community.type = 1;
                communityRepo.addCommunity(community);
            }
            String userPk = MainApplication.getInstance().getPublicKey();
            Friend friend = friendRepo.queryFriend(userPk, friendPk);
            if (friend != null) {
                friend.state = 2;
                friendRepo.updateFriend(friend);
            }
            logger.debug("onNewMessage friendPk::{}，blockNum::{}, blockHash::{}", friendPk,
                    message.getHash(), message.getContentLink());

            String hash = ByteUtil.toHexString(message.getHash());
            String contentLinkStr = ByteUtil.toHexString(message.getContentLink());
            long timestamp = ByteUtil.byteArrayToLong(message.getTimestamp());
            int type = message.getType().ordinal();
            Chat chat = new Chat(hash, friendPk, userPk, contentLinkStr, type, timestamp);
            chatRepo.addChat(chat);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     * 销毁处理程序
     */
    void destroy() {
        disposables.clear();
    }
}
