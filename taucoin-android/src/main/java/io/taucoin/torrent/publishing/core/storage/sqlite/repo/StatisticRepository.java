package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import java.util.List;

import io.reactivex.Single;
import io.taucoin.torrent.publishing.core.model.data.CpuStatistics;
import io.taucoin.torrent.publishing.core.model.data.DataStatistics;
import io.taucoin.torrent.publishing.core.model.data.MemoryStatistics;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Statistic;

/**
 * 提供操作Statistic数据的接口
 */
public interface StatisticRepository {

    /**
     * 添加统计信息
     * @param statistic
     */
   void addStatistic(Statistic statistic);

    /**
     * 获取流量数据统计信息
     */
    Single<List<DataStatistics>> getDataStatistics();

    /**
     * 获取内存数据统计信息
     */
    Single<List<MemoryStatistics>> getMemoryStatistics();

    /**
     * 获取CPU数据统计信息
     */
    Single<List<CpuStatistics>> getCpuStatistics();

    /**
     * 删除旧数据
     */
    void deleteOldStatistics();
}
