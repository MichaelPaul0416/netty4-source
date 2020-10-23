package com.wq.netty.buffer;

import org.junit.Before;
import org.junit.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

/**
 * @Author: wangqiang20995
 * @Date: 2020/10/22 16:46
 * @Description:
 **/
public class UnsafeBasicTest {

    private Unsafe unsafe;

    @Before
    public void init() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            this.unsafe = (Unsafe) field.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void printAddressOffset() {
        // 获取DirectBuffer中的address
        ByteBuffer buffer = ByteBuffer.allocateDirect(10);
        try {
            long offset = unsafe.objectFieldOffset(buffer.getClass().getDeclaredField("address"));
            System.out.println("offset:" + offset);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
