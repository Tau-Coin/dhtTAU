package io.taucoin.torrent.publishing.storage.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.storage.entity.Community;

/**
 * Community操作接口
 */
@Dao
public interface CommunityDao {
    String QUERY_GET_COMMUNITIES_NOT_BLOCKED = "SELECT * FROM Community where blocked = 0";
    String QUERY_GET_COMMUNITY_BY_CHAIN_ID = "SELECT * FROM Community WHERE chainId = :chainId";

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addCommunity(Community community);

    @Update
    int updateCommunity(Community community);

    @Query(QUERY_GET_COMMUNITY_BY_CHAIN_ID)
    Community getCommunityByChainId(String chainId);

    @Query(QUERY_GET_COMMUNITIES_NOT_BLOCKED)
    Flowable<List<Community>> observeCommunitiesNotBlocked();
}
