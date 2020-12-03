## Taucoin-Core

### TauController

代码目录`controller`, 根据传入的数据路径和数据库实例来构建核心组件，并负责启动和关闭。

- accountManager
- dhtEngine
- chainManager 
- communicationManager 

[TauController Tutorial]()

### DHTEngine

代码目录`dht`，基于SessionController和Counter构建了一个单例，进行Item系统的数据流收发。
- item system
- sessionController
- counter

[DHTEngine Tutorial]()
### Application

代码目录`communication` 和 `chain`
- [TAU communication on DHT](https://github.com/wuzhengy/TAU/blob/master/Communication%20over%20DHT.md)
- [Key Concepts in TAU chain](https://github.com/wuzhengy/tau#key-concepts)

[Application Tutorial]()