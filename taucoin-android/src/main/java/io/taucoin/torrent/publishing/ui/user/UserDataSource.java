package io.taucoin.torrent.publishing.ui.user;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.paging.PositionalDataSource;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.model.data.UserAndFriend;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.FriendRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;

/**
 * 用户（朋友）列表分页查询数据源
 */
class UserDataSource extends PositionalDataSource<UserAndFriend> {
    private static final Logger logger = LoggerFactory.getLogger("UserDataSource");
    private UserRepository userRepo;
    private MemberRepository memRepo;
    private FriendRepository friendRepo;
    private int order;
    private boolean isAll;
    private String friendPk;
    private CompositeDisposable disposables = new CompositeDisposable();

    UserDataSource(int order, boolean isAll, String friendPk) {
        Context appContext = MainApplication.getInstance();
        this.userRepo = RepositoryHelper.getUserRepository(appContext);
        this.memRepo = RepositoryHelper.getMemberRepository(appContext);
        this.friendRepo = RepositoryHelper.getFriendsRepository(appContext);
        this.order = order;
        this.isAll = isAll;
        this.friendPk = friendPk;
        disposables.add(userRepo.observeDataSetChanged()
                .subscribe(s -> invalidate()));
        disposables.add(memRepo.observeDataSetChanged()
                .subscribe(s -> invalidate()));
        disposables.add(friendRepo.observeDataSetChanged()
                .subscribe(s -> invalidate()));
    }

    @Override
    public void invalidate() {
        if (disposables != null && disposables.isDisposed()) {
            disposables.dispose();
        }
        super.invalidate();
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams params,
                            @NonNull LoadInitialCallback<UserAndFriend> callback) {
        int numMessages = userRepo.getNumUsers(isAll, friendPk);
        int pos;
        int loadSize = params.requestedLoadSize;
        // 初始加载大小大于等于数据总数，开始位置为0，否则为二者之差
        if (loadSize >= numMessages) {
            pos = 0;
        } else {
            pos = numMessages - loadSize;
        }
        logger.debug("loadInitial pos::{}, LoadSize::{}, numMessages::{}", pos, loadSize, numMessages);
        List<UserAndFriend> messages = userRepo.getUsers(isAll, order, friendPk, pos, loadSize);
        logger.debug("loadInitial messages.size::{}", messages.size());
        if (messages.isEmpty()) {
            callback.onResult(messages, 0);
        } else {
            callback.onResult(messages, pos);
        }
    }

    @Override
    public void loadRange(@NonNull LoadRangeParams params,
                          @NonNull LoadRangeCallback<UserAndFriend> callback) {
        List<UserAndFriend> messages;
        int numMessages = userRepo.getNumUsers(isAll, friendPk);
        int pos = params.startPosition;
        int loadSize = params.loadSize;
        logger.debug("loadRange pos::{}, loadSize::{}, numEntries::{}", pos, loadSize, numMessages);
        if (pos < numMessages) {
            // 开始位置小于数据总数
            messages = userRepo.getUsers(isAll, order, friendPk, pos, loadSize);
        } else {
            // 否则数据为空
            messages = new ArrayList<>(0);
        }
        logger.debug("loadRange messages.size::{}", messages.size());
        callback.onResult(messages);
    }
}
