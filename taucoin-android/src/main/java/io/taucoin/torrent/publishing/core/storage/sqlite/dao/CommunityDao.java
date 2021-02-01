package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndMember;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;

/**
 * Room:Community操作接口
 */
@Dao
public interface CommunityDao {
    String QUERY_GET_CURRENT_USER_PK = " (SELECT publicKey FROM Users WHERE isCurrentUser = 1 limit 1) ";
    String QUERY_GET_BANNED_USER_PK = " (SELECT publicKey FROM Users WHERE isBanned == 1 and isCurrentUser != 1) ";
    String QUERY_NEWEST_MSG = " (SELECT * FROM (SELECT * FROM (" +
            " SELECT timestamp, hash, contentType, senderPk, friendPk, friendPk AS friendPkTemp" +
            " FROM (SELECT * FROM ChatMessages" +
            " WHERE senderPk = " + QUERY_GET_CURRENT_USER_PK +
            " ORDER BY timestamp) GROUP BY friendPk" +
            " UNION ALL" +
            " SELECT timestamp, hash, contentType, senderPk, friendPk, senderPk AS friendPkTemp" +
            " FROM (SELECT * FROM ChatMessages" +
            " WHERE friendPk = "+ QUERY_GET_CURRENT_USER_PK +
            " ORDER BY timestamp) GROUP BY senderPk)" +
            " ORDER BY timestamp)" +
            " GROUP BY friendPkTemp)";

    String QUERY_GET_COMMUNITIES_NOT_IN_BLACKLIST = "SELECT a.*, b.balance, b.power," +
            " (case when a.type = 0 then" +
            " (case when (d.timestamp IS NULL OR c.timestamp >= d.timestamp) then c.memo else d.content end)" +
            " else e.hash end)" +
            " AS txMemo," +
            " (case when a.type = 0 then 0 else e.contentType end) AS msgType," +
            " (case when a.type = 0 then" +
            " (case when (d.timestamp IS NULL OR c.timestamp >= d.timestamp) then c.timestamp else d.timestamp end)" +
            " else e.timestamp end)" +
            " AS txTimestamp" +
            " FROM Communities AS a" +
            " LEFT JOIN Members AS b ON a.type = 0 AND a.chainID = b.chainID and b.publicKey = " + QUERY_GET_CURRENT_USER_PK +
            " LEFT JOIN (SELECT timestamp, memo, chainID FROM (SELECT timestamp, memo, chainID FROM Txs" +
            " WHERE senderPk NOT IN " + QUERY_GET_BANNED_USER_PK +
            " ORDER BY timestamp) GROUP BY chainID) AS c" +
            " ON a.type = 0 AND a.chainID = c.chainID" +
            " LEFT JOIN (SELECT timestamp, content, chainID FROM (SELECT timestamp, content, chainID FROM Messages " +
            " WHERE senderPk NOT IN " + QUERY_GET_BANNED_USER_PK +
            " ORDER BY timestamp) GROUP BY chainID) AS d" +
            " ON a.type = 0 AND a.chainID = d.chainID" +
            " LEFT JOIN " + QUERY_NEWEST_MSG + " AS e" +
            " ON (a.chainID = e.senderPk AND a.publicKey = e.friendPk)" +
            " OR (a.chainID = e.friendPk AND a.publicKey = e.senderPk)" +
            " WHERE (a.type = 0 AND isBanned = 0)" +
            " OR (a.type = 1 AND a.chainID NOT IN " + QUERY_GET_BANNED_USER_PK + ")" +
            " ORDER BY txTimestamp DESC";
    String QUERY_GET_COMMUNITIES_IN_BLACKLIST = "SELECT * FROM Communities where isBanned = 1";
    String QUERY_GET_COMMUNITY_BY_CHAIN_ID = "SELECT * FROM Communities WHERE chainID = :chainID";
    String QUERY_ADD_COMMUNITY_BLACKLIST = "Update Communities set isBanned =:isBanned WHERE chainID = :chainID";
    String QUERY_JOINED_COMMUNITY = "SELECT * FROM Communities";
    String QUERY_CLEAR_COMMUNITY_STATE = "UPDATE Communities SET totalBlocks = 0, syncBlock = 0" +
            " WHERE chainID = :chainID";
    String QUERY_CURRENT_MEMBER = "SELECT * FROM Members" +
            " WHERE chainID = :chainID AND publicKey = :publicKey";

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
    Flowable<List<CommunityAndMember>> observeCommunitiesNotInBlacklist();

    /**
     * 获取在黑名单的社区列表
     */
    @Query(QUERY_GET_COMMUNITIES_IN_BLACKLIST)
    List<Community> getCommunitiesInBlacklist();

    /**
     * 添加社区黑名单
     */
    @Query(QUERY_ADD_COMMUNITY_BLACKLIST)
    void setCommunityBlacklist(String chainID, int isBanned);

    /**
     * 获取用户加入的社区列表
     */
    @Query(QUERY_JOINED_COMMUNITY)
    List<Community> getJoinedCommunityList();

    /**
     * 根据chainID查询社区
     * @param chainID 社区chainID
     */
    @Query(QUERY_GET_COMMUNITY_BY_CHAIN_ID)
    Single<Community> getCommunityByChainIDSingle(String chainID);

    @Query(QUERY_GET_COMMUNITY_BY_CHAIN_ID)
    Observable<Community> observerCommunityByChainID(String chainID);

    @Query(QUERY_CLEAR_COMMUNITY_STATE)
    void clearCommunityState(String chainID);

    @Query(QUERY_CURRENT_MEMBER)
    Observable<Member> observerCurrentMember(String chainID, String publicKey);
}