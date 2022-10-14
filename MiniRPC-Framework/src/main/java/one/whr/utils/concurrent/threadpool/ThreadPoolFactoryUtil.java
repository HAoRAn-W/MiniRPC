package one.whr.utils.concurrent.threadpool;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ThreadPoolFactoryUtil {
    private static final Map<String, ExecutorService> THREAD_POOLS = new ConcurrentHashMap<>();

    private ThreadPoolFactoryUtil() {

    }
    /* Generally speaking, ExecutorService automatically provides a pool of threads and an API for assigning tasks to it. */

    // default thread pool
    public static ExecutorService createCustomThreadPoolIfAbsent(String threadNamePrefix) {
        CustomThreadPoolConfig customThreadPoolConfig = new CustomThreadPoolConfig();
        return createCustomThreadPoolIfAbsent(customThreadPoolConfig, threadNamePrefix, false);

    }

    public static ExecutorService createCustomThreadPoolIfAbsent(String threadNamePrefix, CustomThreadPoolConfig customThreadPoolConfig) {
        return createCustomThreadPoolIfAbsent(customThreadPoolConfig, threadNamePrefix, false);
    }

    public static ExecutorService createCustomThreadPoolIfAbsent(CustomThreadPoolConfig customThreadPoolConfig,
                                                                 String threadNamePrefix, Boolean daemon) {
        ExecutorService threadPool = THREAD_POOLS.computeIfAbsent(threadNamePrefix, k -> createThreadPool(customThreadPoolConfig, threadNamePrefix, daemon));
        if (threadPool.isShutdown() || threadPool.isTerminated()) {
            THREAD_POOLS.remove(threadNamePrefix);
            threadPool = createThreadPool(customThreadPoolConfig, threadNamePrefix, daemon);
            THREAD_POOLS.put(threadNamePrefix, threadPool);
        }
        return threadPool;
    }

    public static void shutdownAllThreadPool() {
        log.info("=================== [ begin shutdownAllThreadPool ] ===================");
        THREAD_POOLS.entrySet().parallelStream().forEach(entry -> {
            ExecutorService executorService = entry.getValue();
            executorService.shutdown();
            log.info("shutdown thread pool [{}] [{}]", entry.getKey(), executorService.isTerminated());
            try {
                executorService.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("Thread pool not terminated");
                executorService.shutdownNow();
            }
        });
    }

    private static ExecutorService createThreadPool(CustomThreadPoolConfig customeThreadPoolConfig, String threadNamePrefix, Boolean daemon) {
        ThreadFactory threadFactory = createThreadFactory(threadNamePrefix, daemon);

        /*
         * The ThreadPoolExecutor is an extensible thread pool implementation with lots of parameters and hooks for fine-tuning.
         * corePoolSize: a fixed number of core threads that are kept inside all the time
         * maximumPoolSize: if all core threads are busy and the internal queue is full, the pool is allowed to grow up to maximumPoolSize
         * keepAliveTime: the interval of time for which the excessive threads (instantiated in excess of the corePoolSize) are allowed to exist in the idle state
         * workQueue: the queue to use for holding tasks before they are executed. This queue will hold only the Runnable tasks submitted by the execute method
         * threadFactory: the factory to use when the executor creates a new thread
         */
        return new ThreadPoolExecutor(customeThreadPoolConfig.getCorePoolSize(), customeThreadPoolConfig.getMaximumPoolSize(),
                customeThreadPoolConfig.getKeepAliveTime(), customeThreadPoolConfig.getUnit(), customeThreadPoolConfig.getWorkQueue(),
                threadFactory);
    }

    public static ThreadFactory createThreadFactory(String threadNamePrefix, Boolean daemon) {
        if (threadNamePrefix != null) {
            if (daemon != null) {
                return new ThreadFactoryBuilder()
                        .setNameFormat(threadNamePrefix + "-%d")
                        .setDaemon(daemon)
                        .build();
            } else {
                return new ThreadFactoryBuilder()
                        .setNameFormat(threadNamePrefix + "-%d")
                        .build();
            }
        }
        return Executors.defaultThreadFactory();
    }

    public static void printThredPoolStatus(ThreadPoolExecutor threadPool) {
        ScheduledExecutorService statusExecutor = new ScheduledThreadPoolExecutor(1,
                createThreadFactory("print-status-thread-pool", false));
        statusExecutor.scheduleAtFixedRate(() -> {
            log.info("============ Thread Pool Status ============");
            log.info("ThreadPool size: [{}]", threadPool.getPoolSize());
            log.info("Active threads: [{}]", threadPool.getActiveCount());
            log.info("Number of Tasks : [{}]", threadPool.getCompletedTaskCount());
            log.info("Number of Tasks in Queue: {}", threadPool.getQueue().size());
            log.info("===========================================");
        }, 0, 1, TimeUnit.SECONDS);
    }

}
