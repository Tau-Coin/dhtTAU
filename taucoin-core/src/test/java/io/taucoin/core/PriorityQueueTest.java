package io.taucoin.core;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.PriorityQueue;

public class PriorityQueueTest {
    private static final Logger logger = LoggerFactory.getLogger("test");

    @Test
    public void PriorityQueueFuncTest() {
        PriorityQueue<Integer> priorityQueue = new PriorityQueue();

        priorityQueue.add(5);
        priorityQueue.add(7);
        priorityQueue.add(4);
        priorityQueue.add(9);
        priorityQueue.add(2);
        // 2, 4, 5, 9, 7
        logger.info("priorityQueue:{}", priorityQueue);
    }
}
