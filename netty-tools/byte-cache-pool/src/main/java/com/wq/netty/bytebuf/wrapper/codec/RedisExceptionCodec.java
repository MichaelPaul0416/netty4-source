package com.wq.netty.bytebuf.wrapper.codec;

import com.wq.netty.bytebuf.wrapper.core.RedisException;
import io.netty.buffer.ByteBuf;
import io.netty.util.internal.StringUtil;

public class RedisExceptionCodec extends AbstractRedisDataCodec<RuntimeException> {
    @Override
    protected RuntimeException doDecode(ByteBuf buf) {
        String msg = super.decodePlainString(buf,EXCEPTION);
        if(StringUtil.isNullOrEmpty(msg)){
            throw new RedisException("empty err info");
        }

        return new RedisException(msg);
    }

    @Override
    protected ByteBuf doEncode(RuntimeException e) {
        throw new RedisException("redisCodec can't support Exception encode...");
    }
}
