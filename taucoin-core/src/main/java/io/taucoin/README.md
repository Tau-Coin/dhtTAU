## Taucoin-Core

### TauController

代码目录`controller`, 根据传入的数据路径和数据库实例来构建核心组件，并负责启动和关闭。

- accountManager
- dhtEngine
- chainManager 
- communicationManager 

[TauController Tutorial](https://github.com/Tau-Coin/dhtTAU/blob/master/taucoin-core/src/main/java/io/taucoin/controller/README.md)

### DHTEngine

代码目录`dht`，基于frostwire-jlibtorrent 与 bittorrent dht network完成数据交换。

- dht item request queue
- SessionController
- TauSession & Worker

[DHTEngine Tutorial](https://github.com/Tau-Coin/dhtTAU/blob/master/taucoin-core/src/main/java/io/taucoin/dht/README.md)
### Application

代码目录`communication` 和 `chain`
- [TAU communication on DHT](https://github.com/wuzhengy/TAU/blob/master/Communication%20over%20DHT.md)
- [Key Concepts in TAU chain](https://github.com/wuzhengy/tau#key-concepts)

[Application Tutorial]()