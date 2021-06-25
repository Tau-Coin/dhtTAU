package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import io.reactivex.Single;
import io.taucoin.torrent.publishing.core.model.data.DataStatistics;
import io.taucoin.torrent.publishing.core.model.data.MemoryStatistics;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Statistic;

/**
 * Room:Statistics操作接口
 */
@Dao
public interface StatisticDao {

    // 查询统计数据
    String QUERY_DATA_STATISTICS = "SELECT round(timestamp/(:seconds), 0) AS timeKey, max(timestamp) AS timestamp," +
            " avg(dataSize) AS dataAvg" +
            " FROM  Statistics WHERE" +
            " datetime(timestamp, 'unixepoch', 'localtime') > datetime('now','-24 hour','localtime')" +
            " GROUP BY timeKey" +
            " ORDER BY timeKey";

    // 查询统计数据
    String QUERY_MEMORY_STATISTICS = "SELECT round(timestamp/(:seconds), 0) AS timeKey, max(timestamp) AS timestamp," +
            " avg(memorySize) AS memoryAvg" +
            " FROM  Statistics WHERE" +
            " datetime(timestamp, 'unixepoch', 'localtime') > datetime('now','-24 hour','localtime')" +
            " GROUP BY timeKey" +
            " ORDER BY timeKey";

    // 删除旧数据
    String DELETE_OLD_STATISTICS = "DELETE FROM Statistics WHERE" +
            " datetime(timestamp, 'unixepoch', 'localtime') <= datetime('now','-24 hour','localtime')";

    /**
     * 添加用户设备信息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addStatistic(Statistic statistic);

    /**
     * 查询统计数据
     */
    @Query(QUERY_DATA_STATISTICS)
    Single<List<DataStatistics>> getDataStatistics(int seconds);

    @Query(QUERY_MEMORY_STATISTICS)
    Single<List<MemoryStatistics>> getMemoryStatistics(int seconds);

    /**
     * 删除旧数据
     */
    @Query(DELETE_OLD_STATISTICS)
    void deleteOldStatistics();
}
