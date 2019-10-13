package com.wq.netty.bytebuf.wrapper.codec;

import com.wq.netty.bytebuf.wrapper.core.RedisException;

public enum RedisDataEnum {
    PLAIN_STRING((byte) '+', "string"),

    EXCEPTION((byte) '-', "exception"),

    INTEGER((byte) ':', "integer"),

    FIXED_STRING((byte) '$', "fixedString"),

    ARRAY((byte) '*', "array");


    private byte type;
    private String name;

    RedisDataEnum(byte type, String name) {
        this.type = type;
        this.name = name;
    }

    public static RedisDataEnum getInstance(byte type) {
        for (RedisDataEnum dataEnum : RedisDataEnum.values()) {
            if (dataEnum.type == type) {
                return dataEnum;
            }
        }

        throw new RedisException("redis data type not exist for byte code:" + (char) type);
    }

    @Override
    public String toString() {
        return "RedisDataEnum{" +
                "name='" + name + '\'' +
                '}';
    }

    public byte getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
