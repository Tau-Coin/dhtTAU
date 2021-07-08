package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import android.content.Context;
import android.os.Build;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndFriend;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;

/**
 * CommunityRepository接口实现
 */
public class CommunityRepositoryImpl implements CommunityRepository{

    private Context appContext;
    private AppDatabase db;

    /**
     * CommunityRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    public CommunityRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    /**
     * 添加新的社区
     * @param community 社区数据
     */
    @Override
    public long addCommunity(@NonNull Community community) {
        return db.communityDao().addCommunity(community);
    }

    @Override
    public int updateCommunity(@NonNull Community community) {
        return db.communityDao().updateCommunity(community);
    }

    @Override
    public Community getCommunityByChainID(@NonNull String chainID) {
        return db.communityDao().getCommunityBychainID(chainID);
    }

    @Override
    public Community getChatByFriendPk(@NonNull String friendPk) {
        return db.communityDao().getCommunityBychainID(friendPk);
    }

    /**
     * 观察不在黑名单的社区列表数据变化
     * @return 被观察的社区数据列表
     */
    @Override
    public Flowable<List<CommunityAndFriend>> observeCommunitiesAndFriends() {
        int sdkVersion = Build.VERSION.SDK_INT;
        // android11中SQLite版本为3.28.0, group by取第一条记录，低版本取最后一条记录
        if (sdkVersion >= 30) {
            return db.communityDao().observeCommunitiesAndFriendsDESC();
        } else {
            return db.communityDao().observeCommunitiesAndFriendsASC();
        }
    }

    /**
     * 获取在黑名单的社区列表
     * @return List<Community>
     */
    public List<Community> getCommunitiesInBlacklist(){
        return db.communityDao().getCommunitiesInBlacklist();
    }

    /**
     * 添加社区黑名单实现
     * @param chainID 社区chainID
     * @param blacklist 是否加入黑名单
     */
    @Override
    public void setCommunityBlacklist(String chainID, boolean blacklist) {
        db.communityDao().setCommunityBlacklist(chainID, blacklist ? 1 : 0);
    }

    /**
     * 获取用户加入的社区列表
     * @param chainID
     */
    public List<Community> getJoinedCommunityList(String chainID){
        return db.communityDao().getJoinedCommunityList();
    }

    /**
     * 根据chainID查询社区
     * @param chainID 社区chainID
     */
    public Single<Community> getCommunityByChainIDSingle(String chainID){
        return db.communityDao().getCommunityByChainIDSingle(chainID);
    }

    @Override
    public Observable<Community> observerCommunityByChainID(String chainID) {
        return db.communityDao().observerCommunityByChainID(chainID);
    }

    @Override
    public void clearCommunityState(String chainID) {
        db.communityDao().clearCommunityState(chainID);
    }

    @Override
    public Observable<Member> observerCurrentMember(String chainID, String publicKey) {
        return db.communityDao().observerCurrentMember(chainID, publicKey);
    }
}
