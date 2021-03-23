package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndFriend;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;

/**
 * 提供操作Community数据的接口
 */
public interface CommunityRepository {

    /**
     * 添加新的社区
     * @param community 社区数据
     */
    long addCommunity(@NonNull Community community);

    int updateCommunity(@NonNull Community community);
//
    Community getCommunityByChainID(@NonNull String chainID);

    Community getChatByFriendPk(@NonNull String friendPk);

    /**
     * 观察不在黑名单的社区列表数据变化
     * @return 被观察的社区数据列表
     */
    Flowable<List<CommunityAndFriend>> observeCommunitiesAndFriends();

    /**
     * 获取在黑名单的社区列表
     * @return List<Community>
     */
    List<Community> getCommunitiesInBlacklist();

    /**
     * 添加社区黑名单实现
     * @param chainID 社区chainID
     * @param blacklist 是否加入黑名单
     */
    void setCommunityBlacklist(String chainID, boolean blacklist);

    /**
     * 获取用户加入的社区列表
     * @param chainID 社区chainID
     */
    List<Community> getJoinedCommunityList(String chainID);

    /**
     * 根据chainID查询社区
     * @param chainID 社区chainID
     */
    Single<Community> getCommunityByChainIDSingle(String chainID);

    Observable<Community> observerCommunityByChainID(String chainID);

    /**
     * 清除社区状态，totalBlocks, syncBlock数据
     * @param chainID
     */
    void clearCommunityState(String chainID);

    /**
     * 观察当前登陆的社区成员
     * @param chainID
     * @param publicKey
     * @return
     */
    Observable<Member> observerCurrentMember(String chainID, String publicKey);
}
