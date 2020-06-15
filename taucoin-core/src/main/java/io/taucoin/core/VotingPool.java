package io.taucoin.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class VotingPool {
    private static final Logger logger = LoggerFactory.getLogger("voting pool");

    private Map<String, Map<String, Vote>> votingPool = new HashMap<>();

//    public VotingPool(Map<String, Map<String, Vote>> votingPool) {
//        this.votingPool = votingPool;
//    }

    /**
     * put a vote into voting pool
     * @param chainID
     * @param blockHash
     * @param blockNumber
     */
    public synchronized void putIntoVotingPool(String chainID, byte[] blockHash, int blockNumber) {
        Map<String, Vote> votes = votingPool.get(chainID);
        if(null == votes) {
            logger.info("ChainID[{}] not found in voting pool. Make a new one.", chainID);
            votes = new HashMap<>();
        }
        String key = new String(blockHash);
        Vote vote = votes.get(key);
        if (null == vote) {
            vote = new Vote(blockHash, blockNumber);
            logger.info("The first time the vote appeared:{}", vote.toString());
        }
        // 唱票
        vote.voteUp();

        // 更新投票池
        votes.put(key, vote);
        votingPool.put(chainID, votes);
    }

    /**
     * remove all the votes corresponding to an chainID from voting pool
     * @param chainID
     */
    public synchronized void removeAllVotes(String chainID) {
        if(votingPool.containsKey(chainID)) {
            votingPool.remove(chainID);
        } else {
            logger.info("Not found. Chain id:{}", chainID);
        }
    }

    /**
     * get the sorted vote list corresponding to an chainID
     * @param chainID
     * @return
     */
    public synchronized List<Vote> getSortedVotes(String chainID) {
        Map<String, Vote> votes = votingPool.get(chainID);
        if(null == votes || votes.isEmpty()) {
            logger.info("Chain ID:{} is empty.", chainID);
            return null;
        }

        List<Vote> list = new ArrayList<>(votes.values());
        Collections.sort(list);

        return list;
    }
}

