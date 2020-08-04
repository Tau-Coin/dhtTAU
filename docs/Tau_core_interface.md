# Taucoin UI 端功能需求分析

## Taucoin-android 调用接口

### 区块链相关

- 启动链端运行环境
```
	void startTau();
```

- 停止链端运行环境
```
	void stopTau();
```

- 暂停链端挖矿
```
	void startApplications();
```

- 恢复链端挖矿
```
	void stopApplications();
```

- 获取 DHT Engine 状态
```
	DhtNodeList getDhtEngineStatus();
```

- 产生新用户seed
```
	void generateAccountSeed(byte[] seed);
```

- 更新用户seed
```
	void updateAccountSeed(byte[] seed);
```

- 获取用户某链的状态
```
	AccountState getAccountState(String chainID, String publicKey);
```

- 创建交易-(Note, Comment, CommuintyAnnouncement, Wiring, ???)
```
  Transaction createTransaction(ChainConfig chainConfig);
```
  
- 提交交易
```
  void submitTransaction(Transaction tx);
```

- 获取交易池交易

- 获取当前链最高区块
 
### Chat 相关
- Instant chat
```
  void createInstantChat(String chainID, byte[] msg， Hash hash);
```

- Invite chat


### 事件管理相关
- 增加事件
```
	void registerListener(TauListener listener)；
```

- 取消事件
```
	void unregisterListener(TauListener listener)；
```
## Taucoin-core 模块事件

###  DHT 相关
- DHT SessionStatus (Nodes no, Nodes info- ip、port、nodeid)
```
	void onSessionStats(@NonNull SessionStats newStats)；
```

### 区块链业务相关

- 区块链组件启动完成通知
```
	void onTauStarted();
```

- 区块链组件停止完成通知
```
	void onTauStopped();
```

- 区块链组件出错通知
```
	void onTauError(@NonNull String errorMsg);
```

- 新的区块通知
```
	void onNewBlock(@NonNull Block block);
```

- 区块回滚通知
```
	void onRollBackBlock(@NonNull Block block);
```

- 区块同步通知
```
	void onSyncBlock(@NonNull Block block);
```

### Chat 相关事件
- Instant chat
```
  void createInstantChat(@NonNull DhtItem item);
```

- Invite chat

## 其他
- Contact list
- Transaction fee
- Journal
