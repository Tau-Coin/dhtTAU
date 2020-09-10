package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import java.util.List;

import io.reactivex.Observable;
import io.taucoin.torrent.publishing.core.model.data.MsgAndReply;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Message;

/**
 * 提供外部操作Message数据的接口
 */
public interface MsgRepository {

    /**
     * 发送新的消息
     */
    void sendMessage(Message msg);

    /**
     * 根据chainID查询社区消息
     * @param chainID 社区链id
     */
    List<Message> getMessagesByChainID(String chainID);

    /**
     * 根据msgID查询社区消息
     * @param msgID 社区消息id
     */
    Message getMessageByID(String msgID);

    /**
     * 获取社区的消息
     * @param chainID 社区chainID
     * @return 消息总数
     */
    int getNumMessages(String chainID);

    /**
     * 获取社区的消息
     * @param chainID 社区chainID
     * @param startPosition 数据开始位置
     * @param loadSize 加载数据大小
     * @return List<MsgAndReply>
     */
    List<MsgAndReply> getMessages(String chainID, int startPosition, int loadSize);

    /**
     * 观察社区的消息的变化
     */
    Observable<String> observeDataSetChanged();

    /**
     * 提交数据变化
     */
    void submitDataSetChanged();
}
