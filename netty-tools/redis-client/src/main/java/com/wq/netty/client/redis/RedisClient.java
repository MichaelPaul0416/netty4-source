package com.wq.netty.client.redis;

import com.wq.netty.bytebuf.wrapper.codec.AbstractRedisDataCodec;
import com.wq.netty.bytebuf.wrapper.core.RedisException;
import com.wq.netty.client.redis.bean.RedisMessage;
import com.wq.netty.client.redis.codec.RedisDecoder;
import com.wq.netty.client.redis.codec.RedisEncoder;
import com.wq.netty.client.redis.codec.RedisProtocolHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RedisClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String host;
    private int port;

    private Bootstrap bootstrap;
    private Channel channel;
    private boolean start;
    private RedisProtocolHandler handler;

    private NioEventLoopGroup nioEventLoopGroup;


    public RedisClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.bootstrap = new Bootstrap();
        this.nioEventLoopGroup = new NioEventLoopGroup();

        handler = prepare();

        try {
            ChannelFuture channelFuture = this.bootstrap.connect(this.host, this.port).sync();
            this.channel = channelFuture.channel();

            Map<Channel,RedisResultFuture> futureMap = new ConcurrentHashMap<>();
            futureMap.put(channel,new RedisResultFuture(channel));
            handler.setRedisClientMap(futureMap);

            start = true;
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(),e);
            throw new RedisException(e);
        } finally {
            if (!start) {
                this.nioEventLoopGroup.shutdownGracefully();
            }
        }
    }

    private RedisProtocolHandler prepare() {
        RedisProtocolHandler redisProtocolHandler = new RedisProtocolHandler();

        this.bootstrap.group(this.nioEventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline channelPipeline = socketChannel.pipeline();
                        channelPipeline.addLast(new RedisEncoder())
                                .addLast(new RedisDecoder())
                                .addLast(redisProtocolHandler);
                    }
                });

        return redisProtocolHandler;
    }

    public <T> T execute(String cmd) {
        RedisMessage redisMessage = new RedisMessage();
        redisMessage.setType(AbstractRedisDataCodec.PLAIN_STRING);
        redisMessage.setObject(cmd);
        RedisResultFuture<T> future = handler.getRedisClientMap().get(channel);
        return (T) future.execute(cmd).get();
    }
}
