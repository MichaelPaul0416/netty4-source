package com.wq.netty.bytebuf.wrapper.codec;

import com.wq.netty.bytebuf.wrapper.core.RedisException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertEquals;

public class RedisExceptionCodecTest {

    private RedisDataTypeCodec exceptionCodec = new RedisExceptionCodec();


    @Test
    public void decode() throws UnsupportedEncodingException {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(AbstractRedisDataCodec.EXCEPTION);
        byteBuf.writeBytes("empty keys".getBytes(AbstractRedisDataCodec.CHARSET));
        byteBuf.writeBytes(AbstractRedisDataCodec.REDIS_CRLF);


        assertEquals(RedisException.class,exceptionCodec.decode(byteBuf).getClass());

    }
}