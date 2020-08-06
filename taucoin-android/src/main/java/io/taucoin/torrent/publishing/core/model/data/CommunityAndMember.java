package io.taucoin.torrent.publishing.core.model.data;

import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;

/**
 * Room: 查询Communities, 返回社区成员的信息
 */
public class CommunityAndMember extends Community {
    public long balance;                    // 成员在此社区的balance
    public long power;                      // 成员在此社区的power
    public String txMemo;                   // 交易备注信息
    public long txTimestamp;              // 交易时间戳
}
