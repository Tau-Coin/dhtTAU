package io.taucoin.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class VotingPool {
    private static final Logger logger = LoggerFactory.getLogger("voting pool");

    private Map<String, Vote> votingPool = new HashMap<>();
    private Vote bestVote = null;

//    public VotingPool(Map<String, Map<String, Vote>> votingPool) {
//        this.votingPool = votingPool;
//    }

    /**
     * put a vote into voting pool
     * @param blockHash
     * @param blockNumber
     */
    public synchronized void putIntoVotingPool(byte[] blockHash, int blockNumber) {
        String key = new String(blockHash);
        Vote vote = votingPool.get(key);
        if (null == vote) {
            vote = new Vote(blockHash, blockNumber);
            logger.info("The first time the vote appeared:{}", vote.toString());
        }
        // 唱票
        vote.voteUp();

        // 判断是否需要更新bestVote，三种情况下需要更新：
        // 1. bestVote为null;
        // 2. bestVote票数落后;
        // 3. bestVote与选票最多的票数相同，但是高度较低
        if (null == bestVote || bestVote.getCount() < vote.getCount() ||
                (bestVote.getCount() == vote.getCount() && bestVote.getBlockNumber() < vote.getBlockNumber())) {
            bestVote = vote;
            logger.info("Update Best Vote:{}", bestVote);
        }

        // 更新投票池
        votingPool.put(key, vote);
    }

    /**
     * get best vote
     * @return
     */
    public Vote getBestVote() {
        return bestVote;
    }

    /**
     * clear the voting pool
     */
    public void clearVotingPool() {
        votingPool.clear();
        bestVote = null;
    }

    /**
     * remove all the votes corresponding to an chainID from voting pool
     * @param chainID
     */
//    public synchronized void removeAllVotes(String chainID) {
//        if(votingPool.containsKey(chainID)) {
//            votingPool.remove(chainID);
//        } else {
//            logger.info("Not found. Chain id:{}", chainID);
//        }
//    }

    /**
     * get the sorted vote list corresponding to an chainID
     * @param chainID
     * @return
     */
//    public synchronized List<Vote> getSortedVotes(String chainID) {
//        Map<String, Vote> votes = votingPool.get(chainID);
//        if(null == votes || votes.isEmpty()) {
//            logger.info("Chain ID:{} is empty.", chainID);
//            return null;
//        }
//
//        List<Vote> list = new ArrayList<>(votes.values());
//        Collections.sort(list);
//
//        return list;
//    }

}

