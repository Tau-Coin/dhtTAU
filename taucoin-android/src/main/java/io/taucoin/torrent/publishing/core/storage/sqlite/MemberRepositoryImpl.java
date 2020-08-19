package io.taucoin.torrent.publishing.core.storage.sqlite;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.paging.PagedList;
import io.reactivex.Flowable;
import io.reactivex.Single;
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

    /**
     * 查询社区成员
     * @param chainID 社区链ID
     * @param onChain 是否上链
     * @return DataSource.Factory
     */
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
    public Single<List<String>> getCommunityNumInCommon(String currentUserPk, String memberPk){
        return db.memberDao().getCommunityNumInCommon(currentUserPk, memberPk);
    }

    /**
     * 获取社区limit个成员
     * @param chainID
     * @param limit
     */
    public Single<List<String>> getCommunityMembersLimit(String chainID, int limit){
        return db.memberDao().getCommunityMembersLimit(chainID, limit);
    }
}
