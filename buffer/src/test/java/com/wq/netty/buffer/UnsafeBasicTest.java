package com.wq.netty.buffer;

import org.junit.Before;
import org.junit.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * @Author: wangqiang20995
 * @Date: 2020/10/22 16:46
 * @Description:
 **/
public class UnsafeBasicTest {

    private Unsafe unsafe;

    @Before
    public void init() {
        Object object = AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    final Field field = Unsafe.class.getDeclaredField("theUnsafe");
                    field.setAccessible(true);
                    return field.get(null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        this.unsafe = (Unsafe) object;
    }

    @Test
    public void printAddressOffset() {
        // 获取DirectBuffer中的address
        ByteBuffer buffer = ByteBuffer.allocateDirect(10);
        try {
            /**
             * 获取{@link java.nio.Buffer#address}对应的{@link Field}
             */
            Field field = Buffer.class.getDeclaredField("address");

            // address字段在对象中的偏移量，通过unsafe获取偏移量
            long offset = this.unsafe.objectFieldOffset(field);

            // 通过offset获取某个对象在该内存地址上的值
            long memoryAddress = unsafe.getLong(buffer, offset);

            System.out.println("offset->" + offset);
            System.out.println("memoryAddress->" + memoryAddress);

            // 写入一部分信息
            buffer.putInt(5);
            // 从memoryAddress开始读取四个byte组装成int返回
            /**
             * {@link Unsafe#getByte(long)}获取指定内存地址的字节值
             * 下面这块代码其实就是{@link io.netty.buffer.UnpooledUnsafeDirectByteBuf}里面一些列以下划线开头的方法
             */
            int value = this.unsafe.getByte(memoryAddress) << 24 |
                    (this.unsafe.getByte(memoryAddress + 1) & 0xff) << 16 |
                    (this.unsafe.getByte(memoryAddress + 2) & 0xff) << 8 |
                    this.unsafe.getByte(memoryAddress + 3) & 0xff;
            // 前面四个字节是5
            System.out.println("valueAtIndex->" + value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
