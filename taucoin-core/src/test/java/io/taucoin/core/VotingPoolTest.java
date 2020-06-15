package io.taucoin.core;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class VotingPoolTest {
    private static final Logger logger = LoggerFactory.getLogger("test");

    public VotingPool makeVotingPool() {
        VotingPool votingPool = new VotingPool();
        for (int i = 4; i > 0; i--) {
            votingPool.putIntoVotingPool("chainID1", "vote1".getBytes(), 100);
        }
        for (int i = 8; i > 0; i--) {
            votingPool.putIntoVotingPool("chainID1", "vote2".getBytes(), 100);
        }
        for (int i = 5; i > 0; i--) {
            votingPool.putIntoVotingPool("chainID1", "vote3".getBytes(), 100);
        }
        for (int i = 5; i > 0; i--) {
            votingPool.putIntoVotingPool("chainID1", "vote4".getBytes(), 101);
        }
        for (int i = 4; i > 0; i--) {
            votingPool.putIntoVotingPool("chainID2", "vote5".getBytes(), 100);
        }
        return votingPool;
    }

    @Test
    public void testPutIntoVotingPool() {
        VotingPool votingPool = new VotingPool();
        for (int i = 4; i > 0; i--) {
            votingPool.putIntoVotingPool("chainID1", "vote1".getBytes(), 100);
        }
    }

    @Test
    public void testRemoveAllVotes() {
        VotingPool votingPool = makeVotingPool();
        votingPool.removeAllVotes("chainID1");
        votingPool.removeAllVotes("chainID2");
        votingPool.removeAllVotes("chainID3");
    }

    @Test
    public void testGetSortedVotes() {
        VotingPool votingPool = makeVotingPool();
        List<Vote> list = votingPool.getSortedVotes("chainID1");
        for (Vote vote : list) {
            logger.info(vote.toString());
        }
    }
}
