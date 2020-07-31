# taucoin-android 和 taucoin-core 两个Module之间的交互接口

## taucoin-core上报事件
### 区块链相关
- 区块链组件全部启动完成<br/>
  void onTauStarted();
  
- 区块链组件全部停止结束<br/>
  void onTauStopped();
  
- 区块链组件出错<br/>
  void onTauError(@NonNull String errorMsg);
  
- 新的区块<br/>
  void onNewBlock(@NonNull Block block);
  
- 区块回滚<br/>
  void onRollBack(@NonNull Block block);
  
- 同步区块<br/>
  void onSyncBlock(@NonNull Block block);
  
###  DHT相关
- DHT SessionStat变化事件<br/>
  void onSessionStats(@NonNull SessionStats newStats)
  
## taucoin-android可调用的接口
### 区块链相关
- 启动链端业务<br/>
  void start();

- 停止链端业务<br/>
  void stop();

- 暂停链端业务<br/>
  void pause();

- 恢复链端业务<br/>
  void resume();

- 更新用户seed<br/>
   void updateKey(byte[] seed);

- 注册事件<br/>
  registerListener(TauListener listener)

- 反注册事件<br/>
  unregisterListener(TauListener listener)

- 创建社区<br/>
  void createCommunity(ChainConfig chainConfig);
  
- 提交交易到链端交易池<br/>
  void submitTransaction(Transaction tx);

- 获取用户当前链的Power值<br/>
  void getUserPower(String chainID, String publicKey);
  
- 获取用户当前链的Balance值<br/>
  void getUserBalance(String chainID, String publicKey);