package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.taucoin.torrent.publishing.core.model.data.MemberAndUser;
import io.taucoin.torrent.publishing.core.model.data.Statistics;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
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
    @Override
    public Member getMemberByChainIDAndPk(@NonNull String chainID, @NonNull String publicKey){
        return db.memberDao().getMemberByChainIDAndPk(chainID, publicKey);
    }
    @Override
    public Flowable<List<MemberAndUser>> observeCommunityMembers(String chainID){
        return db.memberDao().observeCommunityMembers(chainID);
    }

    /**
     * 查询社区成员
     * @param chainID 社区链ID
     * @param onChain 是否上链
     * @return DataSource.Factory
     */
    @Override
    public DataSource.Factory<Integer, MemberAndUser> queryCommunityMembers(String chainID, boolean onChain){
        if(onChain){
            return db.memberDao().queryCommunityMembersOnChain(chainID);
        }else{
            return db.memberDao().queryCommunityMembersNotOnChain(chainID);
        }
    }

    /**
     * 获取和社区成员共在的社区数
     * @param currentUserPk
     * @param memberPk
     */
    @Override
    public Single<List<String>> getCommunityNumInCommon(String currentUserPk, String memberPk){
        return db.memberDao().getCommunityNumInCommon(currentUserPk, memberPk);
    }

    /**
     * 获取社区limit个成员
     * @param chainID
     * @param limit
     */
    @Override
    public Single<List<String>> getCommunityMembersLimit(String chainID, int limit){
        return db.memberDao().getCommunityMembersLimit(chainID, limit);
    }

    @Override
    public Flowable<Statistics> getMembersStatistics(String chainID){
        return db.memberDao().getMembersStatistics(chainID);
    }
}
