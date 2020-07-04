package io.taucoin.torrent.publishing.core.storage;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.storage.entity.Community;

/**
 * 提供操作Community数据的接口
 */
public interface CommunityRepository {

    long addCommunity(@NonNull Community community);

    int updateCommunity(@NonNull Community community);

    Community getCommunityByChainId(@NonNull String chainId);

    Flowable<List<Community>> observeCommunitiesNotBlocked();
}
