package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import io.taucoin.torrent.publishing.core.model.data.FavoriteAndUser;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Favorite;

/**
 * TxRepository接口实现
 */
public class FavoriteRepositoryImpl implements FavoriteRepository{

    private Context appContext;
    private AppDatabase db;

    /**
     * CommunityRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    public FavoriteRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    @Override
    public long addFavorite(Favorite favorite) {
        return db.favoriteDao().addFavorite(favorite);
    }

    /**
     * 更新收藏
     */
    @Override
    public int updateFavorite(Favorite favorite){
        return db.favoriteDao().updateFavorite(favorite);
    }

    @Override
    public void delFavoriteByID(String ID) {
        db.favoriteDao().delFavoriteByID(ID);
    }

    /**
     * 查询收藏信息
     */
    @Override
    public Favorite queryFavoriteByID(String ID){
        return db.favoriteDao().queryFavoriteByID(ID);
    }

    @Override
    public DataSource.Factory<Integer, FavoriteAndUser> queryFavorites() {
        return db.favoriteDao().queryFavorites();
    }
}
