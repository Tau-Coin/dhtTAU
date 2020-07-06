package io.taucoin.torrent.publishing.core.storage;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.storage.entity.Community;

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
//    public Community getCommunityByChainId(@NonNull String chainId) {
//        return db.communityDao().getCommunityByChainId(chainId);
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
     * 添加社区黑名单实现
     * @param chainId 社区chainId
     * @param blacklist 是否加入黑名单
     */
    @Override
    public void setCommunityBlacklist(String chainId, boolean blacklist) {
        db.communityDao().setCommunityBlacklist(chainId, blacklist ? 1 : 0);
    }

    /**
     * 设置社区是否静音实现
     * @param chainId 社区chainId
     * @param isMute 是否静音
     */
    @Override
    public void setCommunityMute(String chainId, boolean isMute){
        db.communityDao().setCommunityMute(chainId, isMute ? 1 : 0);
    }
}
