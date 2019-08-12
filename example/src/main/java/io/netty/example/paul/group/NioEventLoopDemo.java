package io.netty.example.paul.group;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: wangqiang20995
 * @Date:2019/8/12
 * @Description:
 * @Resource:
 */
public class NioEventLoopDemo {

    private static final Logger logger = LoggerFactory.getLogger(NioEventLoopDemo.class);

    private static class DemoEventLoop extends SingleThreadEventLoop {

        protected DemoEventLoop(EventLoopGroup parent, ThreadFactory threadFactory, boolean addTaskWakesUp) {
            super(parent, threadFactory, addTaskWakesUp);
        }

        @Override
        protected void run() {
            for (; ; ) {
                Runnable runnable = takeTask();//SingleThreadEventExecutor中构造
                if (runnable == null) {
                    continue;
                }

                logger.info("call task...");
                try {
                    runnable.run();
                } catch (Throwable e) {
                    logger.error("throwable while call task:" + e.getMessage(), e);
                }

            }
        }
    }

    private static class DemoEventLoopGroup extends MultithreadEventLoopGroup {

        /**
         * @param nThreads
         * @param executor
         * @param args     -- 实际的类型只需要传ThreadFactory的实现即可
         */
        protected DemoEventLoopGroup(int nThreads, Executor executor, Object... args) {
            super(nThreads, executor, args);
        }

        @Override
        protected EventLoop newChild(Executor executor, Object... args) throws Exception {
            ThreadFactory threadFactory = null;
            if (args.length > 1) {
                if (args[0] instanceof ThreadFactory) {
                    threadFactory = (ThreadFactory) args[0];
                }
            }

            if (threadFactory == null) {
                threadFactory = new CustomerThreadFactory();
            }

            return new DemoEventLoop(this, threadFactory, false);
        }
    }

    private static class CustomerThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup threadGroup;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String prefix = "customer-pool-";

        public CustomerThreadFactory() {
            SecurityManager securityManager = System.getSecurityManager();
            if (securityManager == null) {
                this.threadGroup = Thread.currentThread().getThreadGroup();
            } else {
                this.threadGroup = securityManager.getThreadGroup();
            }
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(this.threadGroup, r,
                    prefix + poolNumber + "-" + threadNumber.getAndIncrement() + "-thread", 0);
            if (thread.isDaemon()) {
                thread.setDaemon(false);
            }

            if (thread.getPriority() != Thread.NORM_PRIORITY) {
                thread.setPriority(Thread.NORM_PRIORITY);
            }

            return thread;
        }
    }

    public static void main(String[] args) {
//        simpleEventLoopGroup();

        simpleEventLoop();
    }

    private static void simpleEventLoop() {
        DemoEventLoopGroup group = new DemoEventLoopGroup(10, null, new CustomerThreadFactory());
        CountDownLatch latch = new CountDownLatch(1);

        group.execute(() -> {
            logger.info("caller task run in custom EventLoopGroup");
            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }

        logger.info("shutdown EventLoopGroup gracefully");
        group.shutdownGracefully();

    }

    private static void simpleEventLoopGroup() {
        /**
         * ThreadPoolExecutor是线程池，父类都是AbstractExecutorService[多个线程从一个队列里面拉取数据，对应的模型是多producer多consumer]
         * NioEventLoop可以理解为是一个线程池，它内部有一个阻塞队列和一个线程，对应的模型是多producer单consumer
         * NioEventLoopGroup本质其实就是一个NioEventLoop的数组，参考MultiThreadEventExecutorGroup
         */
        //EventLoopGroup 可以简单理解为就是jdk中的线程池，只不过它添加了一些额外的功能
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(3);

        AtomicBoolean stop = new AtomicBoolean(false);
        eventLoopGroup.execute(() -> {
            System.out.println("hello");
            stop.set(true);
        });

        while (!stop.get()) {

        }

        eventLoopGroup.shutdownGracefully();
    }
}
