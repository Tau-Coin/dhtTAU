package io.taucoin.torrent.publishing.core.storage.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.storage.entity.Community;

/**
 * Room:Community操作接口
 */
@Dao
public interface CommunityDao {
    String QUERY_GET_COMMUNITIES_NOT_IN_BLACKLIST = "SELECT * FROM Communities where blacklist = 0";
    String QUERY_GET_COMMUNITIES_IN_BLACKLIST = "SELECT * FROM Communities where blacklist = 1";
    String QUERY_GET_COMMUNITY_BY_CHAIN_ID = "SELECT * FROM Communities WHERE chainID = :chainID";
    String QUERY_ADD_COMMUNITY_BLACKLIST = "Update Communities set blacklist =:blacklist WHERE chainID = :chainID";
    String QUERY_SET_COMMUNITY_MUTE = "Update Communities set mute =:mute WHERE chainID = :chainID";
    String QUERY_JOINED_COMMUNITY = "SELECT * FROM Communities";

    /**
     * 添加新的社区
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addCommunity(Community community);

    /**
     * 更新社区
     */
    @Update
    int updateCommunity(Community community);

    /**
     * 根据chainIDc查询社区
     */
    @Query(QUERY_GET_COMMUNITY_BY_CHAIN_ID)
    Community getCommunityBychainID(String chainID);

    /**
     * 查询不在黑名单中的社区列表
     */
    @Query(QUERY_GET_COMMUNITIES_NOT_IN_BLACKLIST)
    Flowable<List<Community>> observeCommunitiesNotInBlacklist();

    /**
     * 获取在黑名单的社区列表
     */
    @Query(QUERY_GET_COMMUNITIES_IN_BLACKLIST)
    List<Community> getCommunitiesInBlacklist();

    /**
     * 添加社区黑名单
     */
    @Query(QUERY_ADD_COMMUNITY_BLACKLIST)
    void setCommunityBlacklist(String chainID, int blacklist);

    /**
     * 设置社区是否静音
     */
    @Query(QUERY_SET_COMMUNITY_MUTE)
    void setCommunityMute(String chainID, int mute);

    /**
     * 获取用户加入的社区列表
     */
    @Query(QUERY_JOINED_COMMUNITY)
    List<Community> getJoinedCommunityList();
}