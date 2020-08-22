package io.taucoin.torrent.publishing.core.storage.sqlite;

import java.util.List;

import androidx.paging.DataSource;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.model.data.MsgAndReply;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Message;

/**
 * 提供外部操作Message数据的接口
 */
public interface MsgRepository {

    /**
     * 发送新的消息
     */
    long sendMessage(Message msg);

    /**
     * 根据chainID查询社区消息
     * @param chainID 社区链id
     */
    List<Message> getMessagesByChainID(String chainID);

    /**
     * 根据chainID获取社区的消息
     * @param chainID 社区链id
     */
    DataSource.Factory<Integer, MsgAndReply> queryMessagesByChainID(String chainID);
}
