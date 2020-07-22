package io.taucoin.core;

import io.taucoin.param.ChainParam;
import io.taucoin.util.ByteArrayWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PeerManager {

    private static final Logger logger = LoggerFactory.getLogger("PeerMananger");

    // chain ID
    private byte[] chainID;

    private final static int PEER_NUMBER = 8;

    // all peers
    private Set<ByteArrayWrapper> allPeers;

    // peers in mutable range
    private List<ByteArrayWrapper> priorityPeers;

    // peer info: peer <-> time
    private Map<ByteArrayWrapper, Long> peerInfo = new HashMap<>();

    // block peers
    private Set<ByteArrayWrapper> blockPeers = new HashSet<>(PEER_NUMBER);

    // tx peers
    private Set<ByteArrayWrapper> txPeers = new HashSet<>(PEER_NUMBER);

    public PeerManager(byte[] chainID) {
        this.chainID = chainID;
    }

    /**
     * init peer manager
     * @param allPeers not null
     * @param priorityPeers not null
     * @return
     */
    public boolean init(Set<ByteArrayWrapper> allPeers, List<ByteArrayWrapper> priorityPeers) {
        if (null == allPeers || allPeers.isEmpty()) {
            logger.error("All peers is empty!");
            return false;
        }

        if (null == priorityPeers || priorityPeers.isEmpty()) {
            logger.error("Priority peers is empty!");
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
     * add a peer to all peer set
     * @param peer
     */
    public void addNewPeer(byte[] peer) {
        this.allPeers.add(new ByteArrayWrapper(peer));
        if (this.priorityPeers.size() >= ChainParam.MUTABLE_RANGE) {
            this.priorityPeers.remove(0);
        }
        this.priorityPeers.add(new ByteArrayWrapper(peer));
    }

    /**
     * get optimal block peer, then remove it from set
     * @return
     */
    public byte[] popUpOptimalBlockPeer() {
        byte[] peer;

        while (true) {
            // if the number of peers is too small, remove the limit
            boolean limit = true;

            // if empty, fill it up
            if (this.blockPeers.isEmpty()) {
                int size = this.priorityPeers.size();
                Random random = new Random(System.currentTimeMillis());

                for (int i = 0; i < PEER_NUMBER; i++) {
                    // add a peer to block peers randomly from priority peers
                    this.blockPeers.add(priorityPeers.get(random.nextInt(size)));
                }

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
            if (currentTime - time > ChainParam.DefaultBlockTimeInterval) {
                break;
            }
        }

        // update latest timestamp
        long currentTime = System.currentTimeMillis() / 1000;
        this.peerInfo.put(new ByteArrayWrapper(peer), currentTime);

        return peer;
    }

    /**
     * add a peer to block peer set
     * @param peer
     */
    public void addBlockPeer(byte[] peer) {
        // if full, throw it away
        if (this.blockPeers.size() < PEER_NUMBER) {
            this.blockPeers.add(new ByteArrayWrapper(peer));
        }
    }

    /**
     * get optimal tx peer, then remove it from set
     * @return
     */
    public byte[] popUpOptimalTxPeer() {
        // if empty, fill it up
        if (this.txPeers.isEmpty()) {
            int size = this.priorityPeers.size();
            Random random = new Random(System.currentTimeMillis());

            for (int i = 0; i < PEER_NUMBER; i++) {
                // add a peer to tx peers randomly from priority peers
                this.txPeers.add(priorityPeers.get(random.nextInt(size)));
            }
        }
        // get first peer, then remove it
        Iterator<ByteArrayWrapper> iterator = this.txPeers.iterator();
        byte[] peer = iterator.next().getData();
        iterator.remove();
        return peer;
    }

    /**
     * add a peer to tx peer set
     * @param peer
     */
    public void addTxPeer(byte[] peer) {
        // if full, throw it away
        if (this.txPeers.size() < PEER_NUMBER) {
            this.txPeers.add(new ByteArrayWrapper(peer));
        }
    }

}

