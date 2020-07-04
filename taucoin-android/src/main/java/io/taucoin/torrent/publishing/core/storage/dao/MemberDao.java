package io.taucoin.torrent.publishing.core.storage.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Update;
import io.taucoin.torrent.publishing.core.storage.entity.Member;

/**
 * Room:Member操作接口
 */
@Dao
public interface MemberDao {
    String QUERY_GET_COMMUNITIES_NOT_BLOCKED = "SELECT * FROM Community where blocked = 0";
    String QUERY_GET_COMMUNITY_BY_CHAIN_ID = "SELECT * FROM Community WHERE chainId = :chainId";

    /**
     * 添加新社区成员
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addMember(Member member);

    /**
     * 更新社区成员
     */
    @Update
    int updateMember(Member member);
}
