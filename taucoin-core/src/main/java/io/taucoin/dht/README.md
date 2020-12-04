### DHT


#### 设计目的

DHT模块作为应用程序与BitTorrent dht networks桥梁，主要为区块链业务和实时消息业务提供数据交换服务，基于[frostwire-jlibtorrent](https://github.com/frostwire/frostwire-jlibtorrent)开发，主要提供4种数据服务：get immutable item, put immutable item, get mutable item, put mutable item.

#### 核心模块

-- request queue: 数据请求通过DHTEngine API 放入到请求队列中。

-- session controller: 采用多session方案，session controller负责管理多个session，包括创建，销毁等。

-- TauSession & Worker: TauSession对[SessionManager](https://github.com/frostwire/frostwire-jlibtorrent/blob/master/src/main/java/com/frostwire/jlibtorrent/SessionManager.java)进行了封装，Worker 是request queue的消费者，负责从request queue取出请求通过TauSession进行数据交换。