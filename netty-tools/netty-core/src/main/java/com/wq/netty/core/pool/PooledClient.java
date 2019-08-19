package com.wq.netty.core.pool;


import com.wq.netty.core.proto.AbstractProtocol;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

/**
 * @Author: wangqiang20995
 * @Date:2019/8/16
 * @Description:池化的client
 * @Resource:
 */
public interface PooledClient {

    /**
     * 选一个可用的Channel
     *
     * @return
     */
    ChannelWrapper selectChannel();

    /**
     * 初始化Pool，并且初始化出几个核心的Channel
     *
     * @param address
     * @param port
     */
    void bindBase(String address, int port);

    /**
     * 关闭core的Channel，同时关闭池子
     *
     * @return
     */
    boolean shutdownNow();

    void submitCallback(CallBackProcessor<? extends AbstractProtocol> processor);

    void asynWriteChannel(Channel channel, PoolParam poolParam, ChannelFutureListener... listeners);
}
