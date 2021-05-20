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

### 信息流相关
- 设备
```
AlertType.TAU_DEVICE_NEW     //新device id通知
```
- 信息
```
AlertType.TAU_MESSAGE_NEW        //新消息通知
AlertType.TAU_MESSAGE_ROOT       //已读消息root通知
AlertType.TAU_MESSAGE_SYNC       //正在同步的消息
```
- 节点
```
AlertType.TAU_FRIEND_DISCOVERY   //发现节点通知
AlertType.TAU_FRIEND_FROM_DEVICE   //发现多设备新节点通知
```
