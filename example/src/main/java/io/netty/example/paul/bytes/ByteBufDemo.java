package io.netty.example.paul.bytes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;

/**
 * @Author: wangqiang20995
 * @Date:2019/8/15
 * @Description:Netty Bytebuf操作demo
 * @Resource:
 */
public class ByteBufDemo {

    public static void main(String[] args){
        ByteBufAllocator bufAllocator = PooledByteBufAllocator.DEFAULT;

        ByteBuf byteBuf = bufAllocator.buffer();

        byteBuf.writeInt(156);

        byteBuf.release();
    }
}
