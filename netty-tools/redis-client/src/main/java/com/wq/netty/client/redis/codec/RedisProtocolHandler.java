package com.wq.netty.client.redis.codec;

import com.wq.netty.bytebuf.wrapper.core.RedisException;
import com.wq.netty.client.redis.RedisResultFuture;
import com.wq.netty.client.redis.bean.RedisMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ChannelHandler.Sharable
public class RedisProtocolHandler extends SimpleChannelInboundHandler<RedisMessage> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Map<Channel, RedisResultFuture> redisClientMap;

    public Map<Channel, RedisResultFuture> getRedisClientMap() {
        return redisClientMap;
    }

    public void setRedisClientMap(Map<Channel, RedisResultFuture> redisClientMap) {
        this.redisClientMap = redisClientMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RedisMessage redisMessage) throws Exception {
        if(RedisDecoder.DECODE_STATUS_FAILED.equals(redisMessage.getMessage())){
            throw new RedisException("decode message failed");
        }

        logger.info("redis decoder:{}",redisMessage.getObject());
        if(!redisClientMap.containsKey(channelHandlerContext.channel())){
            throw new RedisException(RedisException.TIMEOUT_RESULT);
        }

        RedisResultFuture redisResultFuture = redisClientMap.get(channelHandlerContext.channel());
        redisResultFuture.received(redisMessage.getObject());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getLocalizedMessage(),cause);
        if(cause instanceof RedisException && cause.getMessage().equals(RedisException.TIMEOUT_RESULT)){
            Channel channel = ctx.channel();
            redisClientMap.remove(channel);
        }
    }
}
