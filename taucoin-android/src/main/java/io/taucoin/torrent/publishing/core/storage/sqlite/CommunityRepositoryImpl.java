package io.taucoin.torrent.publishing.core.storage.sqlite;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;

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

//    @Override
//    public int updateCommunity(@NonNull Community community) {
//        return db.communityDao().updateCommunity(community);
//    }
//
//    @Override
//    public Community getCommunityBychainID(@NonNull String chainID) {
//        return db.communityDao().getCommunityBychainID(chainID);
//    }

    /**
     * 观察不在黑名单的社区列表数据变化
     * @return 被观察的社区数据列表
     */
    @Override
    public Flowable<List<Community>> observeCommunitiesNotInBlacklist() {
        return db.communityDao().observeCommunitiesNotInBlacklist();
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
     * 设置社区是否静音实现
     * @param chainID 社区chainID
     * @param isMute 是否静音
     */
    @Override
    public void setCommunityMute(String chainID, boolean isMute){
        db.communityDao().setCommunityMute(chainID, isMute ? 1 : 0);
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
}
