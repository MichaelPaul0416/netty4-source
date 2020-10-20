package com.wq.netty.buffer;

import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * @Author: wangqiang20995
 * @Date: 2020/10/20 16:38
 * @Description:
 **/
public class ByteBufferTest {

    @Test
    public void display() {
        ByteBuffer buffer = ByteBuffer.allocate(10);

        /**
         * position=3,limit=9,capacity=9
         */
        buffer.put((byte) 1);
        buffer.put((byte) 2);
        buffer.put((byte) 3);
        System.out.println("--------before--------");
        printByteBuffer(buffer);

        /**
         * 将limit设置为position的值，并且将position设置为0,
         * 读的时候就从position读取到limit
         */
        System.out.println("--------end--------");
        buffer.flip();
        printByteBuffer(buffer);
    }

    private void printByteBuffer(ByteBuffer buffer) {
        printLimit(buffer);
        printPosition(buffer);
    }


    private void printPosition(ByteBuffer buffer) {
        System.out.println("position:" + buffer.position());
    }

    private void printLimit(ByteBuffer buffer) {
        System.out.println("limit:" + buffer.limit());
    }
}
