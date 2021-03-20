package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.taucoin.repository.AppRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Friend;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.types.Message;

/**
 * FriendRepository接口实现
 */
public class AppRepositoryImpl implements AppRepository {

    private Context appContext;
    private AppDatabase db;

    /**
     * FriendRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    public AppRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    /**
     * 获取自己所有朋友
     * @return 朋友集合
     */
    @Override
    public Set<byte[]> getAllFriends() {
        Set<byte[]> set = new HashSet<>();
        return set;
    }

    /**
     * 获取自己和朋友的最新消息列表
     * @param friendPk 朋友的公钥
     * @param num 查询的消息的条数
     * @return 消息列表
     */
    @Override
    public List<Message> getLatestMessageList(byte[] friendPk, int num) {
        List<Message> list = new ArrayList<>();
        return list;
    }
}
