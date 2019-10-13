package com.wq.netty.bytebuf.wrapper.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.List;

import static org.junit.Assert.*;

public class RedisArrayCodecTest {

    private RedisDataTypeCodec arrayCodec = new RedisArrayCodec();

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    public void decodeNullArray() throws UnsupportedEncodingException {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(AbstractRedisDataCodec.ITEM_ARRAY);
        byteBuf.writeBytes("-1".getBytes(AbstractRedisDataCodec.CHARSET));
        byteBuf.writeBytes(AbstractRedisDataCodec.REDIS_CRLF);

        assertNull(arrayCodec.decode(byteBuf));
    }

    @Test
    public void copyByteBuf() {
        ByteBuf buf = Unpooled.copyInt(1);
        // 调用copy之后 被拷贝的ByteBuf和副本就无关了
        ByteBuf copy = buf.copy();

        ReferenceCountUtil.safeRelease(buf);
        ReferenceCountUtil.safeRelease(copy);
    }

    @Test
    public void decodeEmptyArray() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(AbstractRedisDataCodec.ITEM_ARRAY);
        buf.writeByte('0');
        buf.writeBytes(AbstractRedisDataCodec.REDIS_CRLF);

        Object object = arrayCodec.decode(buf);
        assertEquals(RedisArrayCodec.InnerList.class, object.getClass());
        List list = (List) object;
        assertEquals(0, list.size());
    }

    @Test
    public void decodeSimpleArray() throws UnsupportedEncodingException {
        ByteBuf buf = arrayByteBuf();

        Object object = arrayCodec.decode(buf);
        assertEquals(RedisArrayCodec.InnerList.class, object.getClass());

        RedisArrayCodec.InnerList<Object> list = (RedisArrayCodec.InnerList<Object>) object;
        assertEquals(3, list.size());

        logger.info("list>>:{}",list);
    }

    @Test
    public void decodeCompositeArray() throws UnsupportedEncodingException {
        ByteBuf array = arrayByteBuf();

        ByteBuf main = Unpooled.buffer();

        writeArrayHeader(main);

        writeException(main);

        main.writeBytes(array);

        // null
        writeFixedNull(main);

        // int
        writeInt(main);

        Object o = arrayCodec.decode(main);
        assertEquals(RedisArrayCodec.InnerList.class, o.getClass());

        RedisArrayCodec.InnerList list = (RedisArrayCodec.InnerList) o;
        assertEquals(4, list.size());

        logger.info("list>>:{}",list);
    }

    private ByteBuf arrayByteBuf() throws UnsupportedEncodingException {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(AbstractRedisDataCodec.ITEM_ARRAY);
        buf.writeByte('3');
        buf.writeBytes(AbstractRedisDataCodec.REDIS_CRLF);

        // string
        writePlainString(buf);

        // fix string
        writeFixedString(buf);

        // int
        writeInt(buf);
        return buf;
    }


    private void writeArrayHeader(ByteBuf main) {
        main.writeByte(AbstractRedisDataCodec.ITEM_ARRAY);
        main.writeByte('4');
        main.writeBytes(AbstractRedisDataCodec.REDIS_CRLF);
    }

    private void writeException(ByteBuf main) throws UnsupportedEncodingException {
        main.writeByte(AbstractRedisDataCodec.EXCEPTION);
        main.writeBytes("empty key".getBytes(AbstractRedisDataCodec.CHARSET));
        main.writeBytes(AbstractRedisDataCodec.REDIS_CRLF);
    }

    private void writeFixedNull(ByteBuf main) throws UnsupportedEncodingException {
        main.writeByte(AbstractRedisDataCodec.FIX_LENGTH_STRING);
        main.writeBytes("-1".getBytes(AbstractRedisDataCodec.CHARSET));
        main.writeBytes(AbstractRedisDataCodec.REDIS_CRLF);
    }

    private void writePlainString(ByteBuf buf) throws UnsupportedEncodingException {
        buf.writeByte(AbstractRedisDataCodec.PLAIN_STRING);
        buf.writeBytes("hello world".getBytes(AbstractRedisDataCodec.CHARSET));
        buf.writeBytes(AbstractRedisDataCodec.REDIS_CRLF);
    }

    private void writeFixedString(ByteBuf buf) throws UnsupportedEncodingException {
        buf.writeByte(AbstractRedisDataCodec.FIX_LENGTH_STRING);
        buf.writeByte('3');
        buf.writeBytes(AbstractRedisDataCodec.REDIS_CRLF);
        buf.writeBytes("foo".getBytes(AbstractRedisDataCodec.CHARSET));
        buf.writeBytes(AbstractRedisDataCodec.REDIS_CRLF);
    }

    private void writeInt(ByteBuf buf) throws UnsupportedEncodingException {
        buf.writeByte(AbstractRedisDataCodec.NUMBER_LONG);
        buf.writeBytes("16".getBytes(AbstractRedisDataCodec.CHARSET));
        buf.writeBytes(AbstractRedisDataCodec.REDIS_CRLF);
    }
}