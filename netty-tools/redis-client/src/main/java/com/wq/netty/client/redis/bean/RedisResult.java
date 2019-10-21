package com.wq.netty.client.redis.bean;

public class RedisResult<T> {
    private volatile T result;

    private String status;

    public boolean done(){
        return this.result != null;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
