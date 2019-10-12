package com.wq.netty.bytebuf.wrapper.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

public class RedisSimpleStringCodecTest {

    private RedisDataTypeCodec redisDataTypeCodec = new RedisSimpleStringCodec();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    public void decode() throws UnsupportedEncodingException {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(AbstractRedisDataCodec.PLAIN_STRING);
        buf.writeBytes("HelloWorld".getBytes("UTF-8"));
        buf.writeBytes(AbstractRedisDataCodec.REDIS_CRLF);

        String info = (String) redisDataTypeCodec.decode(buf);
        logger.info(info);
    }

}