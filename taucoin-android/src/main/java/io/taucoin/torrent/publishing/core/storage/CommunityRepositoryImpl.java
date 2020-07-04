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

    @Override
    public long addCommunity(@NonNull Community community) {
        return db.communityDao().addCommunity(community);
    }

    @Override
    public int updateCommunity(@NonNull Community community) {
        return db.communityDao().updateCommunity(community);
    }

    @Override
    public Community getCommunityByChainId(@NonNull String chainId) {
        return db.communityDao().getCommunityByChainId(chainId);
    }

    @Override
    public Flowable<List<Community>> observeCommunitiesNotBlocked() {
        return db.communityDao().observeCommunitiesNotBlocked();
    }
}
