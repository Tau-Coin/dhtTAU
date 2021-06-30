package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import io.reactivex.Single;
import io.taucoin.torrent.publishing.core.model.data.CpuStatistics;
import io.taucoin.torrent.publishing.core.model.data.DataStatistics;
import io.taucoin.torrent.publishing.core.model.data.MemoryStatistics;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Statistic;

/**
 * Room:Statistics操作接口
 */
@Dao
public interface StatisticDao {

    // 查询流量统计数据
//    String QUERY_DATA_STATISTICS = "SELECT round(timestamp/(:seconds), 0) AS timeKey, max(timestamp) AS timestamp," +
//            " avg(dataSize) AS dataAvg" +
//            " FROM  Statistics WHERE isMetered = :isMetered AND" +
//            " datetime(timestamp, 'unixepoch', 'localtime') > datetime('now','-24 hour','localtime')" +
//            " GROUP BY timeKey" +
//            " ORDER BY timeKey";

    String QUERY_WHERE_24HOURS = " datetime(timestamp, 'unixepoch', 'localtime') > datetime('now','-24 hour','localtime')";

    String QUERY_DATA_STATISTICS = "SELECT a.timeKey, a.timestamp, b.meteredDataAvg, c.unMeteredDataAvg" +
            " From (SELECT round(timestamp/(:seconds), 0) AS timeKey, max(timestamp) AS timestamp" +
            " FROM  Statistics WHERE " + QUERY_WHERE_24HOURS + " GROUP BY timeKey ORDER BY timeKey) AS a" +

            " LEFT JOIN (SELECT round(timestamp/(:seconds), 0) AS timeKey, avg(dataSize) AS meteredDataAvg" +
            " FROM  Statistics WHERE isMetered = 1 AND " + QUERY_WHERE_24HOURS + " GROUP BY timeKey ORDER BY timeKey) AS b" +
            " ON a.timeKey = b.timeKey" +

            " LEFT JOIN (SELECT round(timestamp/(:seconds), 0) AS timeKey, avg(dataSize) AS unMeteredDataAvg" +
            " FROM  Statistics WHERE isMetered = 0 AND " + QUERY_WHERE_24HOURS + " GROUP BY timeKey ORDER BY timeKey) AS c" +
            " ON a.timeKey = c.timeKey";



    // 查询内存统计数据
    String QUERY_MEMORY_STATISTICS = "SELECT round(timestamp/(:seconds), 0) AS timeKey, max(timestamp) AS timestamp," +
            " avg(memorySize) AS memoryAvg" +
            " FROM  Statistics WHERE" +
            QUERY_WHERE_24HOURS +
            " GROUP BY timeKey" +
            " ORDER BY timeKey";


    // 查询CPU统计数据
    String QUERY_CPU_STATISTICS = "SELECT round(timestamp/(:seconds), 0) AS timeKey, max(timestamp) AS timestamp," +
            " avg(cpuUsageRate) AS cpuUsageRateAvg" +
            " FROM  Statistics WHERE" +
            QUERY_WHERE_24HOURS +
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

    @Query(QUERY_CPU_STATISTICS)
    Single<List<CpuStatistics>> getCpuStatistics(int seconds);

    /**
     * 删除旧数据
     */
    @Query(DELETE_OLD_STATISTICS)
    void deleteOldStatistics();
}
