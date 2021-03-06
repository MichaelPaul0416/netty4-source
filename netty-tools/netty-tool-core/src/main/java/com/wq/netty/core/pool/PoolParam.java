package com.wq.netty.core.pool;

import java.io.Serializable;

/**
 * @Author: wangqiang20995
 * @Date:2019/8/16
 * @Description:
 * @Resource:
 */
public class PoolParam<T extends Serializable> {
    private String uuid;

    private int channelId;

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    @Override
    public String toString() {
        return "PoolParam{" +
                "uuid='" + uuid + '\'' +
                ", channelId=" + channelId +
                ", message=" + message +
                '}';
    }

    private T message;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public T getMessage() {
        return message;
    }

    public void setMessage(T message) {
        this.message = message;
    }

}
