package com.wq.netty.core.pool;

import io.netty.channel.ChannelFutureListener;

import java.util.List;

/**
 * @Author: wangqiang20995
 * @Date:2019/8/19
 * @Description:
 * @Resource:
 */
public abstract class RequestTask {

    protected String taskId;
    protected int channelId;

    public RequestTask(String taskId,int channelId){
        this.taskId = taskId;
        this.channelId = channelId;
    }
    public int getChannelId(){
        return this.channelId;
    }

    /**
     * 返回taskId
     * @return
     */
    protected abstract String taskId();

    protected abstract Object requestParam();

    protected abstract List<ChannelFutureListener> futureListeners();

}
