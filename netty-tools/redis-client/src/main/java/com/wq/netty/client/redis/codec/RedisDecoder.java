package com.wq.netty.client.redis.codec;

import com.wq.netty.bytebuf.wrapper.codec.*;
import com.wq.netty.bytebuf.wrapper.core.RedisException;
import com.wq.netty.client.redis.bean.RedisMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RedisDecoder extends ByteToMessageDecoder {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String DECODE_STATUS_OK = "ok";

    public static final String DECODE_STATUS_FAILED = "failed";

    private static final Map<Byte, AbstractRedisDataCodec> handlerDecoder = new ConcurrentHashMap<>();

    static {
        handlerDecoder.put(AbstractRedisDataCodec.PLAIN_STRING, new RedisSimpleStringCodec());
        handlerDecoder.put(AbstractRedisDataCodec.EXCEPTION, new RedisExceptionCodec());
        handlerDecoder.put(AbstractRedisDataCodec.NUMBER_LONG, new RedisLongCodec());
        handlerDecoder.put(AbstractRedisDataCodec.FIX_LENGTH_STRING, new RedisFixLengthCodec());
        handlerDecoder.put(AbstractRedisDataCodec.ITEM_ARRAY, new RedisArrayCodec());
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        logger.info("解码redis返回的数据，共:{} 字节", byteBuf.readableBytes());

        ByteBuf buf = byteBuf;
        byte start = buf.getByte(0);
        if (!handlerDecoder.containsKey(start)) {
            logger.info("收到半包...");
            return;
        }

        AbstractRedisDataCodec<?> codec = handlerDecoder.get(start);
        if (codec == null) {
            throw new RedisException("不存在对应数据类型的解码器:" + (char) start);
        }

        Object object = codec.decode(byteBuf);
        RedisMessage redisMessage = new RedisMessage();
        redisMessage.setType(start);
        redisMessage.setMessage(DECODE_STATUS_OK);
        redisMessage.setObject(object);
        list.add(redisMessage);
    }
}
