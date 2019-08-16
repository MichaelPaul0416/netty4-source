package com.wq.client.tools.core;

/**
 * @Author: wangqiang20995
 * @Date:2019/5/30
 * @Description:
 * @Resource:
 */
public class FutureResult<T> {

    private volatile T t;
    private long start;
    private final int needReceive;

    //OK->0,ERROR->1
    private byte status;

    public static final byte SERVER_OK = 0;

    public static final byte SERVER_ERROR = 1;


    @Override
    public String toString() {
        return "FutureResult{" +
                "t=" + t +
                ", start=" + start +
                ", needReceive=" + needReceive +
                '}';
    }

    public FutureResult(long start, int needReceive){
        this.start = start;
        this.needReceive = needReceive;
    }

    public T get(){
        return this.t;
    }

    public void set(T t){
        this.t = t;
    }

    public long startTime(){
        return this.start;
    }

    public int getNeedReceive() {
        return needReceive;
    }

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }
}
