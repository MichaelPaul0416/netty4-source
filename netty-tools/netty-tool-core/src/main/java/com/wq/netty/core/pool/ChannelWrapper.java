package com.wq.netty.core.pool;

import io.netty.channel.Channel;

/**
 * @Author: wangqiang20995
 * @Date:2019/8/19
 * @Description:
 * @Resource:
 */
public class ChannelWrapper {

    private Channel channel;

    private final int channelId;

    public ChannelWrapper(Channel channel,int channelId){
        this.channel = channel;
        this.channelId = channelId;
    }

    @Override
    public String toString() {
        return "ChannelWrapper{" +
                "channelId=" + channelId +
                '}';
    }

    public Channel getChannel() {
        return channel;
    }

    public int getChannelId() {
        return channelId;
    }
}
