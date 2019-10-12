package com.wq.netty.bytebuf.wrapper.codec;

import io.netty.buffer.ByteBuf;

public interface RedisDataTypeCodec {

    Object decode(ByteBuf byteBuf);

    ByteBuf encode(Object t);

}
