package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;

/**
 * Room:Member操作接口
 */
@Dao
public interface MemberDao {
    String QUERY_GET_MEMBER_BY_CHAIN_ID_PK = "SELECT * FROM Members WHERE chainID = :chainID AND publicKey = :publicKey";

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

    /**
     * 获取Member根据公钥和链ID
     * @param chainID 社区链ID
     * @param publicKey 公钥
     * @return Member
     */
    @Query(QUERY_GET_MEMBER_BY_CHAIN_ID_PK)
    Member getMemberByChainIDAndPk(@NonNull String chainID, @NonNull String publicKey);
}
