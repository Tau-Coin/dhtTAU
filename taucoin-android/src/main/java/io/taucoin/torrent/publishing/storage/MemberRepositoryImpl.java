package io.taucoin.torrent.publishing.storage;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.storage.entity.Community;
import io.taucoin.torrent.publishing.storage.entity.Member;

/**
 * CommunityRepository接口实现
 */
public class MemberRepositoryImpl implements MemberRepository{

    private Context appContext;
    private AppDatabase db;

    /**
     * MemberRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    public MemberRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    /**
     * 添加新的Member
     * @param member User对象
     * @return 结果
     */
    @Override
    public long addMember(@NonNull Member member) {
        return db.memberDao().addMember(member);
    }

    /**
     * 更新Member数据
     * @param member User对象
     * @return 结果
     */
    @Override
    public int updateMember(@NonNull Member member) {
        return db.memberDao().updateMember(member);
    }

}
