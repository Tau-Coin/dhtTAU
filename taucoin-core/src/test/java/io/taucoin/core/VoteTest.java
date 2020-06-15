package io.taucoin.core;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VoteTest {
    private static final Logger logger = LoggerFactory.getLogger("test");

    @Test
    public void testVote() {
        Vote vote1 = new Vote("vote1".getBytes(), 100, 5);
        Vote vote2 = new Vote("vote2".getBytes(), 100, 11);
        Vote vote3 = new Vote("vote3".getBytes(), 101, 7);
        Vote vote4 = new Vote("vote4".getBytes(), 100, 7);
        Vote vote5 = new Vote("vote5".getBytes(), 103, 8);
        List<Vote> list = new ArrayList<>(Arrays.asList(vote1, vote2, vote3, vote4, vote5));
        Collections.sort(list);
        for (Vote vote : list) {
            logger.info(vote.toString());
        }
    }
}
