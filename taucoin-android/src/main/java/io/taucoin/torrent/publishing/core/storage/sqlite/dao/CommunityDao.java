package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndFriend;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;

/**
 * Room:Community操作接口
 */
@Dao
public interface CommunityDao {
    String QUERY_GET_CURRENT_USER_PK = " (SELECT publicKey FROM Users WHERE isCurrentUser = 1 limit 1) ";
    String QUERY_GET_BANNED_USER_PK = " (SELECT publicKey FROM Users WHERE isBanned == 1 and isCurrentUser != 1) ";
    String QUERY_NEWEST_MSG_DESC = " (SELECT * FROM (SELECT * FROM (" +
            " SELECT timestamp, content, contentType, logicMsgHash, senderPk, receiverPk, receiverPk AS receiverPkTemp" +
            " FROM (SELECT rowid, * FROM ChatMessages" +
            " WHERE senderPk = " + QUERY_GET_CURRENT_USER_PK +
            " ORDER BY timestamp DESC, logicMsgHash COLLATE UNICODE DESC, nonce DESC) GROUP BY receiverPk" +
            " UNION ALL" +
            " SELECT timestamp, content, contentType, logicMsgHash, senderPk, receiverPk, senderPk AS receiverPkTemp" +
            " FROM (SELECT rowid, * FROM ChatMessages" +
            " WHERE receiverPk = "+ QUERY_GET_CURRENT_USER_PK +
            " ORDER BY timestamp DESC, logicMsgHash COLLATE UNICODE DESC, nonce DESC) GROUP BY senderPk)" +

            " ORDER BY timestamp DESC, logicMsgHash COLLATE UNICODE DESC)" +
            " GROUP BY receiverPkTemp)";

    String QUERY_NEWEST_MSG_ASC = " (SELECT * FROM (SELECT * FROM (" +
            " SELECT timestamp, content, contentType, logicMsgHash, senderPk, receiverPk, receiverPk AS receiverPkTemp" +
            " FROM (SELECT rowid, * FROM ChatMessages" +
            " WHERE senderPk = " + QUERY_GET_CURRENT_USER_PK +
            " ORDER BY timestamp, logicMsgHash COLLATE UNICODE, nonce) GROUP BY receiverPk" +
            " UNION ALL" +
            " SELECT timestamp, content, contentType, logicMsgHash, senderPk, receiverPk, senderPk AS receiverPkTemp" +
            " FROM (SELECT rowid, * FROM ChatMessages" +
            " WHERE receiverPk = "+ QUERY_GET_CURRENT_USER_PK +
            " ORDER BY timestamp, logicMsgHash COLLATE UNICODE, nonce) GROUP BY senderPk)" +

            " ORDER BY timestamp, logicMsgHash COLLATE UNICODE)" +
            " GROUP BY receiverPkTemp)";

    String QUERY_COMMUNITIES = "SELECT a.chainID AS ID, b.balance AS balance, b.power AS power," +
            " 0 AS type, 0 AS msgType, '' AS senderPk, '' AS receiverPk, " +
            " 0 AS msgUnread, '' AS msg," +
            " (case when (d.timestamp IS NULL OR c.timestamp >= d.timestamp) then c.memo else d.content end)" +
            " AS memo," +
            " (case when (d.timestamp IS NULL OR c.timestamp >= d.timestamp) then c.timestamp else d.timestamp end)" +
            " AS timestamp" +
            " FROM Communities AS a" +
            " LEFT JOIN Members AS b ON a.chainID = b.chainID" +
            " AND b.publicKey = " + QUERY_GET_CURRENT_USER_PK +
            " LEFT JOIN (SELECT timestamp, memo, chainID FROM (SELECT timestamp, memo, chainID FROM Txs" +
            " WHERE senderPk NOT IN " + QUERY_GET_BANNED_USER_PK +
            " ORDER BY timestamp) GROUP BY chainID) AS c" +
            " ON a.chainID = c.chainID" +
            " LEFT JOIN (SELECT timestamp, content, chainID FROM (SELECT timestamp, content, chainID FROM Messages " +
            " WHERE senderPk NOT IN " + QUERY_GET_BANNED_USER_PK +
            " ORDER BY timestamp) GROUP BY chainID) AS d" +
            " ON a.chainID = d.chainID";

    String QUERY_FRIENDS_ASC = "SELECT f.friendPK AS ID, 0 AS balance, 0 AS power," +
            " 1 AS type, cm.contentType AS msgType," +
            " cm.senderPk AS senderPk, cm.receiverPk AS receiverPk," +
            " f.msgUnread AS msgUnread," +
            " cm.content AS msg, '' AS memo, cm.timestamp AS timestamp" +
            " FROM Friends f" +
            " LEFT JOIN " + QUERY_NEWEST_MSG_ASC + " AS cm" +
            " ON (f.userPK = cm.senderPk AND f.friendPK = cm.receiverPk)" +
            " OR (f.userPK = cm.receiverPk AND f.friendPK = cm.senderPk)" +
            " WHERE f.userPk = " + QUERY_GET_CURRENT_USER_PK +
            " AND f.friendPK NOT IN " + QUERY_GET_BANNED_USER_PK;

    String QUERY_FRIENDS_DESC = "SELECT f.friendPK AS ID, 0 AS balance, 0 AS power," +
            " 1 AS type, cm.contentType AS msgType," +
            " cm.senderPk AS senderPk, cm.receiverPk AS receiverPk," +
            " f.msgUnread AS msgUnread," +
            " cm.content AS msg, '' AS memo, cm.timestamp AS timestamp" +
            " FROM Friends f" +
            " LEFT JOIN " + QUERY_NEWEST_MSG_DESC + " AS cm" +
            " ON (f.userPK = cm.senderPk AND f.friendPK = cm.receiverPk)" +
            " OR (f.userPK = cm.receiverPk AND f.friendPK = cm.senderPk)" +
            " WHERE f.userPk = " + QUERY_GET_CURRENT_USER_PK +
            " AND f.friendPK NOT IN " + QUERY_GET_BANNED_USER_PK;

    String QUERY_COMMUNITIES_AND_FRIENDS_DESC = "SELECT * FROM (" + QUERY_FRIENDS_DESC +
            " UNION ALL " + QUERY_COMMUNITIES + ")" +
            " ORDER BY timestamp DESC";

    String QUERY_COMMUNITIES_AND_FRIENDS_ASC = "SELECT * FROM (" + QUERY_FRIENDS_ASC +
            " UNION ALL " + QUERY_COMMUNITIES + ")" +
            " ORDER BY timestamp DESC";

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
     * 查询不在黑名单中的社区列表（逆序）
     */
    @Query(QUERY_COMMUNITIES_AND_FRIENDS_DESC)
    @Transaction
    Flowable<List<CommunityAndFriend>> observeCommunitiesAndFriendsDESC();

    /**
     * 查询不在黑名单中的社区列表（正序）
     */
    @Query(QUERY_COMMUNITIES_AND_FRIENDS_ASC)
    @Transaction
    Flowable<List<CommunityAndFriend>> observeCommunitiesAndFriendsASC();

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