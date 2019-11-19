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
//        ByteBuf heap = Unpooled.buffer(10);
//        heap.writeBytes(new byte[16]);
//        System.out.println(heap.capacity());
//
//        capacity();
        // 非池化的ByteBuf申请接口
        /**
         * PooledByteBufAllocator
         */

        // direct
        unPooledDirectByteBuf();
    }

    private static void unPooledDirectByteBuf() {
        ByteBuf direct = Unpooled.directBuffer(10);// 实际内部对象是DirectByteBuffer实现，由Jdk管理，内部维护一个address字段，代表申请的堆外内存的开始地址，由于是一大片连续的地址，所以address+offset代表的就是申请的堆外内存中的某一个地址
        direct.writeByte('a');// 将char -> int -> byte写入
        direct.writeByte(1);// 写入的是int，但是最终会将int -> byte写入到ByteBuf中
        System.out.println(direct);
        byte[] tmp = new byte[10];
        for (int i = 0; i < tmp.length; i++) {
            tmp[i] = (byte) ('A' + i);
        }
        direct.writeBytes(tmp);
        System.out.println(direct);
    }

    private static void capacity() {
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
