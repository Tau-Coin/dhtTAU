package io.taucoin.torrent.publishing.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 线程池工厂
 */
public class ThreadPoolFactory {

    private ExecutorService mThreadService;

    private ThreadPoolFactory() {
        mThreadService = Executors.newSingleThreadExecutor();
    }

    private static class ThreadPoolFactoryHolder {
        private static final ThreadPoolFactory INSTANCE = new ThreadPoolFactory();
    }

    public static ThreadPoolFactory getInstance() {
        return ThreadPoolFactoryHolder.INSTANCE;
    }

    public <T> Future<T> submitRequest(Runnable runnable, T result) {
        return mThreadService.submit(runnable, result);
    }

    public void executeRequest(Runnable runnable) {
        mThreadService.execute(runnable);
    }
}


