package io.taucoin.torrent.publishing.core.storage.sqlite.repo;


import java.util.List;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.taucoin.torrent.publishing.core.model.data.MemberAndUser;
import io.taucoin.torrent.publishing.core.model.data.Statistics;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;

/**
 * 提供操作Member数据的接口
 */
public interface MemberRepository {

    /**
     * 添加新的Member
     * @param member User对象
     * @return 结果
     */
    long addMember(@NonNull Member member);

    /**
     * 更新Member数据
     * @param member User对象
     * @return 结果
     */
    int updateMember(@NonNull Member member);

    /**
     * 获取Member根据公钥和链ID
     * @param chainID 社区链ID
     * @param publicKey 公钥
     * @return Member
     */
    Member getMemberByChainIDAndPk(@NonNull String chainID, @NonNull String publicKey);

    Flowable<List<MemberAndUser>> observeCommunityMembers(String chainID);

    DataSource.Factory<Integer, MemberAndUser> queryCommunityMembers(String chainID, boolean onChain);

    /**
     * 获取和社区成员共在的社区数
     * @param currentUserPk
     * @param memberPk
     */
    Single<List<String>> getCommunityNumInCommon(String currentUserPk, String memberPk);

    /**
     * 获取社区limit个成员
     * @param chainID
     * @param limit
     */
    Single<List<String>> getCommunityMembersLimit(String chainID, int limit);

    List<String> queryCommunityMembersLimit(String chainID, int limit);

    /**
     * 获取社区成员统计
     * @param chainID
     */
    Flowable<Statistics> getMembersStatistics(String chainID);

    /**
     * 删除社区成员数据
     * @param chainID
     */
    void deleteCommunityMembers(String chainID);

    Observable<String> observeDataSetChanged();

    void submitDataSetChanged();
}
