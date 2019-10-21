package com.wq.netty.bytebuf.wrapper.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisSimpleStringCodec extends AbstractRedisDataCodec<String>{

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected String doDecode(ByteBuf buf) {
        return decodePlainString(buf,PLAIN_STRING);
    }

    @Override
    protected ByteBuf doEncode(String s) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(super.encodeString(s));
        buf.writeBytes(AbstractRedisDataCodec.REDIS_CRLF);
        return buf;
    }
}
