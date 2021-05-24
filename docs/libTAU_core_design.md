# libTAU核心设计

## Session功能相关

### Session管理

- 启动session
```
	void start_session(session_params&& params, application_repo repo);
```
- 停止session
```
	void stop_session();
```
- 重启session
```
	void restart_session();
```

### 网络相关
- 重启网络
```
	void reopen_network_sockets();
```

## 参数设置相关
### 网络设置
- read_only to be discussed
### 用户配置相关
- 设置alpha
```
	void set_branch_factor(int branch_factor);
```

## libTAU业务相关
### communication
- 应用端设置
```
	void set_main_loop_interval(int loop_time_interval);  //设置主循环频率
	void add_node(node_id id);             //添加节点
	void remove_node(node_id id);          //删除节点
    void set_node_info(char* node_info);   //更新节点信息
	void set_chatting_node(node_id id);    //设置正在聊天的节点
	void set_active_node(node_id id);      //设置活跃的节点
	void send_payload(node_id id, char* payload);    //发送数据
```

应用端设置操作，影响的是应用层采用的db实例，这个实例定义了相关操作接口，传入libtau;

- libTAU中communication操作
```
	int get_main_loop_interval();       //获取主循环频率
    std::list<node_id> get_all_nodes(); //获取目前所有节点
    node_id get_chatting_node();        //获取正在聊天的节点
    std::list<node_id> get_active_nodes();    //获取活跃节点
    char* get_node_info(node_id node);  //获取节点信息
    std::list<char *> get_latest_payload(node_id node, int num); //获取自己和节点的最新数据列表
```

### chain
- 多链系统
```
void new_chain(char* name);
void follow_chain(chain_id chain);
void unfollow_chain(chain_id chain);
```
- 账户系统
```
int get_accout_info(node_id id, chain_id chain);
```
- Mining相关
```
void start_mining();
void stop_mining();
```

## chain设计相关
### 区块
   No              |  Key           | Size-Byte        | Notes | RLP encoded size-byte 
  ---------------|----------------|------------------|:---------------------|----------------------
   1   |version        | 1         | "0x0" as initial default, increase block through version | 2 
   2   |chain_id       | 64       | chain_id := community name# hash(GenesisMinerPubkey + timestamp) | 66 
   3   |timestamp      | 8         | unix timestamp for winning the block package right| 9 
   4   |block_number   | 8          | block number| 9 
   5   |previous_block_hash  | 20   | hash of previous block| 21 
   6   |immutable_block_hash | 20   | hash of immutable point block| 21 
   7   |base_target     | 8          |  for POT - Proof of Transaction calculation| 9 
   8   |cumulative_difficulty | 8   | block chain difficulty parameter | 9 
   9   |generation_signature  | 32  | for POT calculation, $7 x power x time | 33 
   10  |tx_msg           | **less than 683** | packaged transaction in block, One block only have one transaction| **less than 686** 
   11  |miner_balance    | 8        | miner's balance| 9 
   12  |sender_balance   | 8        | sender's balance| 9 
   13  |receiver_balance | 8        | receiver's balance, nonce initial with 1 | 9 
   14  |sender_nonce     | 8        | sender's nonce(power) //change with fibonacci,  | 9 
   15  |signature        | 64       | r: 32 bytes, s: 32 bytes, when at #8 same difficulty, high signature number wins. | 66 
   16  |miner_pubkey+ip/port| 32    |  miner public key| 33 
   17  |node_id          | 32       |  node id in block for bootstrap | 33
   18  |ep| 32     | default record ipv4 and port, also support ipv6 and domain system| 33
  
问题记录：
- block size的确定，结合以太网MTU, 帧头，帧尾，IP头，UDP报文头，bencode编码等来确定最终block的大小。
- nonce的规则，第一次出现的receiver，以及nonce增加策略(change with fibonacci)

### 交易
No              |  Key           | Size-Byte        |  Notes| RLP encoded size-byte 
 ----------------|----------------|------------------|----------------------|----------------------
 1  |version     | 1         | "0x0" as initial default, increase block through version | 2 
 2  |chain_id    | 64        | Community ChainID := community name# hash(GenesisMInerPubkey + timestamp) | 66 
 3  |timestamp   | 8         | unix timestamp for this transaction. | 9 
 4  |tx_fee      | 8         | tx fee| 9 
 5  |sender_pubkey      | 32         | sender public key| 33 
 6  |nonce       | 8          | deny replicated tx | 9 
 7  |receiver_pubkey  |  32        | receiver Pubkey| 33 
 8  |amount      |  8         | wire amount| 9 
 9  |notes | 427 | explanation of this transaction | **less than 430**
 10 |signature   | 64         | r: 32 bytes, s: 32 bytes. | 66 
 
 问题记录：
- 单一支持wiring transaction, 如果是note交易，可以设定sender_pubkey == receiver_pubkey && amount == 0

## Alert业务相关
### 网络相关
- 网络设置相关
```
AlertType.PORTMAP //upnp, natpmp设置成功
AlertType.PORTMAP_ERROR
```
- 网络数据流量统计量相关
```
AlertType.SESSION_STATS //提供数据的下载，上传量以及速率
```

### communication信息流相关
- 设备
```
AlertType.TAU_DEVICE_NEW     //新device id通知
```
- 信息
```
AlertType.TAU_PAYLOAD_NEW        //新消息通知
AlertType.TAU_PAYLOAD_ROOT       //已读消息root通知
AlertType.TAU_PAYLOAD_SYNC       //正在同步的消息
```
- 节点
```
AlertType.TAU_NODE_DISCOVERY   //发现节点通知
AlertType.TAU_NODE_FROM_DEVICE   //发现多设备新节点通知
```

### chain相关
- 区块
```
AlertType.TAU_BLOCK_NEW     //新区块通知
```
- 交易
```
AlertType.TAU_TX_NEW        //新交易通知
```