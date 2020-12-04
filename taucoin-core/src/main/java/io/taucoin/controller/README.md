### TauController

#### 设计目的
TauController 负责系统的初始化，并提供应用程序的调用接口，为Android UI & Linux client 提供服务。

#### 核心模块

- DHTEngine: 通过frostwire jlibtorrent 与 bittorrent dht network 完成数据交换。

- ChainManager: 负责区块链业务。

- CommunicationManager: 负责聊天业务。

- AccountManager: 存储用户公私钥。