package io.taucoin.torrent.publishing.core.storage.sqlite;

import java.util.List;

import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Message;

/**
 * 提供外部操作User数据的接口
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
     * 根据chainID获取社区的消息的被观察者
     * @param chainID 社区链id
     */
    Flowable<List<Message>> observeMessagesByChainID(String chainID);
}
