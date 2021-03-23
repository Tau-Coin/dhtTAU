package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.paging.PagedList;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.data.MemberAndUser;
import io.taucoin.torrent.publishing.core.model.data.Statistics;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;

/**
 * Room:Member操作接口
 */
@Dao
public interface MemberDao {
    String WHERE_ON_CHAIN = " (balance > 0 OR power > 0) ";
    String QUERY_GET_MEMBER_BY_CHAIN_ID_PK = "SELECT * FROM Members WHERE chainID = :chainID AND publicKey = :publicKey";
    String QUERY_GET_MEMBERS_BY_CHAIN_ID = "SELECT * FROM Members WHERE chainID = :chainID";
    String QUERY_GET_MEMBERS_ON_CHAIN = "SELECT * FROM Members WHERE chainID = :chainID AND " + WHERE_ON_CHAIN;
    String QUERY_GET_MEMBERS_NOT_ON_CHAIN = "SELECT * FROM Members WHERE chainID = :chainID AND balance <= 0 AND power <= 0";
    String QUERY_COMMUNITY_NUM_IN_COMMON = "SELECT chainID FROM " +
            " (Select count(*) AS num, chainID FROM Members" +
            " where (publicKey =:currentUserPk OR publicKey =:memberPk) AND " + WHERE_ON_CHAIN +
            " GROUP BY chainID)" +
            " WHERE num >= 2";
    String QUERY_COMMUNITY_MEMBERS_LIMIT = "SELECT publicKey FROM Members" +
            " WHERE chainID = :chainID AND " + WHERE_ON_CHAIN +
            " ORDER BY power limit :limit";

    String QUERY_MEMBERS_STATISTICS = "SELECT a.members, b.online FROM" +
            " (SELECT COUNT(*) AS members FROM Members WHERE chainID =:chainID and " + WHERE_ON_CHAIN + ") a," +
            " (SELECT COUNT(*) AS online FROM Members WHERE chainID =:chainID and " + WHERE_ON_CHAIN +
            " and publicKey IN" +
            " (SELECT publicKey FROM Users WHERE " +
//            "lastUpdateTime > strftime('%s', 'now','-" + Constants.ONLINE_HOURS + "') OR " +
            "isCurrentUser = 1)) b";

    String QUERY_DELETE_COMMUNITY_MEMBERS = "DELETE FROM Members where chainID =:chainID";
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

    @Query(QUERY_GET_MEMBERS_BY_CHAIN_ID)
    @Transaction
    Flowable<List<MemberAndUser>> observeCommunityMembers(String chainID);

    /**
     * 查询社区上链的成员
     * @param chainID 社区链ID
     * @return DataSource.Factory
     */
    @Query(QUERY_GET_MEMBERS_ON_CHAIN)
    @Transaction
    DataSource.Factory<Integer, MemberAndUser> queryCommunityMembersOnChain(String chainID);

    /**
     * 查询社区未上链的成员
     * @param chainID 社区链ID
     * @return DataSource.Factory
     */
    @Query(QUERY_GET_MEMBERS_NOT_ON_CHAIN)
    @Transaction
    DataSource.Factory<Integer, MemberAndUser> queryCommunityMembersNotOnChain(String chainID);

    /**
     * 获取和社区成员共在的社区数
     * @param currentUserPk
     * @param memberPk
     */
    @Query(QUERY_COMMUNITY_NUM_IN_COMMON)
    Single<List<String>> getCommunityNumInCommon(String currentUserPk, String memberPk);

    /**
     * 获取社区limit个成员
     * @param chainID
     * @param limit
     */
    @Query(QUERY_COMMUNITY_MEMBERS_LIMIT)
    Single<List<String>> getCommunityMembersLimit(String chainID, int limit);

    @Query(QUERY_COMMUNITY_MEMBERS_LIMIT)
    List<String> queryCommunityMembersLimit(String chainID, int limit);

    @Query(QUERY_MEMBERS_STATISTICS)
    Flowable<Statistics> getMembersStatistics(String chainID);

    @Query(QUERY_DELETE_COMMUNITY_MEMBERS)
    void deleteCommunityMembers(String chainID);
}
