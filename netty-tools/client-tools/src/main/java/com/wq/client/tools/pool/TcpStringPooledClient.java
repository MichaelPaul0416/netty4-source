package com.wq.client.tools.pool;

import com.wq.client.tools.SimplePoolChannelHandler;
import com.wq.client.tools.StringPrefixHandler;
import com.wq.netty.core.pool.CallBackProcessor;
import com.wq.netty.core.pool.PooledClient;
import com.wq.netty.core.pool.ProtocolCallbackSelector;
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

    private static class NettyClient{
        private final Bootstrap bootstrap;
        private final EventLoopGroup eventLoopGroup;
        private final StringDecoder stringDecoder = new StringDecoder();
        private final StringEncoder stringEncoder = new StringEncoder();
        private final ChannelHandler receiveHandler;
        private final ProtocolCallbackSelector selector = new DefaultStringCallbackSelector();
        private final int retryTimes ;

        public NettyClient(int size,int retryTimes){
            if (size < 0){
                throw new IllegalArgumentException("EventLoopGroup size must > 0");
            }

            if(retryTimes < 0){
                throw new IllegalArgumentException("retry times should > 0");
            }

            this.bootstrap = new Bootstrap();
            this.eventLoopGroup = new NioEventLoopGroup(size);
            this.receiveHandler = new SimplePoolChannelHandler(selector);
            this.retryTimes = retryTimes;

            this.bootstrap.group(this.eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE,true)
                    .option(ChannelOption.TCP_NODELAY,true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{(byte)'$'});
                            pipeline.addLast(new DelimiterBasedFrameDecoder(1024,buf))
                                    .addLast(stringDecoder)
                                    .addLast(stringEncoder)
                                    .addLast(new StringPrefixHandler("#"))
                                    .addLast(receiveHandler);
                        }
                    });

        }

        private boolean registerCallback(CallBackProcessor<StringProtocol> processor){
            selector.registerCallback(processor);
            return true;
        }

        private Channel connect(InetSocketAddress socketAddress){
            ChannelFuture future = this.bootstrap.connect(socketAddress);
            Channel channel;
            try {
                channel = future.sync().channel();
                return channel;
            } catch (InterruptedException e) {
                logger.error("等待连接建立完成中断...尝试再次连接");
                int times = 0;

                while (times < retryTimes){
                    try {
                        channel = future.sync().channel();
                        return channel;
                    } catch (InterruptedException e1) {
                        logger.error("重试第[{}]次失败...",times + 1);
                        times ++;
                    }
                }

                return null;
            }
        }

        private boolean shutdownComplete(){
            this.eventLoopGroup.shutdownGracefully();
            return true;
        }
    }

    public TcpStringPooledClient(int size,int retry){

        this.pooledChannel = new Channel[size];
        this.counter = new AtomicInteger(0);
        this.nettyClient = new NettyClient(size,retry);
    }

    @Override
    public Channel selectChannel() {
        int index = selectIndex();

        Channel channel = this.pooledChannel[index];
        if(activeChannel(channel)){
            return channel;
        }

        if(!prepareAndRegisterChannel(index,this.serverAddress,this.port)){
            String tips = String.format("server[%:%s] can not connected",this.serverAddress,this.port);
            throw new IllegalStateException(tips);
        }

        return this.pooledChannel[index];
    }

    private boolean prepareAndRegisterChannel(int index,String address,int port){
        InetSocketAddress socketAddress = new InetSocketAddress(address,port);

        Channel channel = this.nettyClient.connect(socketAddress);
        if(channel == null){
            return false;
        }

        this.pooledChannel[index] = channel;
        return true;
    }

    private boolean activeChannel(Channel channel){
        return channel != null && channel.isActive();
    }

    private int selectIndex(){
        int index = this.counter.getAndAdd(1);
        if(index >= Integer.MAX_VALUE){
            this.counter.set(0);
            index = 0;
        }

        return index % this.pooledChannel.length;
    }


    @Override
    public void bindBase(String serverAddress,int port) {
        this.serverAddress = serverAddress;
        this.port = port;
    }

    @Override
    public boolean shutdownNow() {
        //需要关闭所有的channel，然后shutdownGracefully
        for(Channel channel : this.pooledChannel){
            channel.close();
        }

        return this.nettyClient.shutdownComplete();
    }

    @Override
    public void submitCallback(CallBackProcessor<? extends AbstractProtocol> processor) {
        this.nettyClient.registerCallback((CallBackProcessor<StringProtocol>) processor);
    }
}
