# Taucoin-android数据库设计

## 用户表（Users）设计
<table>
	<tr>
		<th>序号</th>
		<th>列名</th>
		<th>类型</th>
		<th>说明</th>
		<th>是否允许为空</th>
		<th>备注</th>
	</tr>
	<tr>
		<td>1</td>
		<td>publicKey</td>
		<td>TEXT</td>
		<td>用户的公钥</td>
		<td></td>
		<td>主键</td>
	</tr>
	<tr>
		<td>2</td>
		<td>seed</td>
		<td>TEXT</td>
		<td>用户的seed</td>
		<td>&radic;</td>
		<td>仅设备登陆用户有值</td>
	</tr>
	<tr>
		<td>3</td>
		<td>localName</td>
		<td>TEXT</td>
		<td>用户本地备注名</td>
		<td>&radic;</td>
		<td></td>
	</tr>
	<tr>
		<td>4</td>
		<td>isCurrentUser</td>
		<td>INTEGER</td>
		<td>是否是当前用户</td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>5</td>
		<td>lastUpdateTime</td>
		<td>INTEGER</td>
		<td>用户最后一次交易、出块、聊天时间</td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>6</td>
		<td>isBanned</td>
		<td>INTEGER</td>
		<td>用户是否被用户拉入黑名单</td>
		<td></td>
		<td></td>
	</tr>
</table>

- 增：
 - 默认创建用户；
 - 创建新用户；
 - onNewBlock、onRollback、onSyncBlock事件；
- 改：
 - 修改用户本地名；
 - 切换用户；
 - onNewBlock、onRollback、onSyncBlock事件；
 - 拉入或移出黑名单；
- 查:
 - UI显示等

## 社区表（Communities）设计
<table>
	<tr>
		<th>序号</th>
		<th>列名</th>
		<th>类型</th>
		<th>说明</th>
		<th>是否允许为空</th>
		<th>备注</th>
	</tr>
	<tr>
		<td>1</td>
		<td>chainID</td>
		<td>TEXT</td>
		<td>社区的chainID</td>
		<td></td>
		<td>主键</td>
	</tr>
	<tr>
		<td>2</td>
		<td>communityName</td>
		<td>TEXT</td>
		<td>社区名字</td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>3</td>
		<td>totalBlocks</td>
		<td>INTEGER</td>
		<td>社区总区块数</td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>4</td>
		<td>syncBlock</td>
		<td>INTEGER</td>
		<td>社区已同步到的区块号</td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>5</td>
		<td>isBanned</td>
		<td>INTEGER</td>
		<td>社区是否被用户拉入黑名单</td>
		<td></td>
		<td></td>
	</tr>
</table>

- 增：
 - 创建社区
 - onNewBlock、onSyncBlock事件；
- 改：
 - onNewBlock、onRollback、onSyncBlock事件；
 - 加入或移出黑名单；
- 查:
 - UI显示等

## 成员表（Members）设计
<table>
	<tr>
		<th>序号</th>
		<th>列名</th>
		<th>类型</th>
		<th>说明</th>
		<th>是否允许为空</th>
		<th>备注</th>
	</tr>
	<tr>
		<td>1</td>
		<td>chainID</td>
		<td>TEXT</td>
		<td>成员所属社区的chainID</td>
		<td></td>
		<td>主键</td>
	</tr>
	<tr>
		<td>2</td>
		<td>publicKey</td>
		<td>TEXT</td>
		<td>成员的公钥</td>
		<td></td>
		<td>主键</td>
	</tr>
	<tr>
		<td>3</td>
		<td>balance</td>
		<td>INTEGER</td>
		<td>成员的balance</td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>4</td>
		<td>power</td>
		<td>INTEGER</td>
		<td>成员的power</td>
		<td></td>
		<td></td>
	</tr>
</table>

注：chainID和publicKey共同组成复合主键，标识唯一性。

- 增：
 - onNewBlock、onSyncBlock事件；
- 改：
 - onNewBlock、onRollback、onSyncBlock事件；
 - 加入或移出黑名单；
- 查:
 - UI显示等

## 交易表（Txs）设计
<table>
	<tr>
		<th>序号</th>
		<th>列名</th>
		<th>类型</th>
		<th>说明</th>
		<th>是否允许为空</th>
		<th>备注</th>
	</tr>
	<tr>
		<td>1</td>
		<td>txID</td>
		<td>TEXT</td>
		<td>交易ID</td>
		<td></td>
		<td>主键</td>
	</tr>
	<tr>
		<td>2</td>
		<td>chainID</td>
		<td>TEXT</td>
		<td>交易所属社区chainID</td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>3</td>
		<td>senderPk</td>
		<td>TEXT</td>
		<td>交易发送者的公钥</td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>4</td>
		<td>fee</td>
		<td>INTEGER</td>
		<td>交易费</td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>5</td>
		<td>timestamp</td>
		<td>INTEGER</td>
		<td>交易时间戳</td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>6</td>
		<td>nonce</td>
		<td>INTEGER</td>
		<td>交易nonce</td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>7</td>
		<td>txType</td>
		<td>INTEGER</td>
		<td>交易类型，同MsgType中枚举类型</td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>8</td>
		<td>memo</td>
		<td>TEXT</td>
		<td> 交易的备注、描述、评论等</td>
		<td>&radic;</td>
		<td></td>
	</tr>
	<tr>
		<td>9</td>
		<td>txStatus</td>
		<td>INTEGER</td>
		<td>交易的状态</td>
		<td></td>
		<td>0：未上链（在交易池中）；1：上链成功 (不上链)</td>
	</tr>
	<tr>
		<td>10</td>
		<td>receiverPk</td>
		<td>TEXT</td>
		<td>交易接收者的公钥</td>
		<td>&radic;</td>
		<td>只针对MsgType.Wiring类型</td>
	</tr>
	<tr>
		<td>11</td>
		<td>amount</td>
		<td>INTEGER</td>
		<td>交易金额</td>
		<td></td>
		<td>只针对MsgType.Wiring类型</td>
	</tr>
</table>

- 增：
 - 发送新的交易；
 - onNewBlock、onRollback、onSyncBlock事件；
- 改：
 - onNewBlock、onRollback事件；
- 查:
 - UI显示等


## 消息表（Messages）设计
<table>
	<tr>
		<th>序号</th>
		<th>列名</th>
		<th>类型</th>
		<th>说明</th>
		<th>是否允许为空</th>
		<th>备注</th>
	</tr>
	<tr>
		<td>1</td>
		<td>msgID</td>
		<td>TEXT</td>
		<td>消息的ID</td>
		<td></td>
		<td>主键</td>
	</tr>
	<tr>
		<td>2</td>
		<td>chainID</td>
		<td>TEXT</td>
		<td>消息所属社区chainID</td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>3</td>
		<td>senderPk</td>
		<td>TEXT</td>
		<td>消息发送者的公钥</td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>4</td>
		<td>timestamp</td>
		<td>INTEGER</td>
		<td>消息时间戳</td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>5</td>
		<td>context</td>
		<td>TEXT</td>
		<td>消息内容</td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>6</td>
		<td>replyID</td>
		<td>TEXT</td>
		<td>被回复的消息ID</td>
		<td>&radic;</td>
		<td></td>
	</tr>
</table>
- 增：
 - 发送新的消息；
- 查:
 - UI显示等

 
 ## 收藏表（Favorites）设计
<table>
	<tr>
		<th>序号</th>
		<th>列名</th>
		<th>类型</th>
		<th>说明</th>
		<th>是否允许为空</th>
		<th>备注</th>
	</tr>
	<tr>
		<td>1</td>
		<td>ID</td>
		<td>TEXT</td>
		<td>消息或交易的ID</td>
		<td></td>
		<td>主键</td>
	</tr>
	<tr>
		<td>2</td>
		<td>communityName</td>
		<td>TEXT</td>
		<td>所属社区名</td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>3</td>
		<td>senderPk</td>
		<td>TEXT</td>
		<td>发送者的公钥</td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>4</td>
		<td>timestamp</td>
		<td>INTEGER</td>
		<td>收藏创建时间</td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>5</td>
		<td>context</td>
		<td>TEXT</td>
		<td>消息内容</td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>6</td>
		<td>replyID</td>
		<td>TEXT</td>
		<td>被回复的消息ID</td>
		<td>&radic;</td>
		<td></td>
	</tr>
</table>
- 增：
 - 收藏；
- 删:
 - 取消收藏
