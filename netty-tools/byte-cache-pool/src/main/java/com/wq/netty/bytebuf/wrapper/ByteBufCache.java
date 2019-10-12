package com.wq.netty.bytebuf.wrapper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ByteProcessor;

import java.nio.charset.StandardCharsets;

public class ByteBufCache {

    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.US_ASCII);

    public static void main(String[] args) {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeBytes("hello".getBytes());
        byteBuf.writeBytes(CRLF);

        /**
         * 找到对应的byte的位置，从1开始，暨1->0
         */
        int index = byteBuf.forEachByte(ByteProcessor.FIND_LF);
        System.out.println(index > 0 && byteBuf.getByte(index - 1) == '\r' ? index : -1);

        byte[] b = "-1".getBytes();
        System.out.println(Integer.toBinaryString(-1));
        System.out.println(b);
    }
}
