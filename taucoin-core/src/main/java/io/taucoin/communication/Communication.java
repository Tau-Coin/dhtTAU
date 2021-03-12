package io.taucoin.communication;

import com.frostwire.jlibtorrent.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import io.taucoin.account.AccountManager;
import io.taucoin.account.KeyChangedListener;
import io.taucoin.core.Bloom;
import io.taucoin.core.DataIdentifier;
import io.taucoin.core.FriendList;
import io.taucoin.core.FriendPair;
import io.taucoin.core.MutableDataWrapper;
import io.taucoin.core.OnlineSignal;
import io.taucoin.db.DBException;
import io.taucoin.db.MessageDB;
import io.taucoin.dht2.DHT;
import io.taucoin.dht2.DHTEngine;
import io.taucoin.listener.MsgListener;
import io.taucoin.param.ChainParam;
import io.taucoin.types.GossipItem;
import io.taucoin.types.HashList;
import io.taucoin.types.Message;
import io.taucoin.types.MutableDataType;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.HashUtil;

import static io.taucoin.param.ChainParam.SHORT_ADDRESS_LENGTH;

public class Communication implements DHT.GetMutableItemCallback, KeyChangedListener {
    private static final Logger logger = LoggerFactory.getLogger("Communication");

    // 朋友禁止访问时间，根据dht short time设定
    private final int BAN_TIME = 1; // 1 s

    // 判断朋友不在线时间
//    private final int LOSE_TOUCH_TIME  = 300; // 5 min

    // 主循环间隔最小时间
    private final int DEFAULT_LOOP_INTERVAL_TIME = 50; // 50 ms

    // 主循环间隔时间
    private int loopIntervalTime = DEFAULT_LOOP_INTERVAL_TIME;

    // 设备ID
    private final byte[] deviceID;

    private final MsgListener msgListener;

    // message db
    private final MessageDB messageDB;

    // 当前我加的朋友集合（完整公钥）
    private final Set<ByteArrayWrapper> friends = new CopyOnWriteArraySet<>();

    // 朋友被禁止访问的时间
    private final Map<ByteArrayWrapper, BigInteger> friendBannedTime = new ConcurrentHashMap<>();

    // 多设备发现的朋友
    private final Set<ByteArrayWrapper> friendsFromRemote = new CopyOnWriteArraySet<>();

    // 我的朋友的最新消息的时间戳 <friend, timestamp>（完整公钥）
    private final Map<ByteArrayWrapper, BigInteger> friendLastSeen = new ConcurrentHashMap<>();

    // 当前发现的等待通知的在线的朋友集合（完整公钥）
    private final Map<ByteArrayWrapper, BigInteger> onlineFriendsToNotify = new ConcurrentHashMap<>();;

    // 得到的消息集合（hash <--> Message），ConcurrentHashMap是支持并发操作的集合
    private final Map<ByteArrayWrapper, Message> messageMap = new ConcurrentHashMap<>();

    // 得到的消息与发送者对照表（Message hash <--> Sender）
    private final Map<ByteArrayWrapper, ByteArrayWrapper> messageSenderMap = new ConcurrentHashMap<>();

    // 与朋友通信消息的集合（friend pair（完整公钥） <--> Latest Message List），最新的消息放在最后，ConcurrentHashMap是支持并发操作的集合
    private final Map<ByteArrayWrapper, LinkedList<Message>> messageListMap = new ConcurrentHashMap<>();

    // 待处理的在线信号集合（peer <--> 在线信号）
    private final Map<ByteArrayWrapper, OnlineSignal> onlineSignalCache = new ConcurrentHashMap<>();

    // 新发现的，等待通知UI的confirmation root <message hash, friend>（完整公钥）
    private final Map<ByteArrayWrapper, byte[]> friendConfirmationRootToNotify = new ConcurrentHashMap<>();

    // 最新的在线信号时间 <friend, timestamp>（完整公钥）
    private final Map<ByteArrayWrapper, BigInteger> latestOnlineSignalTime = new ConcurrentHashMap<>();

    // 发现的我的朋友跟我聊天的最新时间 <friend, time>（完整公钥）
    private final Map<ByteArrayWrapper, BigInteger> friendChattingTime = new ConcurrentHashMap<>();

    // 通过gossip推荐机制发现的有新消息的朋友集合（完整公钥）
    private final Set<ByteArrayWrapper> referredFriends = new CopyOnWriteArraySet<>();

    // 通过gossip机制打听到的跟朋友聊天的time <FriendPair, Timestamp>（完整公钥）
    private final Map<FriendPair, BigInteger> friendGossipChattingTime = new ConcurrentHashMap<>();

    // 当前正在聊天的朋友（完整公钥）
    private final Map<ByteArrayWrapper, BigInteger> chattingFriend = new ConcurrentHashMap<>();

    // Communication thread.
    private Thread communicationThread;

    public Communication(byte[] deviceID, MessageDB messageDB, MsgListener msgListener) {
        this.deviceID = deviceID;
        this.messageDB = messageDB;
        this.msgListener = msgListener;
    }


    /**
     * 初始化，获取朋友列表以及最新消息
     * @return true if success, false otherwise
     */
    private boolean init() {
        try {
            // get friends
            Set<byte[]> friends = this.messageDB.getFriends();
            // 我的公钥
            byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

            if (null != friends) {
                for (byte[] friend: friends) {
                    ByteArrayWrapper key = new ByteArrayWrapper(friend);
                    this.friends.add(key);

                    logger.debug("My friend:{}", key.toString());

                    // 获取我发给朋友的最近的消息
                    FriendPair friendPair = new FriendPair(pubKey, friend);
                    LinkedList<Message> linkedList = new LinkedList<>();
                    // 获取最新消息的编码
                    byte[] encode = this.messageDB.getLatestMessageHashListEncode(friendPair);
                    if (null != encode) {
                        HashList hashList = new HashList(encode);
                        for (byte[] hash : hashList.getHashList()) {
                            logger.debug("Hash:{}", Hex.toHexString(hash));
                            byte[] msgEncode = this.messageDB.getMessageByHash(hash);
                            if (null != msgEncode) {
                                linkedList.add(new Message(msgEncode));
                            }
                        }
                    }

                    this.messageListMap.put(key, linkedList);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * 保存朋友的最新消息哈希列表
     * @param friend 通信的朋友
     * @throws DBException database exception
     */
    private void saveFriendLatestMessageHashList(ByteArrayWrapper friend) throws DBException {
        LinkedList<Message> linkedList = this.messageListMap.get(friend);

        if (null != linkedList && !linkedList.isEmpty()) {
            List<byte[]> list = new ArrayList<>();
            for (Message message : linkedList) {
                try {
                    logger.debug("message hash:{}", Hex.toHexString(message.getHash()));
                    list.add(message.getHash());
                } catch (RuntimeException e) {
                    logger.error(e.getMessage(), e);
                }
            }

            HashList hashList = new HashList(list);
            byte[] pubKey = AccountManager.getInstance().getKeyPair().first;
            FriendPair friendPair = new FriendPair(pubKey, friend.getData());
            this.messageDB.saveLatestMessageHashListEncode(friendPair, hashList.getEncoded());
        }
    }

    /**
     * 尝试往聊天消息集合里面插入新消息
     * @param friend 通信的朋友
     * @param message 新消息
     */
    private void tryToUpdateLatestMessageList(ByteArrayWrapper friend, Message message) throws DBException {
        LinkedList<Message> linkedList = this.messageListMap.get(friend);

        // 更新成功标志
        boolean updated = false;

        if (null != linkedList) {
            if (!linkedList.isEmpty()) {
                try {
                    // 先判断一下是否比最后一个消息时间戳大，如果是，则直接插入末尾
                    if (message.getTimestamp().compareTo(linkedList.getLast().getTimestamp()) > 0) {
                        linkedList.add(message);
                        updated = true;
                    } else {
                        // 寻找从后往前寻找第一个时间小于当前消息时间的消息，将当前消息插入到到该消息后面
                        Iterator<Message> it = linkedList.descendingIterator();
                        while (it.hasNext()) {
                            Message reference = it.next();
                            int diff = reference.getTimestamp().compareTo(message.getTimestamp());
                            // 如果差值小于零，说明找到了比当前消息时间戳小的消息位置，将消息插入到目标位置后面一位
                            if (diff < 0) {
                                updated = true;
                            } else if (diff == 0) {
                                // 如果时间戳一样，并且是不同的消息，则认为后来的消息时间戳更大
                                if (!Arrays.equals(reference.getHash(), message.getHash())) {
                                    updated = true;
                                } else {
                                    // 如果哈希一样，则本身已经在列表中，不再进行查找
                                    break;
                                }
                            }
                            if (updated) {
                                int i = linkedList.indexOf(reference);
                                linkedList.add(i + 1, message);
                                break;
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    logger.error(e.getMessage(), e);
                }
            } else {
                linkedList.add(message);
                updated = true;
            }
        } else {
            linkedList = new LinkedList<>();
            linkedList.add(message);
            updated = true;
        }

        // 更新成功
        if (updated) {
            // 如果更新了消息列表，则判断是否列表长度过长，过长则删掉旧数据，然后停止循环
            if (linkedList.size() > ChainParam.BLOOM_FILTER_MESSAGE_SIZE) {
                linkedList.removeFirst();
            }

            this.messageListMap.put(friend, linkedList);

            saveFriendLatestMessageHashList(friend);
        }
    }

    /**
     * 构建短地址
     * @param pubKey public key
     * @return short address
     */
    private byte[] makeShortAddress(byte[] pubKey) {
        if (pubKey.length > SHORT_ADDRESS_LENGTH) {
            byte[] shortAddress = new byte[SHORT_ADDRESS_LENGTH];
            System.arraycopy(pubKey, 0, shortAddress, 0, SHORT_ADDRESS_LENGTH);
            return shortAddress;
        }

        return pubKey;
    }

    /**
     * 使用短地址来构建gossip item
     * @param sender sender public key
     * @param timestamp timestamp
     * @return gossip item
     */
    private GossipItem makeGossipItemWithShortAddress(byte[] sender, BigInteger timestamp) {
        byte[] shortSender = new byte[SHORT_ADDRESS_LENGTH];
        System.arraycopy(sender, 0, shortSender, 0, SHORT_ADDRESS_LENGTH);

        return new GossipItem(shortSender, timestamp);
    }

    /**
     * 通知UI发现的还在线的朋友
     */
    private void notifyUIOnlineFriend() {
        for (Map.Entry<ByteArrayWrapper, BigInteger> entry: this.onlineFriendsToNotify.entrySet()) {
            logger.trace("Notify UI online friend:{}", entry.getKey().toString());
            this.msgListener.onDiscoveryFriend(entry.getKey().getData(), entry.getValue());

            this.onlineFriendsToNotify.remove(entry.getKey());
        }
    }

    /**
     * 通知UI新发现的朋友的已读消息的root
     */
    private void notifyUIReadMessageRoot() {
        for (Map.Entry<ByteArrayWrapper, byte[]> entry : this.friendConfirmationRootToNotify.entrySet()) {
            logger.debug("Notify UI read message from friend:{}, root:{}",
                    Hex.toHexString(entry.getValue()), entry.getKey().toString());

            this.msgListener.onReadMessageRoot(entry.getValue(), entry.getKey().getData());

            this.friendConfirmationRootToNotify.remove(entry.getKey());
        }
    }

    /**
     * 从朋友列表获取完整公钥
     * @param pubKey public key
     * @return 完整的公钥
     */
    private ByteArrayWrapper getCompletePubKeyFromFriend(byte[] pubKey) {
        for (ByteArrayWrapper key: this.friends) {
            if (ByteUtil.startsWith(key.getData(), pubKey)) {
                return key;
            }
        }

        return null;
    }

    /**
     * 合并来自多设备的朋友列表
     * @throws DBException database exception
     */
    private void tryToMergeFriends() throws DBException {
        for (ByteArrayWrapper friend: this.friendsFromRemote) {
            addNewFriend(friend.getData());

            if (!this.friends.contains(friend)) {
                this.msgListener.onNewFriendFromMultiDevice(friend.getData());
            }

            this.friendsFromRemote.remove(friend);
        }
    }

    /**
     * 处理收到的消息
     * @throws DBException database exception
     */
    private void dealWithMessage() throws DBException {

        // 将消息存入数据库并通知UI，尝试获取下一条消息
        Iterator<Map.Entry<ByteArrayWrapper, Message>> iterator = this.messageMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<ByteArrayWrapper, Message> entry = iterator.next();
            Message message = entry.getValue();
            ByteArrayWrapper msgHash = entry.getKey();

            ByteArrayWrapper sender = this.messageSenderMap.get(msgHash);

            try {
                // save to db
                this.messageDB.putMessage(message.getHash(), message.getEncoded());
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }

            // 更新最新消息列表
            tryToUpdateLatestMessageList(sender, message);

            this.messageSenderMap.remove(msgHash);
            this.messageMap.remove(msgHash);
//            iterator.remove();
        }
    }

    /**
     * 处理收到的在线信号
     */
    private void dealWithOnlineSignal() {
        Set<ByteArrayWrapper> peersToPutOnlineSignal = new HashSet<>();

        for (Map.Entry<ByteArrayWrapper, OnlineSignal> entry: this.onlineSignalCache.entrySet()) {
            boolean publish = false;

            ByteArrayWrapper peer = entry.getKey();
            OnlineSignal onlineSignal = entry.getValue();

            Bloom messageBloomFilter = onlineSignal.getMessageBloomFilter();
            Bloom friendListBloomFilter = onlineSignal.getFriendListBloomFilter();

            byte[] pubKey = AccountManager.getInstance().getKeyPair().first;
            if (Arrays.equals(pubKey, peer.getData()) && null != friendListBloomFilter) {
                // 是另外一台设备
                List<byte[]> friends = new ArrayList<>();
                for (ByteArrayWrapper friend : this.friends) {
                    Bloom bloom = Bloom.create(HashUtil.sha1hash(friend.getData()));
                    if (!friendListBloomFilter.matches(bloom)) {
                        // 发现不在对方朋友列表
                        friends.add(friend.getData());

                        if (friends.size() >= ChainParam.MAX_FRIEND_LIST_SIZE) {
                            break;
                        }
                    }
                }

                if (!friends.isEmpty()) {
                    FriendList friendList = new FriendList(friends);
                    publishFriendList(peer.getData(), friendList);
                }
            } else {
                logger.debug("peer:{},{}", peer.toString(), onlineSignal.toString());
                // 比较双方我发的消息的bloom filter，如果不同，则发出一个对方没有的数据
                LinkedList<Message> list = this.messageListMap.get(peer);

                if (null != list && !list.isEmpty()) {
                    int size = list.size();
                    byte[] firstMsgHash = list.getFirst().getSha1Hash();
                    byte[] lastMsgHash = list.getLast().getSha1Hash();
                    boolean previousMatch = true;

                    Bloom bloom = Bloom.create(firstMsgHash);
                    if (!messageBloomFilter.matches(bloom)) {
                        logger.error("Put [{}] message to {}", 0, peer.toString());
                        publishMessage(peer.getData(), list.getFirst());
                        publish = true;
                        previousMatch = false;
                    }

                    for (int i = 0; i < size - 1; i++) {
                        byte[] mergedHash = ByteUtil.merge(list.get(i).getSha1Hash(), list.get(i + 1).getSha1Hash());
                        bloom = Bloom.create(HashUtil.sha1hash(mergedHash));
                        boolean match = messageBloomFilter.matches(bloom);
                        // 如果合并哈希不匹配，则随机发出一个缺少的消息即可
                        if (!match && !publish) {
                            // 根据时间随机put一个
                            if (System.currentTimeMillis() % 2 == 0) {
                                logger.error("Put [{}] message to {}", i, peer.toString());
                                publishMessage(peer.getData(), list.get(i));
                            } else {
                                logger.error("Put [{}] message to {}", i + 1, peer.toString());
                                publishMessage(peer.getData(), list.get(i + 1));
                            }
                            publish = true;
                        }
                        // 前后两个合并哈希都匹配，才确认收到
                        if (previousMatch && match) {
                            Message message = list.get(i);
                            if (Arrays.equals(pubKey, message.getSender())) {
                                logger.error("confirmation root:{}", Hex.toHexString(message.getHash()));
                                // 若匹配，则大概率对方收到了该消息，记为confirmation root，后续会通知UI
                                this.friendConfirmationRootToNotify.
                                        put(new ByteArrayWrapper(message.getHash()), peer.getData());
                            }
                        }

                        previousMatch = match;
                    }

                    bloom = Bloom.create(lastMsgHash);
                    boolean match = messageBloomFilter.matches(bloom);
                    if (!match && !publish) {
                        logger.error("Put last message to {}", peer.toString());
                        publishMessage(peer.getData(), list.getLast());
                    }
                    // 前后两个合并哈希都匹配，才确认收到
                    if (previousMatch && match) {
                        Message message = list.getLast();
                        if (Arrays.equals(pubKey, message.getSender())) {
                            // 若匹配，则大概率对方收到了该消息，记为confirmation root，后续会通知UI
                            this.friendConfirmationRootToNotify.
                                    put(new ByteArrayWrapper(message.getHash()), peer.getData());
                        }
                    }
                }
            }

            byte[] chattingFriend = onlineSignal.getChattingFriend();
            if (Arrays.equals(pubKey, chattingFriend)) {
                // 如果是正在跟我聊天，判断一下上次标记聊天时间戳是否最新
                ByteArrayWrapper sender = new ByteArrayWrapper(chattingFriend);
                BigInteger latestTimestamp = this.friendChattingTime.get(sender);
                // 如果发现更新的推荐，则加入推荐列表
                if (null == latestTimestamp || latestTimestamp.compareTo(onlineSignal.getChattingTime()) < 0) {
                    // 记录最新的聊天时间
                    this.friendChattingTime.put(sender, onlineSignal.getChattingTime());
                    referToFriend(sender);
                }
            } else {
                // 记录下推荐给别人的聊天时间
                FriendPair friendPair = new FriendPair(peer.getData(), chattingFriend);
                BigInteger latestTimestamp = this.friendGossipChattingTime.get(friendPair);
                if (null == latestTimestamp || latestTimestamp.compareTo(onlineSignal.getChattingTime()) < 0) {
                    this.friendGossipChattingTime.put(friendPair, onlineSignal.getChattingTime());
                }
            }


            if (null != onlineSignal.getGossipItemList()) {

                // 信任发送方自己给的gossip信息
                for (GossipItem gossipItem : onlineSignal.getGossipItemList()) {
                    logger.trace("Got gossip: {} from peer[{}]", gossipItem.toString(), peer.toString());

                    ByteArrayWrapper sender = new ByteArrayWrapper(gossipItem.getSender());

                    // 发送者是我自己的gossip信息直接忽略，因为我自己的信息不需要依赖gossip
                    if (ByteUtil.startsWith(pubKey, gossipItem.getSender())) {
                        logger.trace("Sender[{}] is me.", sender.toString());
                        continue;
                    }

                    BigInteger latestTimestamp = this.friendChattingTime.get(sender);
                    // 如果发现更新的推荐，则加入推荐列表
                    if (null == latestTimestamp || latestTimestamp.compareTo(gossipItem.getTimestamp()) < 0) {
                        // 记录最新的聊天时间
                        this.friendChattingTime.put(sender, gossipItem.getTimestamp());
                        referToFriend(sender);
                    }
                }
            }

            // 如果没有publish，则publish我的在线信号
//            if (!publish) {
                peersToPutOnlineSignal.add(peer);
//            }

            this.onlineSignalCache.remove(entry.getKey());
        }
        // TODO:: 考虑put频率过高的问题，等待优化

        // 发送在线信号
        for (ByteArrayWrapper peer: peersToPutOnlineSignal) {
            publishFriendOnlineSignal(peer.getData());
        }
    }

    /**
     * 将打听到的朋友加入推荐列表，并从禁止列表删除
     * @param friend 推荐的朋友
     */
    private void referToFriend(ByteArrayWrapper friend) {
        this.referredFriends.add(friend);
//        this.friendBannedTime.remove(friend);
    }

    /**
     * 挑选一个推荐的朋友访问，没有则随机挑一个访问
     */
    private void visitReferredFriends() {
        ByteArrayWrapper peer = null;
        Iterator<ByteArrayWrapper> it = this.referredFriends.iterator();
        // 如果有现成的peer，则挑选一个peer访问
        if (it.hasNext()) {
            peer = it.next();

            this.referredFriends.remove(peer);
        } else {
            // 没有找到推荐的活跃的peer，则自己随机访问一个自己的朋友或者自己
            Iterator<ByteArrayWrapper> iterator = this.friends.iterator();
            if (iterator.hasNext()) {

                Random random = new Random(System.currentTimeMillis());
                // 取值范围0 ~ size，当index取size时选自己
                int index = random.nextInt(this.friends.size() + 1);

                if (index != this.friends.size()) {
                    // (0 ~ size)
                    int i = 0;
                    while (iterator.hasNext()) {
                        peer = iterator.next();
                        if (i == index) {
                            break;
                        }

                        i++;
                    }
                } else {
                    peer = new ByteArrayWrapper(AccountManager.getInstance().getKeyPair().first);
                }
            }
        }

        if (null != peer) {
            // 如果选中的朋友没在禁止列表的禁止期，则访问它
            long currentTime = System.currentTimeMillis() / 1000;
            BigInteger timestamp = this.friendBannedTime.get(peer);
            if (null == timestamp || currentTime - this.BAN_TIME >= timestamp.longValue() ) {
                // 没在禁止列表
                requestMutableDataFromPeer(peer);
                this.friendBannedTime.put(peer, BigInteger.valueOf(currentTime + this.BAN_TIME));
            }
        }
    }

    /**
     * 发布某朋友的在线信号
     * @param friend 该朋友
     */
    private void publishFriendOnlineSignal(byte[] friend) {
        OnlineSignal onlineSignal = makePeerOnlineSignal(friend);

        publishOnlineSignal(friend, onlineSignal);
    }

    /**
     * 构造某个朋友的在线信号
     * @param friend 某朋友
     * @return 在线信号
     */
    private OnlineSignal makePeerOnlineSignal(byte[] friend) {
        Bloom messageBloomFilter = new Bloom();
        Bloom friendListBloomFilter = null;
        byte[] chattingFriend = null;
        BigInteger chattingTime = BigInteger.ZERO;
        List<GossipItem> gossipItemList = new ArrayList<>();

        ByteArrayWrapper key = new ByteArrayWrapper(friend);
        LinkedList<Message> messages = this.messageListMap.get(key);
        if (null != messages && !messages.isEmpty()) {
            logger.error("---peer:{}, {}", Hex.toHexString(friend), messages.toString());
            int size = messages.size();
            byte[] firstMsgHash = messages.getFirst().getSha1Hash();
            byte[] lastMsgHash = messages.getLast().getSha1Hash();
            Bloom bloom = Bloom.create(firstMsgHash);
            messageBloomFilter.or(bloom);
            bloom = Bloom.create(lastMsgHash);
            messageBloomFilter.or(bloom);

            for (int i = 0; i < size - 1; i++) {
                byte[] mergedHash = ByteUtil.merge(messages.get(i).getSha1Hash(), messages.get(i + 1).getSha1Hash());
                bloom = Bloom.create(HashUtil.sha1hash(mergedHash));
                messageBloomFilter.or(bloom);
            }
        }

        byte[] pubKey = AccountManager.getInstance().getKeyPair().first;
        if (Arrays.equals(pubKey, friend)) {
            friendListBloomFilter = new Bloom();
            for (ByteArrayWrapper peer : this.friends) {
                Bloom bloom = Bloom.create(HashUtil.sha1hash(peer.getData()));
                friendListBloomFilter.or(bloom);
            }
        }

        Iterator<Map.Entry<ByteArrayWrapper, BigInteger>> iterator = this.chattingFriend.entrySet().iterator();
        if (iterator.hasNext()) {
            Map.Entry<ByteArrayWrapper, BigInteger> entry = iterator.next();
            chattingFriend = entry.getKey().getData();
            chattingTime = entry.getValue();

            iterator.remove();
        }

        // TODO:: 测量极限容量
        Iterator<Map.Entry<FriendPair, BigInteger>> it = this.friendGossipChattingTime.entrySet().iterator();
        int i = 1;
        while (it.hasNext()) {
            Map.Entry<FriendPair, BigInteger> entry = it.next();
            // 查找接收者是peer的
            if (Arrays.equals(entry.getKey().receiver, friend)) {
                gossipItemList.add(new GossipItem(entry.getKey().sender, entry.getValue()));

                i--;
                it.remove();
            }

            if (i <= 0) {
                break;
            }
        }

        return new OnlineSignal(messageBloomFilter, friendListBloomFilter, chattingFriend, chattingTime, gossipItemList);
    }

    /**
     * 发布在线信号
     * @param friend 发送对象
     * @param onlineSignal 发送的在线信号
     */
    private void publishOnlineSignal(byte[] friend, OnlineSignal onlineSignal) {
        if (null != onlineSignal) {
            logger.debug("publish friend[{}] online signal:{}", Hex.toHexString(friend), onlineSignal.toString());
            MutableDataWrapper mutableDataWrapper = new MutableDataWrapper(MutableDataType.ONLINE_SIGNAL,
                    onlineSignal.getEncoded());
            publishMutableData(friend, mutableDataWrapper.getEncoded());
        }
    }

    /**
     * 发布朋友列表
     * @param friendList friend list to publish
     */
    private void publishFriendList(byte[] friend, FriendList friendList) {
        if (null != friendList) {
            MutableDataWrapper mutableDataWrapper = new MutableDataWrapper(MutableDataType.FRIEND_LIST,
                    friendList.getEncoded());
            publishMutableData(friend, mutableDataWrapper.getEncoded());
        }
    }

    /**
     * 发布消息
     * @param message msg to publish
     */
    private void publishMessage(byte[] friend, Message message) {
        if (null != message) {
            MutableDataWrapper mutableDataWrapper = new MutableDataWrapper(MutableDataType.MESSAGE,
                    message.getEncoded());
            publishMutableData(friend, mutableDataWrapper.getEncoded());
        }
    }

    /**
     * 发布mutable数据
     * @param peer 发布数据的对象
     * @param data 发布的数据
     */
    private void publishMutableData(byte[] peer, byte[] data) {
        // put mutable item
        Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();

        byte[] salt = makeSendingSalt(keyPair.first, peer);
        if (null != data) {
            DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first,
                    keyPair.second, data, salt);
            DHTEngine.getInstance().distribute(mutableItem, null, null);
        }
    }

    /**
     * 向某个peer请求mutable数据
     * @param peer public key
     */
    private void requestMutableDataFromPeer(ByteArrayWrapper peer) {
        if (null != peer) {
            logger.trace("Request mutable data from peer:{}", peer.toString());

            byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

            byte[] salt = makeReceivingSalt(pubKey, peer.getData());
            DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer.getData(), salt);
            DataIdentifier dataIdentifier = new DataIdentifier(peer);

            DHTEngine.getInstance().request(spec, this, dataIdentifier);
        }
    }

    /**
     * 主循环
     */
    private void mainLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // 合并来自多设备的朋友
                tryToMergeFriends();

                // 通知UI发现的在线朋友
                notifyUIOnlineFriend();

                // 通知UI已读root
                notifyUIReadMessageRoot();

                // 处理获得的消息
                dealWithMessage();

                // 处理在线信号
                dealWithOnlineSignal();

                // 访问通过gossip机制推荐的活跃peer
                visitReferredFriends();

                try {
                    Thread.sleep(this.loopIntervalTime);
                } catch (InterruptedException e) {
                    logger.info(e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
            } catch (DBException e) {
                this.msgListener.onMsgError("Data Base Exception!");
                logger.error(e.getMessage(), e);

                try {
                    Thread.sleep(this.DEFAULT_LOOP_INTERVAL_TIME);
                } catch (InterruptedException ex) {
                    logger.info(ex.getMessage(), ex);
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);

                try {
                    Thread.sleep(this.DEFAULT_LOOP_INTERVAL_TIME);
                } catch (InterruptedException ex) {
                    logger.info(ex.getMessage(), ex);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * 获取间隔时间
     * @return 间隔时间
     */
    public int getIntervalTime() {
        return this.loopIntervalTime;
    }

    /**
     * 设置最小间隔时间
     */
    public void setIntervalTime(int intervalTime) {
        this.loopIntervalTime = intervalTime;
    }

    /**
     * save message data in database
     * @param message msg
     * @throws DBException database exception
     */
    private void saveMessageDataInDB(Message message) throws DBException {
        if (null != message) {
            this.messageDB.putMessage(message.getHash(), message.getEncoded());
        }
    }

    /**
     * 更新发给朋友的信号以及消息列表等信息
     * @param friend friend to send msg
     * @param message msg
     */
    private void updateMessageInfoToFriend(byte[] friend, Message message) throws DBException {
        chattingWithFriend(friend);

        tryToUpdateLatestMessageList(new ByteArrayWrapper(friend), message);
    }

    /**
     * 向朋友发布新消息
     * @param friend 朋友公钥
     * @param message 新消息
     * @return true:接受该消息， false:拒绝该消息
     */
    public boolean publishNewMessage(byte[] friend, Message message) {
        try {
            if (null != message) {
                logger.debug("Publish message:{}", message.toString());

                saveMessageDataInDB(message);
                updateMessageInfoToFriend(friend, message);
                publishMessage(friend, message);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return true;
    }

    /**
     * 构造mutable数据频道发送侧salt
     * @param pubKey my public key
     * @param friend friend public key
     * @return salt
     */
    private byte[] makeSendingSalt(byte[] pubKey, byte[] friend) {
        byte[] salt = new byte[SHORT_ADDRESS_LENGTH * 2];
        System.arraycopy(pubKey, 0, salt, 0, SHORT_ADDRESS_LENGTH);
        System.arraycopy(friend, 0, salt, SHORT_ADDRESS_LENGTH, SHORT_ADDRESS_LENGTH);

        return salt;
    }

    /**
     * 构造mutable数据频道接收侧salt
     * @param pubKey my public key
     * @param friend friend public key
     * @return salt
     */
    private byte[] makeReceivingSalt(byte[] pubKey, byte[] friend) {
        byte[] salt = new byte[SHORT_ADDRESS_LENGTH * 2];
        System.arraycopy(friend, 0, salt, 0, SHORT_ADDRESS_LENGTH);
        System.arraycopy(pubKey, 0, salt, SHORT_ADDRESS_LENGTH, SHORT_ADDRESS_LENGTH);

        return salt;
    }

    /**
     * 获取最新聊天消息的哈希集合
     * @param friend friend
     * @return hash list
     */
    private ArrayList<byte[]> getLatestMessageHashList(byte[] friend) {
        ArrayList<byte[]> list = new ArrayList<>();

        LinkedList<Message> linkedList = this.messageListMap.get(new ByteArrayWrapper(friend));
        if (null != linkedList && !linkedList.isEmpty()) {

            for (Message message: linkedList) {
                list.add(message.getHash());
            }
        }

        return list;
    }

    /**
     * 随机获取一个朋友
     * @return friend
     */
    private byte[] getFriendRandomly() {
        byte[] friend = null;
        int size = this.friends.size();
        if (size > 0) {
            Iterator<ByteArrayWrapper> it = this.friends.iterator();
            Random random = new Random(System.currentTimeMillis());
            int index = random.nextInt(size) + 1;

            int i = 0;
            while (it.hasNext() && i < index) {
                friend = it.next().getData();
                i++;
            }
        }

        return friend;
    }

    /**
     * 添加新朋友
     * @param pubKey public key
     * @throws DBException database exception
     */
    public void addNewFriend(byte[] pubKey) throws DBException {
        ByteArrayWrapper peer = new ByteArrayWrapper(pubKey);
        // 没有才添加
        if (!this.friends.contains(peer)) {
            byte[] myPubKey = AccountManager.getInstance().getKeyPair().first;

            // 朋友列表排除自己
            if (!Arrays.equals(myPubKey, pubKey)) {
                this.friends.add(peer);
                this.messageListMap.put(peer, new LinkedList<>());

                this.messageDB.addFriend(pubKey);
            }
        }
    }

    /**
     * 删除朋友
     * @param pubKey public key
     * @throws DBException database exception
     */
    public void delFriend(byte[] pubKey) throws DBException {
        // TODO:: to check
        this.messageDB.delFriend(pubKey);

        ByteArrayWrapper key = new ByteArrayWrapper(pubKey);
        this.friends.remove(key);

        Iterator<Map.Entry<FriendPair, BigInteger>> it = this.friendGossipChattingTime.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<FriendPair, BigInteger> entry = it.next();
            if (ByteUtil.startsWith(pubKey, entry.getKey().getReceiver())) {
                it.remove();
            }
        }
    }

    /**
     * 获取所有的朋友
     * @return 所有朋友的公钥
     */
    public List<byte[]> getAllFriends() {
        List<byte[]> list = new ArrayList<>();
        for (ByteArrayWrapper friend: this.friends) {
            list.add(friend.getData());
        }

        return list;
    }

    /**
     * 当前正在与某个朋友聊天
     * @param friend 正在与该朋友聊天
     */
    public void chattingWithFriend(byte[] friend) {
        this.chattingFriend.put(new ByteArrayWrapper(friend), BigInteger.valueOf(System.currentTimeMillis() / 1000));
    }

    /**
     * Start thread
     *
     * @return boolean successful or not.
     */
    public boolean start() {

        AccountManager.getInstance().addListener(this);

        if (!init()) {
            return false;
        }

        communicationThread = new Thread(this::mainLoop);
        communicationThread.start();

        return true;
    }

    /**
     * Stop thread
     */
    public void stop() {
        if (null != communicationThread) {
            communicationThread.interrupt();
        }

        AccountManager.getInstance().removeListener(this);
    }

    @Override
    public void onKeyChanged(Pair<byte[], byte[]> newKey) {
        ByteArrayWrapper key = new ByteArrayWrapper(newKey.first);
        this.friends.remove(key);
        this.friendsFromRemote.remove(key);
//        this.friendBannedTime.clear();
        this.friendLastSeen.clear();
        this.messageMap.clear();
        this.messageSenderMap.clear();
        this.messageListMap.clear();
        this.friendConfirmationRootToNotify.clear();
        this.friendChattingTime.clear();
        this.referredFriends.clear();
        this.friendGossipChattingTime.clear();
        this.chattingFriend.clear();

        init();
    }

    /**
     * 处理收到的mutable data
     * @param mutableDataWrapper 收到的mutable data
     * @param peer gossip发出的peer
     */
    private void processMutableData(MutableDataWrapper mutableDataWrapper, ByteArrayWrapper peer) {

        // 是否更新好友的在线时间（完整公钥）
        BigInteger timestamp = mutableDataWrapper.getTimestamp();
        BigInteger lastSeen = this.friendLastSeen.get(peer);

        if (null != lastSeen && lastSeen.compareTo(timestamp) > 0) {
            logger.debug("-----old mutable data from peer:{}", peer.toString());
        }

        // 判断时间戳，以避免处理历史数据
        if (null == lastSeen || lastSeen.compareTo(timestamp) < 0) { // 判断是否是更新的online signal
            logger.debug("Newer data from peer:{}", peer.toString());
            this.friendLastSeen.put(peer, timestamp);
            this.onlineFriendsToNotify.put(peer, timestamp);
        }
        switch (mutableDataWrapper.getMutableDataType()) {
            case MESSAGE: {
                Message message = new Message(mutableDataWrapper.getData());
                logger.debug("MESSAGE: Got message :{}", message.toString());
                this.messageMap.put(new ByteArrayWrapper(message.getHash()), message);
                this.messageSenderMap.put(new ByteArrayWrapper(message.getHash()), peer);

                this.msgListener.onNewMessage(peer.getData(), message);

                break;
            }
            case FRIEND_LIST: {
                byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

                if (Arrays.equals(pubKey, peer.getData())) {
                    FriendList friendList = new FriendList(mutableDataWrapper.getData());
                    List<byte[]> list = friendList.getFriendList();
                    if (null != list) {
                        for (byte[] friend : list) {
                            this.friendsFromRemote.add(new ByteArrayWrapper(friend));
                        }
                    }
                }

                break;
            }
            case ONLINE_SIGNAL: {
                BigInteger latestOnlineSignalTime = this.latestOnlineSignalTime.get(peer);
                // 判断时间戳，以避免处理历史数据
                if (null == latestOnlineSignalTime || latestOnlineSignalTime.compareTo(timestamp) < 0) { // 判断是否是更新的online signal
                    logger.debug("Newer online signal from peer:{}", peer.toString());
                    this.latestOnlineSignalTime.put(peer, timestamp);
                }

                // 处理更新的或者和当前记录的一样新的在线信号，避免上次处理对方完，对方依旧没有满足的问题
                if (null == latestOnlineSignalTime || latestOnlineSignalTime.compareTo(timestamp) <= 0) {
                    OnlineSignal onlineSignal = new OnlineSignal(mutableDataWrapper.getData());
                    this.onlineSignalCache.put(peer, onlineSignal);
                }

                break;
            }
            case UNKNOWN: {
                logger.error("Unknown type.");
            }
        }
        /* else {
            // 如果拿到的是旧数据
            if (auth) {
                long currentTime = System.currentTimeMillis() / 1000;
                if ((currentTime - timestamp.longValue()) > this.LOSE_TOUCH_TIME) {
                    // 如果不在线已经超过五分钟, 禁止访问其30秒钟
                    this.friendBannedTime.put(peer, BigInteger.valueOf(currentTime + this.BAN_TIME));
                }
            }
        }*/
    }

    @Override
    public void onDHTItemGot(byte[] item, Object cbData, boolean auth) {
        DataIdentifier dataIdentifier = (DataIdentifier) cbData;
        if (null == item) {
            logger.debug("MULTIPLEX DATA from peer[{}] is empty", dataIdentifier.getExtraInfo1().toString());
            // 最后一个仍然是空，则put自己的在线信号
            publishFriendOnlineSignal(dataIdentifier.getExtraInfo1().getData());
            return;
        }

        MutableDataWrapper mutableDataWrapper = new MutableDataWrapper(item);
        processMutableData(mutableDataWrapper, dataIdentifier.getExtraInfo1());
    }

}
