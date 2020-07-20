package io.taucoin.torrent.publishing.core.model.data;

/**
 * Room: 数据库存储Transaction实体类
 */
public class ReplyTx {
    public String txID;                     // 交易ID
    public String chainID;                  // 交易所属社区chainID
    public String senderPk;                 // 交易发送者的公钥
    public long fee;                        // 交易费
    public long timestamp;                  // 交易时间戳
    public long nonce;                      // 交易nonce
    public int txType;                      // 交易类型，同MsgType中枚举类型
    public String memo;                     // 交易的备注、描述、bootstraps、评论等
    public int txStatus;                    // 交易的状态 0：未上链（在交易池中）；1：上链成功 (不上链)

    public String genesisPk;                // 创世区块者的公钥 只针对MsgType.CommunityAnnouncement类型
    public String receiverPk;               // 交易接收者的公钥 只针对MsgType.Wiring类型
    public long amount;                     // 交易金额 只针对MsgType.Wiring类型
    public String name;                     // 用户的链上名字 只针对MsgType.CommunityAnnouncement类型
}
