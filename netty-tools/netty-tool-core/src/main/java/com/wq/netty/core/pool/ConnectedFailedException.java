package com.wq.netty.core.pool;

/**
 * @Author: wangqiang20995
 * @Date:2019/8/19
 * @Description:
 * @Resource:
 */
public class ConnectedFailedException extends RuntimeException {

    public ConnectedFailedException(String msg){
        super(msg);
    }

    public ConnectedFailedException(String msg,Throwable t){
        super(msg,t);
    }

    public ConnectedFailedException(Throwable t){
        super(t.getMessage(),t);
    }
}
