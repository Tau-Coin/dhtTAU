/*
 * Copyright (C) 2019 Yaroslav Pronin <proninyaroslav@mail.ru>
 *
 * This file is part of LibreTorrent.
 *
 * LibreTorrent is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibreTorrent is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LibreTorrent.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.taucoin.torrent.publishing.core.storage;

import android.content.Context;
import androidx.annotation.NonNull;

/**
 * RepositoryHelper创建数据库操作单例
 */
public class RepositoryHelper {
    private static CommunityRepositoryImpl communityRepo;
    private static MemberRepositoryImpl memberRepo;
    private static UserRepositoryImpl userRepo;
    private static TxRepositoryImpl txRepo;

    /**
     * 获取CommunityRepository单例
     * @param appContext 上下文
     * @return CommunityRepository
     */
    public synchronized static CommunityRepository getCommunityRepository(@NonNull Context appContext) {
        if (communityRepo == null)
            communityRepo = new CommunityRepositoryImpl(appContext,
                    AppDatabase.getInstance(appContext));

        return communityRepo;
    }

    /**
     * 获取MemberRepository单例
     * @param appContext 上下文
     * @return MemberRepository
     */
    public synchronized static MemberRepository getMemberRepository(@NonNull Context appContext) {
        if (memberRepo == null)
            memberRepo = new MemberRepositoryImpl(appContext,
                    AppDatabase.getInstance(appContext));

        return memberRepo;
    }

    /**
     * 获取UserRepository单例
     * @param appContext 上下文
     * @return UserRepository
     */
    public synchronized static UserRepository getUserRepository(@NonNull Context appContext) {
        if (userRepo == null)
            userRepo = new UserRepositoryImpl(appContext,
                    AppDatabase.getInstance(appContext));

        return userRepo;
    }

    /**
     * 获取TxRepository单例
     * @param appContext 上下文
     * @return TxRepository
     */
    public synchronized static TxRepository getTxRepository(@NonNull Context appContext) {
        if (txRepo == null)
            txRepo = new TxRepositoryImpl(appContext,
                    AppDatabase.getInstance(appContext));

        return txRepo;
    }
}
