package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.taucoin.torrent.publishing.core.model.data.FavoriteAndUser;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Favorite;

/**
 * Room:Favorite操作接口
 */
@Dao
public interface FavoriteDao {
    String QUERY_REPLY_USERS = " (SELECT u.nickname, f.senderPk, f.ID FROM Users u" +
            " LEFT JOIN Favorites f ON u.publicKey = f.senderPk) ";
    String QUERY_GET_FAVORITES_AND_REPLY = "SELECT f.*, ur.nickname AS replyName FROM Favorites f" +
            " LEFT JOIN " + QUERY_REPLY_USERS + " AS ur ON f.replyID = ur.ID" +
            " WHERE isReply = 0 ORDER BY timestamp DESC";

    String QUERY_DEL_FAVORITE_BY_ID = "DELETE FROM Favorites WHERE ID = :ID";
    String QUERY_FAVORITE_BY_ID = "SELECT * FROM Favorites WHERE ID = :ID";


    /**
     * 添加新的收藏
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addFavorite(Favorite favorite);

    /**
     * 更新收藏
     */
    @Update
    int updateFavorite(Favorite favorite);

    /**
     * 删除收藏
     */
    @Query(QUERY_DEL_FAVORITE_BY_ID)
    void delFavoriteByID(String ID);

    /**
     * 查询收藏信息
     */
    @Query(QUERY_FAVORITE_BY_ID)
    Favorite queryFavoriteByID(String ID);

    /**
     * 查询所有的收藏
     */
    @Query(QUERY_GET_FAVORITES_AND_REPLY)
    @Transaction
    DataSource.Factory<Integer, FavoriteAndUser> queryFavorites();
}