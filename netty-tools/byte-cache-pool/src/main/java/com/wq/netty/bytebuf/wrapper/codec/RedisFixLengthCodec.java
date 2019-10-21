package com.wq.netty.bytebuf.wrapper.codec;

import com.wq.netty.bytebuf.wrapper.core.RedisException;
import io.netty.buffer.ByteBuf;

public class RedisFixLengthCodec extends AbstractRedisDataCodec<String> {

    @Override
    protected String doDecode(ByteBuf buf) {
        return super.fixLengthDecode(buf);
    }

    @Override
    protected ByteBuf doEncode(String s) {
        throw new RedisException("redisCodec can't support fixed string encode...");
    }
}
