package com.wq.netty.bytebuf.wrapper.codec;

import io.netty.buffer.ByteBuf;
import io.netty.util.ByteProcessor;

class ByteBufCodecUtil {

    public static int findCodeIndex(ByteBuf buf,byte code){
        if(buf == null){
            return -1;
        }

        if(code < 0){
            throw new IllegalArgumentException("code error");
        }

        ByteProcessor byteProcessor = new ByteProcessor.IndexOfProcessor(code);
        int index =  buf.forEachByte(byteProcessor);

        return (index > 0 && buf.getByte(index) == code ? index : -1);
    }
}
