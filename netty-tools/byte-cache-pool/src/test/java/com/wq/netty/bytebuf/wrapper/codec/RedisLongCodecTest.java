package com.wq.netty.bytebuf.wrapper.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

public class RedisLongCodecTest {

    private RedisDataTypeCodec codec = new RedisLongCodec();

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    public void decode() throws UnsupportedEncodingException {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(AbstractRedisDataCodec.NUMBER_LONG);

        buf.writeByte(AbstractRedisDataCodec.NEGATIVE_CODE);
        buf.writeBytes("12345".getBytes(AbstractRedisDataCodec.CHARSET));
        buf.writeBytes(AbstractRedisDataCodec.REDIS_CRLF);

        Long data = (Long) codec.decode(buf);
        logger.info("{}",data);
    }

}