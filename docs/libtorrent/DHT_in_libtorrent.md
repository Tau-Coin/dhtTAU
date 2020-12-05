### DHT in bittorrent

You'd better learn these two articles before reading this tutorial.
- [DHT protocol](http://bittorrent.org/beps/bep_0005.html)
- [Storing arbitrary data in the DHT](http://bittorrent.org/beps/bep_0044.html)

### DHT in libtorrent
The following discussion is based on [libtorrent-1.2.10](https://github.com/arvidn/libtorrent/releases/tag/libtorrent-1.2.10).
在libtorrent系统中，有四层结构：

	session_impl -> dht_traker -> node, rpc_manager -> protocols
    
    protocols包括: get_peers, get_item，sample_infohashes, put_data等等
    
    find_data, traversal_algorithm是核心的父类。
    
#### Get item in libtorrent

1. 计算target值，对于Immutable item而言，`target = sha1hash(content)`；对于Mutable item而言，		`target = sha1hash(key+salt)`
2. 调用`traversal_algorithm::start`开始进行查询工作；
3. 过程中会在Routing table中选取离target逻辑距离相邻的`k`个节点, `k = m_node.m_table.bucket_size()`
4. 对于选定的节点，发出request get请求: `traversal_algorithm::add_requests`，libtorrent系统中，为了提高查询效率，定义了`aggressive_lookups`的查询方式；
	> 选定的节点收到get请求，如果存在target值，则回复目标值；如果不存在，则回复离target值逻辑距离更近的节点；
5. 根据第一轮发出的请求节点回复，选择继续请求target内容或者终止请求；
	> 对于Immutable item发现目标值，则会直接终止请求；
	
	> 对于Mutable item而言，即使节点回复了对应于target下的目标值，还会继续请求，因为在mutable item系统中，需要请求到最大sequence值对应的数据；
6. 在libtorrent系统中，也存在找不到目标值的情况，下面我们会具体分析。

#### Put item in libtorrent
对于`put item`的操作，首先会相应的进行`get item`的操作，执行get的操作主要是为了查找离target值逻辑距离最近的节点信息(nodeid, ip, port以及token)

### DHT in TAU
We use ITEM system in dhtTAU for data communication. [Module-DHT]() was made to build a bridge between our applications and libtorrent.

There are three core sub modules in DHT:
- Sessions controller
> dhtTau use multi-sessions scheme to increase the diversity of nodes, improve getting efficiency and success ratio.
- Tau session
> Based on `jlibtorrent/SessionManager`, dhtTau warpped some customized variations.
- Request queque && Worker
> Through `DHTEngine API`, item requests are put into the request queue. Workers are responsible for the implementation of requests in the queue.
