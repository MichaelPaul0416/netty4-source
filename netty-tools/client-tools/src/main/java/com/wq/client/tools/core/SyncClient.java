package com.wq.client.tools.core;

import io.netty.channel.Channel;

/**
 * @Author: wangqiang20995
 * @Date:2019/5/31
 * @Description:
 * @Resource:
 */
public interface SyncClient {

    String ERROR_MESSAGE = "server error";

    String CLIENT_EXCEPTION = "client exception:";

    void initResultReceiver(Channel channel,FutureResult<?> result);

    <T> T getResult(Channel channel);

    boolean cacheClear(Channel channel);

    void addResult(Channel ch,Object object);
}
