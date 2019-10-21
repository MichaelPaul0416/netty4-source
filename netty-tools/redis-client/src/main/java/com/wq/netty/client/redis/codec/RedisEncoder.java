package com.wq.netty.client.redis.codec;

import com.wq.netty.bytebuf.wrapper.codec.AbstractRedisDataCodec;
import com.wq.netty.bytebuf.wrapper.codec.RedisSimpleStringCodec;
import com.wq.netty.bytebuf.wrapper.core.RedisException;
import com.wq.netty.client.redis.bean.RedisMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 将RedisMessage编码为ByteBuf对象
 */
public class RedisEncoder extends MessageToByteEncoder<RedisMessage> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RedisSimpleStringCodec simpleStringCodec = new RedisSimpleStringCodec();

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RedisMessage redisMessage, ByteBuf byteBuf) throws Exception {
        logger.info("encode redis cmd");

        if(AbstractRedisDataCodec.PLAIN_STRING != redisMessage.getType()){
            throw new RedisException("redis命令编码不支持");
        }

        ByteBuf buf = simpleStringCodec.encode(redisMessage.getObject());
        byteBuf.writeBytes(buf);
    }
}
