package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import androidx.paging.DataSource;
import io.taucoin.torrent.publishing.core.model.data.FavoriteAndUser;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Favorite;

/**
 * 提供操作Member数据的接口
 */
public interface FavoriteRepository {

    /**
     * 添加新的收藏
     */
    long addFavorite(Favorite favorite);

    /**
     * 更新收藏
     */
    int updateFavorite(Favorite favorite);

    /**
     * 删除收藏
     */
    void delFavoriteByID(String ID);

    /**
     * 查询收藏信息
     */
    Favorite queryFavoriteByID(String ID);

    /**
     * 查询所有的收藏
     */
    DataSource.Factory<Integer, FavoriteAndUser> queryFavorites();
}
