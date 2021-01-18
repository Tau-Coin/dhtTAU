package io.taucoin.torrent.publishing.ui.user;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import io.taucoin.torrent.publishing.core.model.data.UserAndFriend;
import io.taucoin.torrent.publishing.core.utils.StringUtil;

class UserSourceFactory extends UserDataSource.Factory<Integer, UserAndFriend> {
    private String friendPk;
    private int order;
    private boolean isAll;

    UserSourceFactory() {
    }

    void setParameter(int order, boolean isAll, String friendPk) {
        this.order = order;
        this.isAll = isAll;
        this.friendPk = friendPk;
        // 朋友公钥不存在，friendPk为空字符串，防止sql语句查询为空
        if (StringUtil.isEmpty(friendPk)) {
            this.friendPk = "";
        }
    }

    @NonNull
    @Override
    public DataSource<Integer, UserAndFriend> create() {
        return new UserDataSource(order, isAll, friendPk);
    }
}