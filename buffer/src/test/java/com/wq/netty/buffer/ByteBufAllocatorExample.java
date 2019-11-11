package com.wq.netty.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @Author: wangqiang20995
 * @Date: 2019/11/11 9:31
 * @Description:
 **/
public class ByteBufAllocatorExample {
    // Allocator --> Pooled/UnPooled[分配池化和未池化的ByteBuf]
    // 根据选择的池化还是非池化，进一步选择ByteBuf对象的实际类型[buffer(在堆上分配一个ByteBuf),direct(堆外分配),wrapper(将byte[]包装后返回),composite(返回一个组合，指定组合的个数)]

    public static void main(String[] args) {
        // 池化的ByteBuf申请接口
        /**
         * UnpooledByteBufAllocator
         */
//        Unpooled.buffer();
//        Unpooled.directBuffer();
//        Unpooled.wrappedBuffer(new byte[10]);
//        Unpooled.compositeBuffer();

        // 操作堆上分配的ByteBuf
        ByteBuf heap = Unpooled.buffer(10);
        heap.writeBytes(new byte[16]);
        System.out.println(heap.capacity());

        capacity();
        // 非池化的ByteBuf申请接口
        /**
         * PooledByteBufAllocator
         */
    }

    private static void capacity(){
        ByteBuf byteBuf = Unpooled.buffer(20);
        byte[] bytes = new byte[20];
        bytes[8] = 1;
        bytes[9] = 2;
        bytes[10] = 3;
        bytes[11] = 4;
        byteBuf.writeBytes(bytes);

        byteBuf.capacity(10);

    }
}
