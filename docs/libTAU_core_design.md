# Libtau应用端功能需求分析

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

## libtau业务相关
### communication
- 主循环间隔
```
	void set_main_loop_interval(int loop_time_interval);
```
- 添加朋友
```
	void add_friend(node_id id);
```
- 删除朋友
```
	void remove_friend(node_id id);
```
- 设置正在聊天的朋友
```
	void set_chatting_friend(node_id id);
```
- 设置活跃朋友
```
	void set_active_friend(node_id id);
```
- 发送新消息
```
	void send_new_message(node_id id, string message);
```

接口操作，影响的是应用层采用的db实例，这个实例定义了如下接口，传入libtau，communication可以操作
```
	int get_main_loop_interval();
    std::set<node_id> get_all_friend();
    node_id get_chatting_friend();
    std::list<node_id> get_active_friends();
    FriendInfo get_friend_info(node_id friend);
    std::list<message> get_latest_message_list(node_id friend, int num);
```
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
- 朋友
```
AlertType.TAU_FRIEND_DISCOVERY   //发现朋友通知
AlertType.TAU_FRIEND_FROM_DEVICE   //发现多设备新朋友通知
```