package com.wq.netty.utils;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * @Author: wangqiang20995
 * @Date:2019/5/13
 * @Description:
 * @Resource:
 */
public class ByteUtils {

    public static byte[] intToSignedBytes(int number) {
        //int 32位 = 4 个byte
        byte[] bytes = new byte[4];
        bytes[0] = (byte) ((number >> 24) & 0xFF);
        bytes[1] = (byte) ((number >> 16) & 0xFF);
        bytes[2] = (byte) ((number >> 8) & 0xFF);
        bytes[3] = (byte) (number & 0xFF);

        return bytes;
    }

    public static int signedByteToInt(byte[] data) {
        if (data.length != 4) {
            return 0;
        }

        int number = 0;
        for (int i = 0; i < 4; i++) {
            int index = 8 * (3 - i);
            number += (data[i] & 0xFF) << index;
        }

        //使用|进行计算
        int or = ((data[0] & 0xFF) << 24) | ((data[1] & 0xFF) << 16) | ((data[2] & 0xFF) << 8) | ((data[3] & 0xFF) << 0);
        System.out.println(or);

        return number;
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        System.out.println(signedByteToInt(intToSignedBytes(235100)));
        String info = "<?xml version=\"1.0\" encoding=\"GB2312\" ?><MsgText><MsgHdr><ServiceNo>02</ServiceNo><FunctionNo>1001</FunctionNo><BankNo>014-1000-5</BankNo><RequestNo>2017011200000954</RequestNo><FailFlag>0</FailFlag><ErrCode>0000</ErrCode><ErrMsg>划款成功</ErrMsg><PackCount>1</PackCount></MsgHdr><MsgPack><BankCreditNo>2017011200000954</BankCreditNo></MsgPack></MsgText>";
        System.out.println(info.getBytes("GB2312").length);


        byte[] data = info.getBytes(Charset.forName("UTF-8"));
        System.out.println("length:" + data.length);
        byte[] total = new byte[4 + data.length];
        System.arraycopy(intToSignedBytes(data.length), 0, total, 0, 4);
        System.arraycopy(data, 0, total, 4, data.length);

        byte decode[] = new byte []{
            0,0,3,-32
        };
        int value = 0;
        for (int i = 0; i < decode.length; i++) {
            int shift = (decode.length - 1 - i) * 8;
            value += (decode[i] & 0xFF) << shift;
        }
        System.out.println(value);


        Integer integer = new Integer(-32);
        System.out.println(Integer.toBinaryString(integer));
        System.out.println(-32 & 0xff);

        byte[] bb = new String(decode,"GBK").getBytes("GBK");
        System.out.println();
    }

}
