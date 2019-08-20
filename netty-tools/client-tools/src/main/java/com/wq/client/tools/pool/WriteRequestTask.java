package com.wq.client.tools.pool;

import com.wq.netty.core.pool.RequestTask;
import io.netty.channel.ChannelFutureListener;

import java.util.Arrays;
import java.util.List;

/**
 * @Author: wangqiang20995
 * @Date:2019/8/19
 * @Description:
 * @Resource:
 */
public class WriteRequestTask extends RequestTask {

    private List<ChannelFutureListener> futureListeners;

    private Object sendParam;

    public WriteRequestTask(String taskId, int channelId, Object param,List<ChannelFutureListener> futureListener) {
        super(taskId, channelId);
        this.sendParam = param;

        if (futureListener != null) {
            this.futureListeners = futureListener;
        }else {
            this.futureListeners = null;
        }
    }


    @Override
    protected String taskId() {
        return taskId;
    }

    @Override
    protected Object requestParam() {
        return this.sendParam;
    }

    @Override
    protected List<ChannelFutureListener> futureListeners() {
        return this.futureListeners;
    }

    @Override
    public String toString() {
        return "WriteRequestTask{" +
                "sendParam=" + sendParam +
                ", taskId='" + taskId + '\'' +
                ", channelId=" + channelId +
                "} " + super.toString();
    }
}
