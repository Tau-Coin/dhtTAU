# taucoin-android 和 taucoin-core 两个Module之间的交互接口

## taucoin-core上报事件
### 区块链相关
- 区块链组件全部启动完成<br/>
  void onTauStarted();
  
- 区块链组件全部启动完成<br/>
  void onTauStopped();
  
- 区块链组件出错<br/>
  void onTauError(@NonNull String errorMsg);
  
- 新的区块<br/>
  void onNewBlock(@NonNull Block block);
  
- 区块回滚<br/>
  void onNewBlock(@NonNull Block block);
  
- 同步区块<br/>
  void onNewBlock(@NonNull Block block);  
  
###  DHT相关
- DHT SessionStat变化事件sSessionStats<br/>
  void onSessionStats(@NonNull SessionStats newStats)
  
## taucoin-android可调用的接口
### 区块链相关
- 创建社区<br/>
  void createCommunity(ChainConfig chainConfig);
  
- 提交交易到链端交易池<br/>
  void submitTransaction(Transaction tx);

- 获取用户当前链的Power值<br/>
  void getUserPower(String chainID, String publicKey);
  
- 获取用户当前链的Balance值<br/>
  void getUserBalance(String chainID, String publicKey);