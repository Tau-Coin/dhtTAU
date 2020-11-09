package io.taucoin.core;

import java.util.LinkedHashMap;

import io.taucoin.types.Block;
import io.taucoin.types.BlockContainer;
import io.taucoin.types.HorizontalItem;
import io.taucoin.types.Transaction;
import io.taucoin.types.VerticalItem;
import io.taucoin.util.ByteArrayWrapper;

public class LRUCache {

    // block container cache
    public static class BlockContainerCache extends LinkedHashMap<ByteArrayWrapper, BlockContainer> {
        protected int maxElements;

        public BlockContainerCache(int maxSize)
        {
            super(maxSize, 0.75F, true);
            maxElements = maxSize;
        }

        public BlockContainerCache(int maxSize, boolean accessOrder)
        {
            super(maxSize, 0.75F, accessOrder);
            maxElements = maxSize;
        }

        protected boolean removeEldestEntry(java.util.Map.Entry eldest)
        {
            return size() > maxElements;
        }
    }

    // block cache
    public static class BlockCache extends LinkedHashMap<ByteArrayWrapper, Block> {
        protected int maxElements;

        public BlockCache(int maxSize)
        {
            super(maxSize, 0.75F, true);
            maxElements = maxSize;
        }

        public BlockCache(int maxSize, boolean accessOrder)
        {
            super(maxSize, 0.75F, accessOrder);
            maxElements = maxSize;
        }

        protected boolean removeEldestEntry(java.util.Map.Entry eldest)
        {
            return size() > maxElements;
        }
    }

    // tx cache
    public static class TxCache extends LinkedHashMap<ByteArrayWrapper, Transaction> {
        protected int maxElements;

        public TxCache(int maxSize)
        {
            super(maxSize, 0.75F, true);
            maxElements = maxSize;
        }

        public TxCache(int maxSize, boolean accessOrder)
        {
            super(maxSize, 0.75F, accessOrder);
            maxElements = maxSize;
        }

        protected boolean removeEldestEntry(java.util.Map.Entry eldest)
        {
            return size() > maxElements;
        }
    }

    // vertical item cache
    public static class VerticalItemCache extends LinkedHashMap<ByteArrayWrapper, VerticalItem> {
        protected int maxElements;

        public VerticalItemCache(int maxSize)
        {
            super(maxSize, 0.75F, true);
            maxElements = maxSize;
        }

        public VerticalItemCache(int maxSize, boolean accessOrder)
        {
            super(maxSize, 0.75F, accessOrder);
            maxElements = maxSize;
        }

        protected boolean removeEldestEntry(java.util.Map.Entry eldest)
        {
            return size() > maxElements;
        }
    }

    // horizontal item cache
    public static class HorizontalItemCache extends LinkedHashMap<ByteArrayWrapper, HorizontalItem> {
        protected int maxElements;

        public HorizontalItemCache(int maxSize)
        {
            super(maxSize, 0.75F, true);
            maxElements = maxSize;
        }

        public HorizontalItemCache(int maxSize, boolean accessOrder)
        {
            super(maxSize, 0.75F, accessOrder);
            maxElements = maxSize;
        }

        protected boolean removeEldestEntry(java.util.Map.Entry eldest)
        {
            return size() > maxElements;
        }
    }
}
