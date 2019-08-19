package com.wq.client.tools.pool;

import com.wq.client.tools.SimplePoolChannelHandler;
import com.wq.client.tools.StringPrefixHandler;
import com.wq.netty.core.pool.*;
import com.wq.netty.core.proto.AbstractProtocol;
import com.wq.client.tools.core.proto.StringProtocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @Author: wangqiang20995
 * @Date:2019/8/16
 * @Description:
 * @Resource:
 */
public class TcpStringPooledClient implements PooledClient {

    private final static Logger logger = LoggerFactory.getLogger(TcpStringPooledClient.class);

    private final Channel[] pooledChannel;

    private final AtomicInteger counter;

    private final NettyClient nettyClient;

    private String serverAddress;

    private int port;

    private final Semaphore semaphore;

    private final Semaphore workerCounter;

    private final LinkedBlockingQueue<WriteRequestTask> writeTaskQueue;

    private final Thread backThread;

    private volatile boolean stop = false;

    private static class NettyClient {
        private final Bootstrap bootstrap;
        private final EventLoopGroup eventLoopGroup;
        private final StringDecoder stringDecoder = new StringDecoder();
        private final StringEncoder stringEncoder = new StringEncoder();
        private final ChannelHandler receiveHandler;
        private final ProtocolCallbackSelector selector = new DefaultStringCallbackSelector();
        private final int retryTimes;

        public NettyClient(int size, int retryTimes) {
            if (size < 0) {
                throw new IllegalArgumentException("EventLoopGroup size must > 0");
            }

            if (retryTimes < 0) {
                throw new IllegalArgumentException("retry times should > 0");
            }

            this.bootstrap = new Bootstrap();
            this.eventLoopGroup = new NioEventLoopGroup(size);
            this.receiveHandler = new SimplePoolChannelHandler(selector);
            this.retryTimes = retryTimes;

            this.bootstrap.group(this.eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{(byte) '$'});
                            pipeline.addLast(new DelimiterBasedFrameDecoder(1024, buf))
                                    .addLast(stringDecoder)
                                    .addLast(stringEncoder)
                                    .addLast(new StringPrefixHandler("#"))
                                    .addLast(receiveHandler);
                        }
                    });

        }

        private boolean registerCallback(CallBackProcessor<StringProtocol> processor) {
            selector.registerCallback(processor);
            return true;
        }

        private Channel connect(InetSocketAddress socketAddress) {
            Channel channel;
            try {
                channel = this.bootstrap.connect(socketAddress).sync().channel();
                return channel;
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    logger.error("等待连接建立完成中断...尝试再次连接");
                } else {
                    logger.error("连接建立异常...尝试再次连接");
                }

                int times = 0;

                while (times < retryTimes) {
                    try {
                        channel = this.bootstrap.connect(socketAddress).sync().channel();
                        return channel;
                    } catch (Exception e1) {
                        logger.error("重试第[{}]次失败...", times + 1);
                        times++;
                    }
                }
                return null;
            }
        }

        private boolean shutdownComplete() {
            this.eventLoopGroup.shutdownGracefully();
            return true;
        }
    }

    public TcpStringPooledClient(int size, int retry) {

        this.pooledChannel = new Channel[size];
        this.counter = new AtomicInteger(0);
        this.nettyClient = new NettyClient(size, retry);
        this.semaphore = new Semaphore(size);
        this.workerCounter = new Semaphore(size);

        //初始化大小定为size的大小 * 2
        this.writeTaskQueue = new LinkedBlockingQueue<>(size >> 1);
        this.backThread = new Thread(new Runnable() {
            @Override
            public void run() {
                TcpStringPooledClient.this.runTask();
            }
        });
        this.startBackThread();
    }

    /**
     * 开启后台线程，阻塞式的从RequestTask队列中获取元素
     * 同时，它需要与asynWriteChannel方法并发竞争writerCounter这个信号量
     */
    private void runTask() {
        for (; ; ) {
            if (this.stop) {
                logger.info("shutdownNow signal...stop this pool");
                return;
            }

            WriteRequestTask task;
            try {
                task = this.writeTaskQueue.take();
            } catch (InterruptedException e) {
                //获取中断状态并且清除
                logger.error("interrupted and it's state:[{}]", Thread.interrupted(), e);
                continue;
            }

            try {
                //此时task肯定不是null,然后去获取令牌
                if (this.getWriteToken()) {

                    Channel channel = this.pooledChannel[task.getChannelId()];
                    ChannelFuture channelFuture = channel.writeAndFlush(task.requestParam());
                    List<ChannelFutureListener> listeners = task.futureListeners();
                    if (listeners != null) {
                        for (ChannelFutureListener listener : listeners) {
                            channelFuture.addListener(listener);
                        }
                    }

                    this.safeReleaseToken();
                }
            } catch (Exception e) {
                logger.error("task[{}]发送数据异常:{}", task.taskId(), e.getMessage(), e);
            }
        }
    }

    private void startBackThread() {
        this.backThread.setPriority(Thread.NORM_PRIORITY);
        this.backThread.setDaemon(false);
        this.backThread.setName("asyn-take-worker");

        this.backThread.start();
    }


    @Override
    public ChannelWrapper selectChannel() {
        int index = selectIndex();

        /**
         * 可能存在并发的问题，比如pooled的大小是10，初始化完成之后，一下子进来15个请求，前10个都是去建立连接了
         * 同时假设此时网络环境很差，经常连不上，连接花费的时间比较久，那么11-15个请求，其实在activeChannel(channel) == false
         * 那么这个请求也会去初始化channel，但是实际上，这些请求应该是去等待
         */
        if (!hasIdleChannel()) {
            //直接返回null，不需要放入任务队列，因为后面调用发送方法的时候，会判断channel是否为null
            return new ChannelWrapper(null, index);
        }

        Channel channel = this.pooledChannel[index];
        if (activeChannel(channel)) {
            return new ChannelWrapper(channel, index);
        }

        if (!prepareAndRegisterChannel(index, this.serverAddress, this.port)) {//在这里返回信号量
            String tips = String.format("server[%s\\:%s] can not connected", this.serverAddress, this.port);
            throw new ConnectedFailedException(tips);
        }

        return new ChannelWrapper(this.pooledChannel[index], index);
    }

    private boolean hasIdleChannel() {
        return this.semaphore.tryAcquire();
    }

    private void safeReleaseChannel() {
        this.semaphore.release();
    }

    private boolean getWriteToken() {
        return this.workerCounter.tryAcquire();
    }

    private void safeReleaseToken() {
        this.workerCounter.release();
    }

    private boolean prepareAndRegisterChannel(int index, String address, int port) {
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);

        Channel channel = this.nettyClient.connect(socketAddress);
        this.safeReleaseChannel();

        if (channel == null) {
            return false;
        }

        this.pooledChannel[index] = channel;
        return true;
    }

    private boolean activeChannel(Channel channel) {
        return channel != null && channel.isActive();
    }

    private int selectIndex() {
        int index = this.counter.getAndAdd(1);
        if (index >= Integer.MAX_VALUE) {
            this.counter.set(0);
            index = 0;
        }

        return index % this.pooledChannel.length;
    }


    @Override
    public void bindBase(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
    }

    @Override
    public boolean shutdownNow() {
        synchronized (this) {
            this.stop = true;
            this.backThread.interrupt();
        }

        //需要关闭所有的channel，然后shutdownGracefully
        for (Channel channel : this.pooledChannel) {
            if (channel != null) {
                channel.close();
            }
        }

        return this.nettyClient.shutdownComplete();
    }

    @Override
    public void submitCallback(CallBackProcessor<? extends AbstractProtocol> processor) {
        this.nettyClient.registerCallback((CallBackProcessor<StringProtocol>) processor);
    }

    @Override
    public void asynWriteChannel(Channel channel, PoolParam poolParam, ChannelFutureListener... listeners) {
        //channel initializing
        if (channel == null) {
            logger.info("channel initializing...and register task");
            offerTask(poolParam, listeners);
            return;

        }

        //除了这个方法会被并发执行，需要竞争token之外，执行队列中的RequestTask也需要token，因为token真正关联的其实就是Channel
        if (this.getWriteToken()) {
            ChannelFuture channelFuture = channel.writeAndFlush(poolParam);
            if (listeners != null) {
                for (ChannelFutureListener listener : listeners) {
                    channelFuture.addListener(listener);
                }
            }

            this.safeReleaseToken();
        } else {
            logger.info("all channel are busy and this request[{}] will offer to queue...", poolParam.getUuid());
            this.offerTask(poolParam, listeners);
        }
    }

    private void offerTask(PoolParam poolParam, ChannelFutureListener[] listeners) {
        boolean ok =
                this.writeTaskQueue.offer(new WriteRequestTask(poolParam.getUuid(), poolParam.getChannelId(), Arrays.asList(listeners)));

        if (!ok) {
            throw new IllegalStateException("将当前请求[" + poolParam.getUuid() + "]放入等待队列失败...");
        }
    }
}
