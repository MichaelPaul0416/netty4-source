package com.wq.netty.bytebuf.wrapper.core;

public class RedisException extends RuntimeException {

    public RedisException(String msg) {
        super(msg);
    }

    public RedisException(Throwable e) {
        super("redis exception:" + e.getLocalizedMessage(), e);
    }

    public RedisException(String msg,Throwable e){
        super(msg,e);
    }

    public static final String TIMEOUT_RESULT = "wait time out for result";
}
