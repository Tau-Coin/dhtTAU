package io.taucoin.core;

import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteArrayWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PeerManager {

    private static final Logger logger = LoggerFactory.getLogger("PeerMananger");

    // chain ID
    private final byte[] chainID;

    private final static int PEER_NUMBER = 8;

    // all peers
    private Set<ByteArrayWrapper> allPeers;

    // random peer list
    private List<ByteArrayWrapper> randomPeerList = new ArrayList<>(PEER_NUMBER);

    // peers in mutable range
    private List<ByteArrayWrapper> priorityPeers;

    // peer info: peer <-> time
    private final Map<ByteArrayWrapper, Long> peerInfo = new HashMap<>();

    // block peers
    private final Set<ByteArrayWrapper> blockPeers = new HashSet<>(PEER_NUMBER);

    // tx peers
    private final Set<ByteArrayWrapper> txPeers = new HashSet<>(PEER_NUMBER);

    public PeerManager(byte[] chainID) {
        this.chainID = chainID;
    }

    /**
     * init peer manager
     * @param allPeers not null
     * @param priorityPeers not null
     * @return true if succeed, false otherwise
     */
    public synchronized boolean init(Set<ByteArrayWrapper> allPeers, List<ByteArrayWrapper> priorityPeers) {
        if (null == allPeers || allPeers.isEmpty()) {
            logger.error("ChainID:{}: All peers is empty!", new String(this.chainID));
            return false;
        }

        if (null == priorityPeers || priorityPeers.isEmpty()) {
            logger.error("ChainID:{}: Priority peers is empty!", new String(this.chainID));
            return false;
        }

        this.allPeers = allPeers;
        this.priorityPeers = priorityPeers;

        // set time to zero
        for (ByteArrayWrapper peer: this.allPeers) {
            this.peerInfo.put(peer, (long)0);
        }

        int size = this.priorityPeers.size();
        Random random = new Random(System.currentTimeMillis());

        for (int i = 0; i < PEER_NUMBER; i++) {
            // add a peer to block peers randomly from priority peers
            this.blockPeers.add(priorityPeers.get(random.nextInt(size)));
            // add a peer to tx peers randomly from priority peers
            this.txPeers.add(priorityPeers.get(random.nextInt(size)));
        }

        return true;
    }

    /**
     * add a new peer to all peer set when connect a new block
     * @param peer a peer
     */
    public synchronized void addNewBlockPeer(byte[] peer) {
        this.allPeers.add(new ByteArrayWrapper(peer));
        if (this.priorityPeers.size() >= ChainParam.MUTABLE_RANGE) {
            this.priorityPeers.remove(0);
        }
        this.priorityPeers.add(new ByteArrayWrapper(peer));
    }

    /**
     * add a peer to all peer set when sync old block
     * @param peer a peer
     */
    public synchronized void addOldBlockPeer(byte[] peer) {
        this.allPeers.add(new ByteArrayWrapper(peer));
        if (this.priorityPeers.size() <= ChainParam.MUTABLE_RANGE) {
            this.priorityPeers.add(new ByteArrayWrapper(peer));
        }
    }

    /**
     * get optimal block peer, then remove it from set
     * @return peer public key
     */
    public synchronized byte[] popUpOptimalBlockPeer() {
        byte[] peer;

        while (true) {
            // if the number of peers is too small, remove the limit
            boolean limit = true;

            // if empty, fill it up
            if (this.blockPeers.isEmpty()) {
                fillBlockPeers();

                // remove the limit, when the number of peers is too little
                if (blockPeers.size() < PEER_NUMBER) {
                    limit = false;
                }
            }

            // get first peer, then remove it
            Iterator<ByteArrayWrapper> iterator = this.blockPeers.iterator();
            peer = iterator.next().getData();
            iterator.remove();

            // The loop termination condition is:
            // 1. No limit
            if (!limit) {
                break;
            }

            // 2. Cannot find peer info
            Long time = this.peerInfo.get(new ByteArrayWrapper(peer));
            if (null == time) {
                break;
            }

            // 3. The last time is long enough
            long currentTime = System.currentTimeMillis() / 1000;
            if (currentTime - time > ChainParam.DEFAULT_BLOCK_TIME) {
                break;
            }
        }

        // update latest timestamp
        long currentTime = System.currentTimeMillis() / 1000;
        this.peerInfo.put(new ByteArrayWrapper(peer), currentTime);

        return peer;
    }

    /**
     * get a block peer randomly
     * @return public key
     */
    public synchronized byte[] getBlockPeerRandomly() {
        byte[] peer;

        // if empty, fill it up
        if (this.blockPeers.isEmpty()) {
            fillBlockPeers();
        }

        // get first peer, then remove it
        Iterator<ByteArrayWrapper> iterator = this.blockPeers.iterator();
        peer = iterator.next().getData();
        iterator.remove();

        return peer;
    }

    /**
     * get peer last visiting time
     * @param pubKey peer public key
     * @return last visiting time, or 0 if first visit
     */
    public synchronized long getPeerVisitTime(byte[] pubKey) {
        Long time = this.peerInfo.get(new ByteArrayWrapper(pubKey));
        if (null != time) {
            return time;
        }

        return 0;
    }

    /**
     * update peer the revisit time
     * @param pubKey public key
     */
    public synchronized void updateVisitTime(byte[] pubKey) {
        // update latest timestamp
        long currentTime = System.currentTimeMillis() / 1000;
        this.peerInfo.put(new ByteArrayWrapper(pubKey), currentTime);
    }

    /**
     * add a peer to block peer set
     * @param peer public key
     */
    public synchronized void addBlockPeer(byte[] peer) {
        // if full, throw it away
        if (this.blockPeers.size() < PEER_NUMBER) {
            this.blockPeers.add(new ByteArrayWrapper(peer));
        }
    }

    /**
     * get optimal tx peer, then remove it from set
     * @return public key
     */
    public synchronized byte[] popUpOptimalTxPeer() {
        // if empty, fill it up
        if (this.txPeers.isEmpty()) {
            fillTxPeers();
        }

        // get first peer, then remove it
        Iterator<ByteArrayWrapper> iterator = this.txPeers.iterator();
        byte[] peer = iterator.next().getData();
        iterator.remove();

        return peer;
    }

    /**
     * get optimal tx peer
     *
     * @return public key
     */
    public synchronized byte[] getOptimalTxPeer() {
        // if empty, fill it up
        if (this.txPeers.isEmpty()) {
            fillTxPeers();
        }

        // get first peer
        Iterator<ByteArrayWrapper> iterator = this.txPeers.iterator();
        return iterator.next().getData();
    }

    /**
     * fill random peer list
     */
    private void fillRandomPeerList() {
        int[] indexes = new int[PEER_NUMBER];

        int size = this.allPeers.size();
        Random random = new Random(System.currentTimeMillis());

        for (int i = 0; i < PEER_NUMBER; i++) {
            indexes[i] = random.nextInt(size);
        }

        Arrays.sort(indexes);

        int i = 0;
        int m = 0;
        for (ByteArrayWrapper peer: this.allPeers) {
            if (i >= PEER_NUMBER) {
                break;
            }

            if (m == indexes[i]) {
                this.randomPeerList.add(peer);
                i ++;
                // 处理相邻两个数一样的情况
                continue;
            }

            m++;
        }
    }

    /**
     * fill block peers
     */
    private void fillBlockPeers() {
        int size = this.priorityPeers.size();
        Random random = new Random(System.currentTimeMillis());

        int i = 0;
        for (; i < PEER_NUMBER / 2; i++) {
            // add a peer to tx peers randomly from priority peers
            this.blockPeers.add(priorityPeers.get(random.nextInt(size)));
        }

        if (this.randomPeerList.size() < PEER_NUMBER) {
            fillRandomPeerList();
        }

        for (; i < PEER_NUMBER; i++) {
            // add a peer to tx peers randomly from all peers
            this.blockPeers.add(this.randomPeerList.get(0));
            this.randomPeerList.remove(0);
        }
    }

    /**
     * fill tx peers
     */
    private void fillTxPeers() {
        int size = this.priorityPeers.size();
        Random random = new Random(System.currentTimeMillis());

        int i = 0;
        for (; i < PEER_NUMBER / 2; i++) {
            // add a peer to tx peers randomly from priority peers
            this.txPeers.add(priorityPeers.get(random.nextInt(size)));
        }

        if (this.randomPeerList.size() < PEER_NUMBER) {
            fillRandomPeerList();
        }

        for (; i < PEER_NUMBER; i++) {
            // add a peer to tx peers randomly from all peers
            this.txPeers.add(this.randomPeerList.get(0));
            this.randomPeerList.remove(0);
        }
    }

    /**
     * add a peer to tx peer set
     * @param peer public key
     */
    public synchronized void addTxPeer(byte[] peer) {
        // if full, throw it away
        if (this.txPeers.size() < PEER_NUMBER) {
            this.txPeers.add(new ByteArrayWrapper(peer));
        }
    }

    /**
     * get all peers
     * @return peer set
     */
    public synchronized Set<ByteArrayWrapper> getAllPeers() {
        return this.allPeers;
    }

    /**
     * get all peers number
     * @return peer number
     */
    public synchronized int getPeerNumber() {
        return this.allPeers.size();
    }

    /**
     * get a peer in mutable range randomly
     * @return peer or null if empty
     */
    public synchronized byte[] getMutableRangePeerRandomly() {
        int size = this.priorityPeers.size();
        if (size > 0) {
            Random random = new Random(System.currentTimeMillis());
            return priorityPeers.get(random.nextInt(size)).getData();
        } else {
            logger.info("Chain ID:{}: Cannot get peer in mutable range.", new String(this.chainID));
            return null;
        }
    }

}

