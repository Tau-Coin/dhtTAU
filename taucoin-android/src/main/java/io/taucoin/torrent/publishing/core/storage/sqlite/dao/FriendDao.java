package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Friend;

/**
 * Room:Friend操作接口
 */
@Dao
public interface FriendDao {
    String QUERY_FRIEND = "SELECT * FROM Friends" +
            " WHERE userPK = :userPK AND friendPK = :friendPK";

    String QUERY_CONNECTED_FRIENDS = "SELECT friendPK FROM Friends" +
            " WHERE userPK = :userPK AND state = 2" +
            " limit :limit";

    /**
     * 添加新社区成员
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addFriend(Friend friend);

    @Update
    void updateFriend(Friend friend);

    @Query(QUERY_FRIEND)
    Friend queryFriend(String userPK, String friendPK);

    @Query(QUERY_CONNECTED_FRIENDS)
    List<String> queryConnectedFriends(String userPK, int limit);
}
