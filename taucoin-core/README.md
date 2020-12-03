## taucoin-core

taucoin-core的实现和平台无关，主要包括了<strong>taucontroller, dhtEngine, application</strong>三大部分。

- taucontroller
> 负责管理各核心组件,同时承接了平台相关的(存储目录，数据库选择)传入taucontroller进行构造即可。
- dhtEngine
> 负责应用端的网络数据流，包括communication messages, transactions, blocks等等。

- application
> 负责聊天业务和区块链端的所有业务，聊天内容的收发和存储，交易池管理，挖矿等等。

[详细分析](https://github.com/Tau-Coin/dhtTAU/blob/master/taucoin-core/src/main/java/io/taucoin/README.md)
