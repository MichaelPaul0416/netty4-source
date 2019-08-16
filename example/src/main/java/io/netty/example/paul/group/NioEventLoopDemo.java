package io.netty.example.paul.group;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
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

        //EventLoop中的每个worker干活的实体内容，也就是每个工作线程在这个方法里面循环
        @Override
        protected void run() {
            for (; ; ) {
                //这里需要判断下，EventLoop中的状态是不是shutdown，如果是的话就需要return
                //同时需要判断阻塞队列中是否还有任务，如果有任务的话需要等任务执行完毕
                /**
                 * SingleThreadEventExecutor的五种状态
                 * ST_NOT_STARTED:1
                 * ST_STARTED:2
                 * ST_SHUTTING_DOWN:3
                 * ST_SHUTDOWN:4
                 * ST_TERMINATED:5
                 */
                if (isShuttingDown()) {
                    if (!hasTasks() && confirmShutdown()) {
                        return;
                    }
                }

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
        scheduledTask();
//        simpleEventLoop();
    }

    private static void scheduledTask() {
        EventLoopGroup group = new NioEventLoopGroup(3);

        //call in future after specific time
        group.schedule(new Runnable() {
            @Override
            public void run() {
                logger.info("延迟任务开始执行...");
            }
        }, 1, TimeUnit.SECONDS);

        //以一定的频率执行,通常来说是按照下面的配置，每4s执行一次
        //但是当任务的执行时间>配置的频率的话，比如下面配置的任务执行时间是5s，但是间隔时间是4s
        //那么任务的执行会变为每5s执行一次，周期=max(task.time,config time)
        group.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                logger.info("以一定频率执行的task...");
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    logger.error("fixed rate error:{}",e.getMessage(),e);
                }
            }
        }, 1, 4, TimeUnit.SECONDS);//每4s执行一次

        //程序开始后1s执行，然后以后每3s执行一次：执行的周期=入参设置的延迟的时间(3s) + task执行的实现(5s)
        group.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                logger.info("以一定的延迟间隔执行task...");
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    logger.error("delay error:{}",e.getMessage(),e);
                }
            }
        }, 1, 3, TimeUnit.SECONDS);
    }

    private static void simpleEventLoop() {
        DemoEventLoopGroup group = new DemoEventLoopGroup(10, null, new CustomerThreadFactory());
        final CountDownLatch latch = new CountDownLatch(1);

        group.execute(new Runnable() {
            @Override
            public void run() {
                logger.info("caller task run in custom EventLoopGroup");
                latch.countDown();
            }
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

        final AtomicBoolean stop = new AtomicBoolean(false);
        eventLoopGroup.execute(new Runnable() {
            @Override
            public void run() {
                System.out.println("hello");
                stop.set(true);
            }
        });

        while (!stop.get()) {

        }

        eventLoopGroup.shutdownGracefully();
    }
}
