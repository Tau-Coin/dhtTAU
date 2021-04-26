package io.taucoin.types;

import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteArrayWrapper;

public class NewMsgSignalTest {
    Map<ByteArrayWrapper, LinkedList<BigInteger>> linkedListMap = new ConcurrentHashMap<>();

//    @Test
//    public void testCodec() {
//        byte[] friend = Hex.decode("3e87c35d2079858d88dcb113edadaf1b339fcd4f74c539faa9a9bd59e787f124");
//
//        Bloom messageBloomFilter = new Bloom();
//        byte[] bloomReceiptHash = null;
//        Bloom friendListBloomFilter = new Bloom();
//        byte[] chattingFriend = friend;
//        BigInteger chattingTime = BigInteger.TEN;
//        List<GossipItem> gossipItemList = new ArrayList<>();
//
//        NewMsgSignal newMsgSignal = new NewMsgSignal(messageBloomFilter, null, chattingFriend, chattingTime, gossipItemList);
//        System.out.println(newMsgSignal.toString());
//        byte[] encode = newMsgSignal.getEncoded();
//        NewMsgSignal newMsgSignal1 = new NewMsgSignal(encode);
//        System.out.println(newMsgSignal1.toString());
//        if (null == newMsgSignal1.getFriendListBloomFilter()) {
//            System.out.println("Empty..........");
//        }
//    }

    @Test
    public void testBloomFilter() {
        double n = 200;
        double m = 256 * 8;
        double k = 3;
        double p  = 1 - Math.exp(-1 * (n * k / m));
        System.out.print(Math.pow(p, k));
    }

    public void insert(BigInteger num) {
        ByteArrayWrapper key = new ByteArrayWrapper(Hex.decode("12"));
        LinkedList<BigInteger> linkedList = linkedListMap.get(key);

        // 更新成功标志
        boolean updated = false;

        if (null != linkedList) {
            int size = linkedList.size();
            if (size > 0) {
                try {
                    // 先判断一下是否比最后一个消息时间戳大，如果是，则直接插入末尾
                    if (num.compareTo(linkedList.getLast()) > 0) {
                        linkedList.add(num);
                        updated = true;
                    } else {
                        // 寻找从后往前寻找第一个时间小于当前消息时间的消息，将当前消息插入到到该消息后面
                        Iterator<BigInteger> it = linkedList.descendingIterator();
                        while (it.hasNext()) {
                            BigInteger reference = it.next();
                            int diff = reference.compareTo(num);
                            // 如果差值小于零，说明找到了比当前消息时间戳小的消息位置，将消息插入到目标位置后面一位
                            if (diff < 0) {
                                updated = true;
                            }
                            if (updated) {
                                int i = linkedList.indexOf(reference);
                                linkedList.add(i + 1, num);
                                break;
                            }
                        }
                    }
                } catch (RuntimeException e) {
                }
            } else {
                linkedList.add(num);
                updated = true;
            }
        } else {
            linkedList = new LinkedList<>();
            linkedList.add(num);
            updated = true;
        }

        // 更新成功
        if (updated) {
            // 如果更新了消息列表，则判断是否列表长度过长，过长则删掉旧数据，然后停止循环
            if (linkedList.size() > ChainParam.MAX_MESSAGE_LIST_SIZE) {
                linkedList.removeFirst();
            }

            this.linkedListMap.put(key, linkedList);
        }
    }

    @Test
    public void testLinkedList() {
        LinkedList<BigInteger> linkedListA = new LinkedList<>();
        ByteArrayWrapper key = new ByteArrayWrapper(Hex.decode("12"));
        linkedListMap.put(key, linkedListA);

        LinkedList<BigInteger> linkedList1 = linkedListMap.get(key);
        insert(BigInteger.valueOf(1));
        insert(BigInteger.valueOf(7));
        insert(BigInteger.valueOf(3));
        insert(BigInteger.valueOf(6));
        insert(BigInteger.valueOf(5));
        insert(BigInteger.valueOf(2));
        System.out.println(this.linkedListMap);
    }
}
