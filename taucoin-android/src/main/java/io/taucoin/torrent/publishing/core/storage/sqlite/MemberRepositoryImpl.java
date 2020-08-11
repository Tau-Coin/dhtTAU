package io.taucoin.torrent.publishing.core.storage.sqlite;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.model.data.MemberAndUser;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;

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

    /**
     * 获取Member根据公钥和链ID
     * @param chainID 社区链ID
     * @param publicKey 公钥
     * @return Member
     */
    public Member getMemberByChainIDAndPk(@NonNull String chainID, @NonNull String publicKey){
        return db.memberDao().getMemberByChainIDAndPk(chainID, publicKey);
    }

    public Flowable<List<MemberAndUser>> observeCommunityMembers(String chainID){
        return db.memberDao().observeCommunityMembers(chainID);
    }
}
