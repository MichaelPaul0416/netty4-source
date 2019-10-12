package com.wq.netty.bytebuf.wrapper.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static org.junit.Assert.*;

public class RedisFixLengthCodecTest {

    private RedisDataTypeCodec redisDataTypeCodec = new RedisFixLengthCodec();

    @Test
    public void decodeEmpty() throws UnsupportedEncodingException {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(AbstractRedisDataCodec.FIX_LENGTH_STRING);
        buf.writeBytes("-1".getBytes(AbstractRedisDataCodec.CHARSET));
        buf.writeBytes(AbstractRedisDataCodec.REDIS_CRLF);

        assertNull(redisDataTypeCodec.decode(buf));
    }

    @Test
    public void decodeFixString() throws UnsupportedEncodingException {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(AbstractRedisDataCodec.FIX_LENGTH_STRING);
        buf.writeByte('5');
        buf.writeBytes(AbstractRedisDataCodec.REDIS_CRLF);
        buf.writeBytes("hello".getBytes(AbstractRedisDataCodec.CHARSET));
        buf.writeBytes(AbstractRedisDataCodec.REDIS_CRLF);

        assertEquals("hello",redisDataTypeCodec.decode(buf));
    }

}