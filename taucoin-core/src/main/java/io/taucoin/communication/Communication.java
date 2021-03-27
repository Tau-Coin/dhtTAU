package io.taucoin.communication;

import com.frostwire.jlibtorrent.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
import io.taucoin.core.FriendInfo;
import io.taucoin.core.FriendInfoList;
import io.taucoin.core.FriendPair;
import io.taucoin.core.MessageList;
import io.taucoin.core.MutableDataWrapper;
import io.taucoin.core.NewMsgSignal;
import io.taucoin.db.DBException;
import io.taucoin.dht2.DHT;
import io.taucoin.dht2.DHTEngine;
import io.taucoin.listener.MsgListener;
import io.taucoin.param.ChainParam;
import io.taucoin.repository.AppRepository;
import io.taucoin.repository.NicknameBean;
import io.taucoin.types.GossipItem;
import io.taucoin.types.Message;
import io.taucoin.types.MutableDataType;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.HashUtil;

import static io.taucoin.param.ChainParam.SHORT_ADDRESS_LENGTH;

public class Communication implements DHT.GetMutableItemCallback, KeyChangedListener {
    private static final Logger logger = LoggerFactory.getLogger("Communication");

    // 朋友延迟访问时间，根据dht short time设定
    private final int DELAY_TIME = 1; // 1 s

    // 主循环间隔最小时间
    private final int DEFAULT_LOOP_INTERVAL_TIME = 50; // 50 ms

    // 主循环间隔时间
    private int loopIntervalTime = DEFAULT_LOOP_INTERVAL_TIME;

    // 设备ID
    private final byte[] deviceID;

    private final MsgListener msgListener;

    private final AppRepository repository;

    // 当前我加的朋友集合（完整公钥）
    private final Set<ByteArrayWrapper> friends = new CopyOnWriteArraySet<>();

    // 朋友被延迟访问的时间
    private final Map<ByteArrayWrapper, BigInteger> friendDelayTime = new ConcurrentHashMap<>();

    // TODO:: 1. 对方上次给我发信息的时间； 2. 对方在新时间
    // TODO:: 对方在线可能是个隐私问题，需要从YY中获得
    // 我的朋友的最新消息的时间戳 <friend, timestamp>（完整公钥）
    private final Map<ByteArrayWrapper, BigInteger> lastCommunicatedTime = new ConcurrentHashMap<>();

    // 最新的新消息信号时间 <friend, timestamp>（完整公钥）
    private final Map<ByteArrayWrapper, BigInteger> latestNewMsgSignalTime = new ConcurrentHashMap<>();

    // 最新的的新消息信号集合（peer <--> 新消息信号）
    private final Map<ByteArrayWrapper, NewMsgSignal> latestNewMsgSignals = new ConcurrentHashMap<>();

    // 待处理的有新消息信号的peer集合
    private final Set<ByteArrayWrapper> newMsgPeers = new CopyOnWriteArraySet<>();

    // 发现的我的朋友跟我聊天的最新时间 <friend, time>（完整公钥）
    private final Map<ByteArrayWrapper, BigInteger> friendChattingTime = new ConcurrentHashMap<>();

    // 通过gossip推荐机制发现的有新消息的朋友集合（完整公钥）
    private final Set<ByteArrayWrapper> referredFriends = new CopyOnWriteArraySet<>();

    // 等待发布数据的peer
    private final Set<ByteArrayWrapper> publishFriends = new CopyOnWriteArraySet<>();

    // 通过gossip机制打听到的跟朋友聊天的time <FriendPair, Timestamp>（完整公钥）
    private final Map<FriendPair, BigInteger> gossipChattingTime = new ConcurrentHashMap<>();

    // 每10次get，进行一次put
    private final int GET_THRESHOLD = 10;

    // get计数器
    private final Map<ByteArrayWrapper, Integer> counter = new ConcurrentHashMap<>();

    // Communication thread.
    private Thread communicationThread;

    public Communication(byte[] deviceID, MsgListener msgListener, AppRepository repository) {
        this.deviceID = deviceID;
        this.msgListener = msgListener;
        this.repository = repository;
    }

    /**
     * 检查朋友列表是否变动，变动则进行增删调整
     */
    private void checkFriends() {
        Set<byte[]> friends = this.repository.getAllFriends();

        if (null != friends) {
            // 移除已经删除的朋友
            for (ByteArrayWrapper localFriend: this.friends) {
                boolean found = false;
                for (byte[] friend: friends) {
                    if (Arrays.equals(localFriend.getData(), friend)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    removeFriend(localFriend.getData());
                }
            }

            // 添加新朋友
            for (byte[] friend: friends) {
                addNewFriend(friend);
            }
        }
    }

    /**
     * 保存朋友的最新消息哈希列表
     * @param friend 通信的朋友
     * @throws DBException database exception
     */
//    private void saveFriendLatestMessageHashList(ByteArrayWrapper friend) throws DBException {
//        LinkedList<Message> linkedList = this.messageListMap.get(friend);
//
//        if (null != linkedList && !linkedList.isEmpty()) {
//            List<byte[]> list = new ArrayList<>();
//            for (Message message : linkedList) {
//                try {
//                    list.add(message.getHash());
//                } catch (RuntimeException e) {
//                    logger.error(e.getMessage(), e);
//                }
//            }
//
//            HashList hashList = new HashList(list);
//            byte[] pubKey = AccountManager.getInstance().getKeyPair().first;
//            FriendPair friendPair = new FriendPair(pubKey, friend.getData());
//            this.messageDB.saveLatestMessageHashListEncode(friendPair, hashList.getEncoded());
//        }
//    }

    /**
     * 尝试往聊天消息集合里面插入新消息
     * @param friend 通信的朋友
     * @param message 新消息
     * @return true if message list changed, false otherwise
     */
//    private boolean tryToUpdateLatestMessageList(ByteArrayWrapper friend, Message message) throws DBException {
//        LinkedList<Message> linkedList = this.messageListMap.get(friend);
//
//        // 更新成功标志
//        boolean updated = false;
//
//        if (null != linkedList) {
//            if (!linkedList.isEmpty()) {
//                try {
//                    // 先判断一下是否比最后一个消息时间戳大，如果是，则直接插入末尾
//                    if (message.getTimestamp().compareTo(linkedList.getLast().getTimestamp()) > 0) {
//                        linkedList.add(message);
//                        updated = true;
//                    } else {
//                        // 寻找从后往前寻找第一个时间小于当前消息时间的消息，将当前消息插入到到该消息后面
//                        Iterator<Message> it = linkedList.descendingIterator();
//                        // 是否插入第一个位置，在没找到的情况下会插入到第一个位置
//                        boolean insertFirst = true;
//                        while (it.hasNext()) {
//                            Message reference = it.next();
//                            int diff = reference.getTimestamp().compareTo(message.getTimestamp());
//                            // 如果差值小于零，说明找到了比当前消息时间戳小的消息位置，将消息插入到目标位置后面一位
//                            if (diff < 0) {
//                                updated = true;
//                                insertFirst = false;
//                                int i = linkedList.indexOf(reference);
//                                linkedList.add(i + 1, message);
//                                break;
//                            } else if (diff == 0) {
//                                // 如果时间戳一样，寻找第一个哈希比我小的消息
//                                byte[] referenceHash = reference.getHash();
//                                byte[] msgHash = message.getHash();
//                                if (!Arrays.equals(referenceHash, msgHash)) {
//                                    // 寻找第一个哈希比我小的消息，插入其前面，否则，继续往前找
//                                    if (FastByteComparisons.compareTo(msgHash, 0,
//                                            msgHash.length, referenceHash, 0, referenceHash.length) > 0) {
//                                        updated = true;
//                                        insertFirst = false;
//                                        int i = linkedList.indexOf(reference);
//                                        linkedList.add(i + 1, message);
//                                        break;
//                                    }
//                                } else {
//                                    // 如果哈希一样，则本身已经在列表中，也不再进行查找
//                                    insertFirst = false;
//                                    break;
//                                }
//                            }
//                        }
//
//                        if (insertFirst) {
//                            updated = true;
//                            linkedList.add(0, message);
//                        }
//                    }
//                } catch (RuntimeException e) {
//                    logger.error(e.getMessage(), e);
//                }
//            } else {
//                linkedList.add(message);
//                updated = true;
//            }
//        } else {
//            linkedList = new LinkedList<>();
//            linkedList.add(message);
//            updated = true;
//        }
//
//        // 更新成功
//        if (updated) {
//            // 如果更新了消息列表，则判断是否列表长度过长，过长则删掉旧数据，然后停止循环
//            if (linkedList.size() > ChainParam.BLOOM_FILTER_MESSAGE_SIZE) {
//                linkedList.removeFirst();
//            }
//
//            this.messageListMap.put(friend, linkedList);
//
////            saveFriendLatestMessageHashList(friend);
//        }
//
//        return updated;
//    }

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
//    private void notifyUIOnlineFriend() {
//        for (Map.Entry<ByteArrayWrapper, BigInteger> entry: this.onlineFriendsToNotify.entrySet()) {
//            logger.trace("Notify UI online friend:{}", entry.getKey().toString());
//            this.msgListener.onDiscoveryFriend(entry.getKey().getData(), entry.getValue());
//
//            this.onlineFriendsToNotify.remove(entry.getKey());
//        }
//    }

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
//    private void tryToMergeFriends() throws DBException {
//        for (ByteArrayWrapper friend: this.friendsFromRemote) {
//            addNewFriend(friend.getData());
//
//            if (!this.friends.contains(friend)) {
//                this.msgListener.onNewFriendFromMultiDevice(friend.getData());
//            }
//
//            this.friendsFromRemote.remove(friend);
//        }
//    }

    /**
     * 处理收到的消息，包括存储数据库，更新消息列表
     * @throws DBException database exception
     */
//    private void processReceivedMessages() throws DBException {
//
//        // 将消息存入数据库并通知UI，尝试获取下一条消息
//        for (Map.Entry<ByteArrayWrapper, Message> entry : this.messageMap.entrySet()) {
//            Message message = entry.getValue();
//            ByteArrayWrapper msgHash = entry.getKey();
//
//            try {
//                // 更新最新消息列表
//                ByteArrayWrapper peer = new ByteArrayWrapper(message.getSender());
//                if (tryToUpdateLatestMessageList(peer, message)) {
//                    this.publishFriends.add(peer);
//                }
//            } catch (RuntimeException e) {
//                logger.error(e.getMessage(), e);
//            }
//
//            this.messageMap.remove(msgHash);
//        }
//    }

    /**
     * 参考：https://github.com/Tau-Coin/dhtTAU/issues/35
     * 处理收到的在线信号
     * 在线信号调整策略：XY类型信号本质其实不是在线信号，而是“消息变动”通知信号；XX才是X的在线信号。
     * 主循环在get YX 后，如果get YX 为空就默认用本地内存的两边过滤器，如果XY和YX过滤器相等，
     * 这个是大多数情况，节点不应该put XY信号，这样可以节约流量。put XY信号只有在过滤器不同情况下才发生。
     * 所以真的消耗流量的只有GET。对于我们系统其实不用管对方是否在线，只要有对方最后一次“消息变动”的时间戳就行了。
     * 所以可以把现在onlineSignal换成newMsgSignal。对于同一个YX的下次访问要在short time out后才访问，
     * 也就是1秒后，所以主循环对get YX 触发前要检查是否过了1秒，1秒内多次get没有意义，应为上次get都还没有结束。
     * 为了避免通信不同步，X发出XY过滤器后，期待Y要发出对于XY的过滤器哈希回执和YX的过滤器。
     */
    private void processNewMsgSignals() {
        for (ByteArrayWrapper peer: this.newMsgPeers) {
            try {
                NewMsgSignal newMsgSignal = this.latestNewMsgSignals.get(peer);

                if (null != newMsgSignal) {
                    Bloom localMsgBloomFilter = new Bloom();
                    Bloom messageBloomFilter = newMsgSignal.getMessageBloomFilter();
                    Bloom friendListBloomFilter = newMsgSignal.getFriendListBloomFilter();

                    byte[] pubKey = AccountManager.getInstance().getKeyPair().first;
                    if (Arrays.equals(pubKey, peer.getData()) && null != friendListBloomFilter) {
                        // 是另外一台设备
                        for (ByteArrayWrapper friend : this.friends) {
                            Bloom bloom = Bloom.create(HashUtil.sha1hash(friend.getData()));
                            if (!friendListBloomFilter.matches(bloom)) {
                                // 发现不在对方朋友列表
                                this.publishFriends.add(peer);
                                break;
                            }
                        }
                    }

                    logger.debug("peer:{},{}", peer.toString(), newMsgSignal.toString());
                    // 比较双方我发的消息的bloom filter，如果不同，则发出一个对方没有的数据
                    List<Message> list = this.repository.getLatestMessageList(peer.getData(), ChainParam.BLOOM_FILTER_MESSAGE_SIZE);

                    // 查看对方是否缺消息，并合成本地的消息过滤器
                    if (null != list && !list.isEmpty()) {
                        int size = list.size();
                        byte[] firstMsgHash = list.get(0).getSha1Hash();
                        byte[] lastMsgHash = list.get(size - 1).getSha1Hash();

                        Bloom bloom = Bloom.create(firstMsgHash);
                        localMsgBloomFilter.or(bloom);

                        for (int i = 0; i < size - 1; i++) {
                            byte[] mergedHash = ByteUtil.merge(list.get(i).getSha1Hash(), list.get(i + 1).getSha1Hash());
                            bloom = Bloom.create(HashUtil.sha1hash(mergedHash));
                            localMsgBloomFilter.or(bloom);
                        }

                        bloom = Bloom.create(lastMsgHash);
                        localMsgBloomFilter.or(bloom);
                    }

                    // 两者的bloom filter不一样，put我的让对方看到
                    if (!localMsgBloomFilter.equals(messageBloomFilter)) {
                        logger.debug("Bloom filter mismatch, add peer[{}] to publish list", peer.toString());
                        this.publishFriends.add(peer);
                    }

                    // 如果你没看到我最新的bloom filter（即bloom收据哈希对不上），则发布我的在线信号
                    if (!this.publishFriends.contains(peer)) {
                        byte[] bloomReceiptHash = newMsgSignal.getBloomReceiptHash();
                        if (null != bloomReceiptHash) {
                            if (!Arrays.equals(HashUtil.sha1hash(localMsgBloomFilter.getData()), bloomReceiptHash)) {
                                logger.info("Bloom receipt hash[{}] from peer:{} is not same.", Hex.toHexString(bloomReceiptHash), peer.toString());
                                this.publishFriends.add(peer);
                            }
                        } else {
                            if (!Arrays.equals(localMsgBloomFilter.getData(), new Bloom().getData())) {
                                logger.info("Bloom receipt hash from peer:{} is not same..", peer.toString());
                                this.publishFriends.add(peer);
                            }
                        }
                    }

                    byte[] chattingFriend = newMsgSignal.getChattingFriend();
                    if (Arrays.equals(pubKey, chattingFriend)) {
                        // 如果是正在跟我聊天，判断一下上次标记聊天时间戳是否最新
                        ByteArrayWrapper sender = new ByteArrayWrapper(chattingFriend);
                        BigInteger latestTimestamp = this.friendChattingTime.get(sender);
                        // 如果发现更新的推荐，则加入推荐列表
                        if (null == latestTimestamp || latestTimestamp.compareTo(newMsgSignal.getChattingTime()) < 0) {
                            // 记录最新的聊天时间
                            this.friendChattingTime.put(sender, newMsgSignal.getChattingTime());
                            referToFriend(sender);
                        }
                    } else {
                        // 记录下推荐给别人的聊天时间
                        FriendPair friendPair = new FriendPair(peer.getData(), chattingFriend);
                        BigInteger latestTimestamp = this.gossipChattingTime.get(friendPair);
                        if (null == latestTimestamp || latestTimestamp.compareTo(newMsgSignal.getChattingTime()) < 0) {
                            this.gossipChattingTime.put(friendPair, newMsgSignal.getChattingTime());
                        }
                    }


                    if (null != newMsgSignal.getGossipItemList()) {

                        // 信任发送方自己给的gossip信息
                        for (GossipItem gossipItem : newMsgSignal.getGossipItemList()) {
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
                }
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }

            this.newMsgPeers.remove(peer);
        }
    }

    /**
     * 发布需要发布数据的peer的数据
     */
    private void publishFriendMutableData() {
        for (ByteArrayWrapper peer: this.publishFriends) {
            try {
                publishFriendMutableData(peer);
            } catch (RuntimeException e) {
                logger.error(peer.toString() + ":" + e.getMessage(), e);
            }

            this.publishFriends.remove(peer);
        }
    }

    /**
     * 向朋友发布相关mutable数据
     * @param peer 发布数据的对象
     */
    private void publishFriendMutableData(ByteArrayWrapper peer) {
        List<ByteArrayWrapper> dataSet = new ArrayList<>();
        // 关于此处使用的发现对方缺少的消息集合，使用linked hash set，而不是set或者list的原因:
        // 1. 不使用list的原因，是因为每个消息是前后两个相结合生成的bloom filter，因此，每个消息会被使用两次，
        // 因而也就有可能被添加进集合两次，list不具有去重作用
        // 2. 不使用set的原因，是识别对方缺少消息机制存在假阴性问题（也就是不缺该消息，却会被判定为缺少该消息），
        // 而set不像list那样是有序的，如果这样把set集合里面的消息put给对方，对方可能会拿到一些互相不相邻的消息，
        // 而这些不相邻的消息，由于假阴性问题的存在，仍然会重新会被判定为不存在而重新put，
        // 这样就会导致一直put这多个对方已有的数据，从而陷入死局
        // 3. 使用linked hash set这种既有顺序，又有唯一性保证的集合，能实现按顺序put，只要按顺序put，
        // 消息之间是相邻的，那么最多只有右边界的消息是假阴性的，这样dht put的8个一个item中最多只有一个是假阴性的，
        // 其它7个（若有7个）肯定是对方缺少的消息，这样不会陷入死局，对方会逐渐拿到缺少的数据
        LinkedHashSet<Message> linkedMessageSet = new LinkedHashSet<>();

        NewMsgSignal myNewMsgSignal = makeNewMsgSignal(peer);
        MutableDataWrapper mutableDataWrapper = new MutableDataWrapper(MutableDataType.NEW_MSG_SIGNAL,
                myNewMsgSignal.getEncoded());
        dataSet.add(new ByteArrayWrapper(mutableDataWrapper.getEncoded()));

        NewMsgSignal newMsgSignal = this.latestNewMsgSignals.get(peer);

        if (null != newMsgSignal) {
            Bloom messageBloomFilter = newMsgSignal.getMessageBloomFilter();
            Bloom friendListBloomFilter = newMsgSignal.getFriendListBloomFilter();

            byte[] pubKey = AccountManager.getInstance().getKeyPair().first;
            if (Arrays.equals(pubKey, peer.getData()) && null != friendListBloomFilter) {
                // 是另外一台设备
                List<FriendInfo> friendInfos = new ArrayList<>();
                for (ByteArrayWrapper friend : this.friends) {
                    Bloom bloom = Bloom.create(HashUtil.sha1hash(friend.getData()));
                    if (!friendListBloomFilter.matches(bloom)) {
                        // 发现不在对方朋友列表
                        NicknameBean nicknameBean = this.repository.getFriendNickName(friend.getData());
                        if (null != nicknameBean) {
                            friendInfos.add(new FriendInfo(friend.getData(), nicknameBean.getNickname(), nicknameBean.getTimestamp()));

                            if (friendInfos.size() >= ChainParam.MAX_FRIEND_LIST_SIZE) {
                                break;
                            }
                        }
                    }
                }

                if (!friendInfos.isEmpty()) {
                    FriendInfoList friendInfoList = new FriendInfoList(this.deviceID, friendInfos);
                    if (dataSet.size() < ChainParam.MAX_DHT_PUT_ITEM_SIZE) {
                        mutableDataWrapper = new MutableDataWrapper(MutableDataType.FRIEND_LIST,
                                friendInfoList.getEncoded());
                        dataSet.add(new ByteArrayWrapper(mutableDataWrapper.getEncoded()));
                    }
                }
            }

            // 比较双方我发的消息的bloom filter，如果不同，则发出一个对方没有的数据
            List<Message> messageList = this.repository.getLatestMessageList(peer.getData(), ChainParam.BLOOM_FILTER_MESSAGE_SIZE);

            if (null != messageList && !messageList.isEmpty()) {
                int size = messageList.size();
                byte[] firstMsgHash = messageList.get(0).getSha1Hash();
                byte[] lastMsgHash = messageList.get(size - 1).getSha1Hash();

                Bloom bloom = Bloom.create(firstMsgHash);
                if (!messageBloomFilter.matches(bloom)) {
                    linkedMessageSet.add(messageList.get(0));
                }

                for (int i = 0; i < size - 1; i++) {
                    byte[] mergedHash = ByteUtil.merge(messageList.get(i).getSha1Hash(), messageList.get(i + 1).getSha1Hash());
                    bloom = Bloom.create(HashUtil.sha1hash(mergedHash));
                    boolean match = messageBloomFilter.matches(bloom);
                    // 如果合并哈希不匹配，则随机发出一个缺少的消息即可
                    if (!match) {
                        linkedMessageSet.add(messageList.get(i));
                        linkedMessageSet.add(messageList.get(i + 1));
                    }
                }

                bloom = Bloom.create(lastMsgHash);
                boolean match = messageBloomFilter.matches(bloom);
                if (!match) {
                    linkedMessageSet.add(messageList.get(0));
                }
            }
        }

        BigInteger timestamp = BigInteger.valueOf(System.currentTimeMillis() / 1000);
        while (dataSet.size() < ChainParam.MAX_DHT_PUT_ITEM_SIZE && !linkedMessageSet.isEmpty()) {
            List<Message> messages = new ArrayList<>();
            MessageList messageList = new MessageList(messages);

            Iterator<Message> iterator = linkedMessageSet.iterator();
            // 构造一个尺寸安全的消息列表
            while (iterator.hasNext()) {
                Message message= iterator.next();

                if (validateMessage(message)) {
                    if (messageList.getEncoded().length + message.getEncoded().length <= ChainParam.MESSAGE_LIST_SAFE_SIZE) {
                        // 如果还能装载，继续填装
                        logger.error("Put message:{}", message.toString());
                        this.msgListener.onSyncMessage(message, timestamp);
                        messages.add(message);
                        messageList = new MessageList(messages);

                        // 用过的消息删掉
                        iterator.remove();
                    } else {
                        break;
                    }
                } else {
                    iterator.remove();
                }
            }

            // 如果消息列表里面有消息
            if (!messages.isEmpty()) {
                mutableDataWrapper = new MutableDataWrapper(MutableDataType.MESSAGE_LIST,
                        messageList.getEncoded());
                dataSet.add(new ByteArrayWrapper(mutableDataWrapper.getEncoded()));
            }
        }

        publishMutableData(peer.getData(), dataSet);
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
     * 更新访问计数器，在达到一定的访问次数时，进行一次put
     * @param peer 更新的朋友
     */
    private void updateCounter(ByteArrayWrapper peer) {
        Integer counter = this.counter.get(peer);
        if (null != counter) {
            counter++;

            if (counter >= this.GET_THRESHOLD) {
                // 达到10次后，进行一次put，并将计数器归零
                this.counter.put(peer, 0);

                this.publishFriends.add(peer);
            } else {
                // 否则，更新计数器
                this.counter.put(peer, counter);
            }
        } else {
            // 没有访问记录，则记下该次
            this.counter.put(peer, 1);
        }
    }

    /**
     * 挑选一个推荐的朋友访问，没有则随机挑一个访问
     */
    private void visitReferredFriends() {
        ByteArrayWrapper peer = null;

        byte[] chattingFriend = this.repository.getChattingFriend();
        if (null != chattingFriend) {
            peer = new ByteArrayWrapper(chattingFriend);
        } else {
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
                    int index = random.nextInt(this.friends.size());

//                    if (index != this.friends.size()) {
                        // (0 ~ size)
                        int i = 0;
                        while (iterator.hasNext()) {
                            peer = iterator.next();
                            if (i == index) {
                                break;
                            }

                            i++;
                        }
//                    } else {
//                        peer = new ByteArrayWrapper(AccountManager.getInstance().getKeyPair().first);
//                    }
                }
            }
        }

        if (null != peer) {
            updateCounter(peer);

            // 如果选中的朋友没在禁止列表的禁止期，则访问它
            long currentTime = System.currentTimeMillis() / 1000;
            BigInteger timestamp = this.friendDelayTime.get(peer);
            if (null == timestamp || currentTime - this.DELAY_TIME >= timestamp.longValue() ) {
                // 没在禁止列表
                requestMutableDataFromPeer(peer);
                this.friendDelayTime.put(peer, BigInteger.valueOf(currentTime + this.DELAY_TIME));
            }
        }
    }

    /**
     * 发布某朋友的在线信号
     * @param friend 该朋友
     */
//    private void publishFriendOnlineSignal(byte[] friend) {
//        NewMsgSignal newMsgSignal = makeNewMsgSignal(friend);
//
//        publishOnlineSignal(friend, newMsgSignal);
//    }

    /**
     * 构造某个朋友的新消息信号
     * @param peer 构造新消息信号的朋友
     * @return 在线信号
     */
    private NewMsgSignal makeNewMsgSignal(ByteArrayWrapper peer) {
        Bloom messageBloomFilter = new Bloom();
        byte[] bloomReceiptHash = null;
        Bloom friendListBloomFilter = null;
        byte[] chattingFriend = this.repository.getChattingFriend();
        BigInteger chattingTime = BigInteger.valueOf(System.currentTimeMillis() / 1000);
        List<GossipItem> gossipItemList = new ArrayList<>();

        List<Message> messageList = this.repository.getLatestMessageList(peer.getData(), ChainParam.BLOOM_FILTER_MESSAGE_SIZE);
        if (null != messageList && !messageList.isEmpty()) {
            int size = messageList.size();
            byte[] firstMsgHash = messageList.get(0).getSha1Hash();
            byte[] lastMsgHash = messageList.get(size - 1).getSha1Hash();
            Bloom bloom = Bloom.create(firstMsgHash);
            messageBloomFilter.or(bloom);
            bloom = Bloom.create(lastMsgHash);
            messageBloomFilter.or(bloom);

            for (int i = 0; i < size - 1; i++) {
                byte[] mergedHash = ByteUtil.merge(messageList.get(i).getSha1Hash(), messageList.get(i + 1).getSha1Hash());
                bloom = Bloom.create(HashUtil.sha1hash(mergedHash));
                messageBloomFilter.or(bloom);
            }
        }

        NewMsgSignal latestNewMsgSignal = this.latestNewMsgSignals.get(peer);
        if (null != latestNewMsgSignal) {
            bloomReceiptHash = latestNewMsgSignal.getMessageBloomFilterHash();
        }

        byte[] pubKey = AccountManager.getInstance().getKeyPair().first;
        if (Arrays.equals(pubKey, peer.getData())) {
            friendListBloomFilter = new Bloom();
            for (ByteArrayWrapper friend : this.friends) {
                Bloom bloom = Bloom.create(HashUtil.sha1hash(friend.getData()));
                friendListBloomFilter.or(bloom);
            }
        }

        // TODO:: 测量极限容量
        Iterator<Map.Entry<FriendPair, BigInteger>> it = this.gossipChattingTime.entrySet().iterator();
        int i = 1;
        while (it.hasNext()) {
            Map.Entry<FriendPair, BigInteger> entry = it.next();
            // 查找接收者是peer的
            if (Arrays.equals(entry.getKey().receiver, peer.getData())) {
                gossipItemList.add(new GossipItem(entry.getKey().sender, entry.getValue()));

                i--;
                it.remove();
            }

            if (i <= 0) {
                break;
            }
        }

        return new NewMsgSignal(messageBloomFilter, bloomReceiptHash, friendListBloomFilter,
                chattingFriend, chattingTime, gossipItemList);
    }

    /**
     * 发布在线信号
     * @param friend 发送对象
     * @param newMsgSignal 发送的在线信号
     */
//    private void publishOnlineSignal(byte[] friend, NewMsgSignal newMsgSignal) {
//        if (null != newMsgSignal) {
//            logger.debug("publish friend[{}] online signal:{}", Hex.toHexString(friend), newMsgSignal.toString());
//            MutableDataWrapper mutableDataWrapper = new MutableDataWrapper(MutableDataType.NEW_MSG_SIGNAL,
//                    newMsgSignal.getEncoded());
//            publishMutableData(friend, mutableDataWrapper.getEncoded());
//        }
//    }

    /**
     * 发布朋友列表
     * @param friendList friend list to publish
     */
//    private void publishFriendList(byte[] friend, FriendList friendList) {
//        if (null != friendList) {
//            MutableDataWrapper mutableDataWrapper = new MutableDataWrapper(MutableDataType.FRIEND_LIST,
//                    friendList.getEncoded());
//            publishMutableData(friend, mutableDataWrapper.getEncoded());
//        }
//    }

    /**
     * 发布消息
     * @param message msg to publish
     */
//    private void publishMessage(byte[] friend, Message message) {
//        if (null != message) {
//            MutableDataWrapper mutableDataWrapper = new MutableDataWrapper(MutableDataType.MESSAGE,
//                    message.getEncoded());
//            publishMutableData(friend, mutableDataWrapper.getEncoded());
//        }
//    }

    /**
     * 发布mutable数据
     * @param peer 发布数据的对象
     * @param data 发布的数据
     */
//    private void publishMutableData(byte[] peer, byte[] data) {
//        // put mutable item
//        Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();
//
//        byte[] salt = makeSendingSalt(keyPair.first, peer);
//        if (null != data) {
//            DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first,
//                    keyPair.second, data, salt);
//            DHTEngine.getInstance().distribute(mutableItem, null, null);
//        }
//    }

    /**
     *发布mutable数据列表
     * @param peer 发布数据的对象
     * @param list 发布的数据列表
     */
    private void publishMutableData(byte[] peer, List<ByteArrayWrapper> list) {
        logger.debug("Put mutable data to peer:{}", Hex.toHexString(peer));
        Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();

        if (null != list && !list.isEmpty()) {
            byte[] salt = makeSendingSalt(keyPair.first, peer);
            DHT.MutableItemBatch mutableItemBatch = new DHT.MutableItemBatch(keyPair.first,
                    keyPair.second, list, salt);
            DHTEngine.getInstance().distribute(mutableItemBatch, null, null);
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
                checkFriends();

                // 合并来自多设备的朋友
//                tryToMergeFriends();

                // 通知UI发现的在线朋友
//                notifyUIOnlineFriend();

                // 处理获得的消息
//                processReceivedMessages();

                // 处理新消息信号
                processNewMsgSignals();

                // 访问通过gossip机制推荐的活跃peer
                visitReferredFriends();

                // 发布需要发布数据的peer的数据
                publishFriendMutableData();

                try {
                    this.loopIntervalTime = this.repository.getMainLoopInterval();
                    Thread.sleep(this.loopIntervalTime);
                } catch (InterruptedException e) {
                    logger.info(e.getMessage(), e);
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
     * save message data in database
     * @param message msg
     * @throws DBException database exception
     */
//    private void saveMessageDataInDB(Message message) throws DBException {
//        if (null != message) {
//            this.messageDB.putMessage(message.getHash(), message.getEncoded());
//        }
//    }

    /**
     * 验证消息，目前只验证编码长度
     * @param message 消息
     * @return true/false
     */
    private boolean validateMessage(Message message) {
        if (null != message) {
            if (message.getEncoded().length > ChainParam.MESSAGE_LIST_SAFE_SIZE) {
                logger.error("Oversize message:{}", Hex.toHexString(message.getHash()));
                return false;
            }

            return true;
        }

        return false;
    }

    /**
     * 向朋友发布新消息
     * @param friend 朋友公钥
     * @param message 新消息
     * @return true:接受该消息， false:拒绝该消息
     */
//    public boolean publishNewMessage(byte[] friend, Message message) {
//        try {
//            if (null != message) {
//                if (!validateMessage(message)) {
//                    return false;
//                }
//
//                logger.debug("Publish message:{}", message.toString());
//
//                ByteArrayWrapper peer = new ByteArrayWrapper(friend);
//
//                chattingWithFriend(peer);
////                saveMessageDataInDB(message);
////                tryToUpdateLatestMessageList(peer, message);
//                publishFriendMutableData(peer);
//            }
//        } catch (Exception e) {
//            logger.error(e.getMessage(), e);
//        }
//
//        return true;
//    }

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
//    private ArrayList<byte[]> getLatestMessageHashList(byte[] friend) {
//        ArrayList<byte[]> list = new ArrayList<>();
//
//        LinkedList<Message> linkedList = this.messageListMap.get(new ByteArrayWrapper(friend));
//        if (null != linkedList && !linkedList.isEmpty()) {
//
//            for (Message message: linkedList) {
//                list.add(message.getHash());
//            }
//        }
//
//        return list;
//    }

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
     * @param friend public key
     */
    public void addNewFriend(byte[] friend) {
        ByteArrayWrapper key = new ByteArrayWrapper(friend);
        logger.debug("Add friend:{}", key.toString());
        this.friends.add(key);
        // 没有才添加
//        if (!this.friends.contains(key)) {
//            byte[] myPubKey = AccountManager.getInstance().getKeyPair().first;

            // 朋友列表排除自己
//            if (!Arrays.equals(myPubKey, friend)) {
//                logger.debug("Add friend:{}", key.toString());
//                this.friends.add(key);

//                List<Message> messageList = this.repository.getLatestMessageList(friend, ChainParam.BLOOM_FILTER_MESSAGE_SIZE);
//
//                LinkedList<Message> linkedList = new LinkedList<>();
//                if (null != messageList) {
//                    linkedList.addAll(messageList);
//                }
//
//                this.messageListMap.put(key, linkedList);
//            }
//        }
    }

    /**
     * 删除朋友
     * @param friend public key
     */
    public void removeFriend(byte[] friend) {
        ByteArrayWrapper key = new ByteArrayWrapper(friend);

        clearPeerCache(key);
    }

    /**
     * Start thread
     *
     * @return boolean successful or not.
     */
    public boolean start() {
        AccountManager.getInstance().addListener(this);

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

    /**
     * 删掉该朋友相关的缓存
     * @param peer 要删掉的朋友
     */
    private void clearPeerCache(ByteArrayWrapper peer) {
        this.friends.remove(peer);
        this.friendDelayTime.remove(peer);
        this.lastCommunicatedTime.remove(peer);
        this.latestNewMsgSignalTime.remove(peer);
        this.latestNewMsgSignals.remove(peer);
        this.newMsgPeers.remove(peer);
        this.friendChattingTime.remove(peer);
        this.referredFriends.remove(peer);
        this.publishFriends.remove(peer);

        for (Map.Entry<FriendPair, BigInteger> entry: this.gossipChattingTime.entrySet()) {
            FriendPair friendPair = entry.getKey();
            if (Arrays.equals(peer.getData(), friendPair.getReceiver())) {
                this.gossipChattingTime.remove(entry.getKey());
            }
        }
    }

    /**
     * 清空所有缓存数据
     */
    private void clearAllCache() {
        this.friends.clear();
        this.friendDelayTime.clear();
        this.lastCommunicatedTime.clear();
        this.latestNewMsgSignalTime.clear();
        this.latestNewMsgSignals.clear();
        this.newMsgPeers.clear();
        this.friendChattingTime.clear();
        this.referredFriends.clear();
        this.publishFriends.clear();
        this.gossipChattingTime.clear();
    }

    @Override
    public void onKeyChanged(Pair<byte[], byte[]> newKey) {
        clearAllCache();
    }

    /**
     * 处理收到的mutable data
     * @param mutableDataWrapper 收到的mutable data
     * @param peer gossip发出的peer
     */
    private void processMutableData(MutableDataWrapper mutableDataWrapper, ByteArrayWrapper peer) {

        // 是否更新好友的在线时间（完整公钥）
        BigInteger timestamp = mutableDataWrapper.getTimestamp();
        BigInteger lastCommunicated = this.lastCommunicatedTime.get(peer);

        if (null != lastCommunicated && lastCommunicated.compareTo(timestamp) > 0) {
            logger.debug("-----old mutable data from peer:{}", peer.toString());
        }

        // 判断时间戳，以避免处理历史数据
        if (null == lastCommunicated || lastCommunicated.compareTo(timestamp) < 0) { // 判断是否是更新的online signal
            logger.debug("Newer data from peer:{}", peer.toString());
            this.lastCommunicatedTime.put(peer, timestamp);
            this.msgListener.onDiscoveryFriend(peer.getData(), timestamp);
//            this.onlineFriendsToNotify.put(peer, timestamp);
        }
        switch (mutableDataWrapper.getMutableDataType()) {
            case MESSAGE_LIST: {
                MessageList messageList = new MessageList(mutableDataWrapper.getData());
                List<Message> messages = messageList.getMessageList();
                if (null != messages) {
                    for (Message message: messages) {
                        logger.debug("MESSAGE: Got message :{}", message.toString());

                        this.msgListener.onNewMessage(peer.getData(), message);
                    }
                }

                break;
            }
            case FRIEND_LIST: {
                byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

                if (Arrays.equals(pubKey, peer.getData())) {
                    FriendInfoList friendInfoList = new FriendInfoList(mutableDataWrapper.getData());

                    // 如果来自不同的设备
                    byte[] deviceID = friendInfoList.getDeviceID();
                    if (!Arrays.equals(this.deviceID, deviceID)) {
                        this.msgListener.onNewDeviceID(deviceID);

                        List<FriendInfo> list = friendInfoList.getFriendInfoList();
                        if (null != list) {
                            for (FriendInfo friendInfo : list) {
                                if (!this.friends.contains(new ByteArrayWrapper(friendInfo.getPubKey()))) {
                                    this.msgListener.onNewFriendFromMultiDevice(friendInfo.getPubKey(),
                                            friendInfo.getNickname(), friendInfo.getTimestamp());
                                }
                            }
                        }
                    }
                }

                break;
            }
            case NEW_MSG_SIGNAL: {
                NewMsgSignal newMsgSignal = new NewMsgSignal(mutableDataWrapper.getData());

                BigInteger latestNewMsgSignalTime = this.latestNewMsgSignalTime.get(peer);
                // 判断时间戳，以避免处理历史数据
                if (null == latestNewMsgSignalTime || latestNewMsgSignalTime.compareTo(timestamp) < 0) { // 判断是否是更新的online signal
                    logger.debug("Newer online signal:{} from peer:{}", newMsgSignal.toString(), peer.toString());
                    this.latestNewMsgSignalTime.put(peer, timestamp);
                    this.latestNewMsgSignals.put(peer, newMsgSignal);
                    this.newMsgPeers.add(peer);

                    Bloom messageBloomFilter = newMsgSignal.getMessageBloomFilter();

                    byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

                    // 比较双方我发的消息的bloom filter，如果不同，则发出一个对方没有的数据
                    List<Message> list = this.repository.getLatestMessageList(peer.getData(), ChainParam.BLOOM_FILTER_MESSAGE_SIZE);

                    if (null != list && !list.isEmpty()) {
                        int size = list.size();
                        byte[] firstMsgHash = list.get(0).getSha1Hash();
                        byte[] lastMsgHash = list.get(size - 1).getSha1Hash();
                        boolean previousMatch = true;

                        Bloom bloom = Bloom.create(firstMsgHash);
                        if (!messageBloomFilter.matches(bloom)) {
                            previousMatch = false;
                        }

                        for (int i = 0; i < size - 1; i++) {
                            byte[] mergedHash = ByteUtil.merge(list.get(i).getSha1Hash(), list.get(i + 1).getSha1Hash());
                            bloom = Bloom.create(HashUtil.sha1hash(mergedHash));
                            boolean match = messageBloomFilter.matches(bloom);
                            // 前后两个合并哈希都匹配，才确认收到
                            if (previousMatch && match) {
                                Message message = list.get(i);
                                if (Arrays.equals(pubKey, message.getSender())) {
//                                    logger.debug("Notify UI confirmation root:{}", Hex.toHexString(message.getHash()));
                                    // 若匹配，则大概率对方收到了该消息，记为confirmation root，通知UI
                                    this.msgListener.onReadMessageRoot(peer.getData(), message.getHash(), timestamp);
                                }
                            }

                            previousMatch = match;
                        }

                        bloom = Bloom.create(lastMsgHash);
                        boolean match = messageBloomFilter.matches(bloom);
                        // 前后两个合并哈希都匹配，才确认收到
                        if (previousMatch && match) {
                            Message message = list.get(size - 1);
                            if (Arrays.equals(pubKey, message.getSender())) {
//                                logger.debug("Notify UI confirmation root:{}", Hex.toHexString(message.getHash()));
                                // 若匹配，则大概率对方收到了该消息，记为confirmation root，通知UI
                                this.msgListener.onReadMessageRoot(peer.getData(), message.getHash(), timestamp);
                            }
                        }
                    }

                }

                break;
            }
            case UNKNOWN: {
                logger.error("Unknown type.");
            }
        }
    }

    @Override
    public void onDHTItemGot(byte[] item, Object cbData, boolean auth) {
        DataIdentifier dataIdentifier = (DataIdentifier) cbData;
        if (null == item) {
            logger.debug("MULTIPLEX DATA from peer[{}] is empty", dataIdentifier.getExtraInfo1().toString());
            // 最后一个仍然是空，则put自己的新消息信号
//            publishFriendMutableData(dataIdentifier.getExtraInfo1());
            return;
        }

        MutableDataWrapper mutableDataWrapper = new MutableDataWrapper(item);
        processMutableData(mutableDataWrapper, dataIdentifier.getExtraInfo1());
    }

}
