package io.taucoin.core;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VotingPoolTest {
    private static final Logger logger = LoggerFactory.getLogger("test");

    @Test
    public void testPutIntoVotingPool() {
        VotingPool votingPool = new VotingPool("chain1".getBytes());
        votingPool.putIntoVotingPool("vote1".getBytes(), 100);
        votingPool.putIntoVotingPool("vote2".getBytes(), 101);
        votingPool.putIntoVotingPool("vote3".getBytes(), 102);
        votingPool.putIntoVotingPool("vote1".getBytes(), 100);
        votingPool.putIntoVotingPool("vote2".getBytes(), 101);
        Vote vote = new Vote("vote2".getBytes(), 101, 2);
        Assert.assertEquals(votingPool.getBestVote(), vote);
    }

//    public VotingPool makeVotingPool() {
//        VotingPool votingPool = new VotingPool("chain1".gtByte());
//        for (int i = 4; i > 0; i--) {
//            votingPool.putIntoVotingPool("chainID1", "vote1".getBytes(), 100);
//        }
//        for (int i = 8; i > 0; i--) {
//            votingPool.putIntoVotingPool("chainID1", "vote2".getBytes(), 100);
//        }
//        for (int i = 5; i > 0; i--) {
//            votingPool.putIntoVotingPool("chainID1", "vote3".getBytes(), 100);
//        }
//        for (int i = 5; i > 0; i--) {
//            votingPool.putIntoVotingPool("chainID1", "vote4".getBytes(), 101);
//        }
//        for (int i = 4; i > 0; i--) {
//            votingPool.putIntoVotingPool("chainID2", "vote5".getBytes(), 100);
//        }
//        return votingPool;
//    }
//
//    @Test
//    public void testPutIntoVotingPool() {
//        VotingPool votingPool = new VotingPool();
//        for (int i = 4; i > 0; i--) {
//            votingPool.putIntoVotingPool("chainID1", "vote1".getBytes(), 100);
//        }
//    }
//
//    @Test
//    public void testRemoveAllVotes() {
//        VotingPool votingPool = makeVotingPool();
//        votingPool.removeAllVotes("chainID1");
//        votingPool.removeAllVotes("chainID2");
//        votingPool.removeAllVotes("chainID3");
//    }
//
//    @Test
//    public void testGetSortedVotes() {
//        VotingPool votingPool = makeVotingPool();
//        List<Vote> list = votingPool.getSortedVotes("chainID1");
//        for (Vote vote : list) {
//            logger.info(vote.toString());
//        }
//    }
//
//    @Test
//    public void testMap() {
//        Map<String, Vote> votes = new HashMap<>();
//        Vote vote = new Vote("dd".getBytes(), 1);
//        votes.put("chain", vote);
//        List<Vote> list = new ArrayList<>(votes.values());
//        list.get(0).setBlockNumber(222);
//        logger.info("----{}", votes);
//    }

}