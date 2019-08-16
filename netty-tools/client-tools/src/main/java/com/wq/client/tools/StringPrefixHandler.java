package com.wq.client.tools;

import com.wq.netty.core.pool.PoolParam;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: wangqiang20995
 * @Date:2019/8/16
 * @Description:
 * @Resource:
 */
public class StringPrefixHandler extends ChannelOutboundHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private final String split;

    public StringPrefixHandler(String split){
        this.split = split;
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof PoolParam) {
            PoolParam<String> poolParam = (PoolParam<String>) msg;
            String request = poolParam.getUuid() + split + poolParam.getMessage();
            super.write(ctx, request, promise);
        } else {
            super.write(ctx, msg, promise);
        }
    }
}
