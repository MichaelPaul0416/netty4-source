package com.wq.netty.bytebuf.wrapper.codec;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisLongCodec extends AbstractRedisDataCodec<Long>{

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected Long doDecode(ByteBuf buf) {
        return super.decodeLong(buf);
    }

    @Override
    protected ByteBuf doEncode(Long aLong) {
        return null;
    }
}
