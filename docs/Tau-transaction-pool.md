### 交易池介绍

#### 交易池里按照nonce的连续性分为两个池：

pending->存放和statedb里对应账户nonce能对接上的连续的交易，里面的交易可以直接用来出块；

queue->存放因为某些原因nonce比较小的交易未收到，导致的nonce不连续的交易。

pending与queue里面的交易，因为某些原因增删可能会互相转移，queue里面的交易，因为某些原因导致nonce值能和pending里面对应账户连续接上，就可以从queue里面移到pending里面，这个过程叫做promote；反之，不连续的交易从pending移到queue的过程叫做demote。

#### 触发交易池状态改变的操作主要包括：

Add local->增加本地交易；

Add remote->增加远端转发的交易；

Reset->由于链的状态导致的改变，导致nonce值变化，使得交易池里的交易合法性状态发生改变；

Remove tx->定时删除一些很久上不了链的交易。

前面三个操作会统一通过交易池重组（Reorg）接口，统一对交易池进行promote或者demote。

很久上不了链的交易会有heart beats来记录交易最近操作的时间，还有一个pending nonce内存数据库用来辅助记录pending里对应账户的nonce增长情况。

#### 交易池配置说明：

var DefaultTxPoolConfig = TxPoolConfig{

  Journal:  "transactions.rlp",

  Rejournal: time.Hour,



  PriceLimit: 1,

  PriceBump: 10,



  AccountSlots: 16,

  GlobalSlots: 4096,

  AccountQueue: 64,

  GlobalQueue: 1024,



  Lifetime: 3 * time.Hour,

}

以上为eth的交易池默认配置，考虑到taucoin后面可能会允许负交易费的交易，作相应的调整：

PriceLimit为限制进入交易池的最小price，taucoin没有price概念而是Fee，目前暂时设置Fee>=0，后面版本可考虑小于零的情况；

PriceBump为相同账户相同nonce的交易替换比例门槛值，同样的对应taucoin的FeeBump，目前暂时设置FeeBump=0，后面版本考虑取消；

AccountSlots、GlobalSlots、AccountQueue、GlobalQueue对应pengding和queue交易池每个账户存放交易的上限以及总量上限，目前暂时保持不动；

Lifetime为交易池里交易的生命周期，taucoin目前去除了expiretime字段，目前可默认此值用于交易过期。
