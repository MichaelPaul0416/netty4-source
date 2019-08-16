package com.wq.client.tools;

import com.wq.client.tools.core.FutureResult;
import com.wq.client.tools.core.SyncClient;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: wangqiang20995
 * @Date:2019/5/30
 * @Description:
 * @Resource:
 */
public class DefaultTcpClient extends ChannelInboundHandlerAdapter implements SyncClient {


    private Logger logger = LoggerFactory.getLogger(getClass());

    private Map<Channel, FutureResult> channelLocalMap = new ConcurrentHashMap<>();

    private Map<Channel, AtomicInteger> totalMessage = new ConcurrentHashMap<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.info("receive : {}", msg);
        Channel channel = ctx.channel();
        if (!(msg instanceof String)) {
            ctx.fireChannelRead(msg);
        }

        this.addResult(channel, msg);

    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        logger.debug("clear cache for channel[{}]", ctx.channel());
        this.channelLocalMap.remove(ctx.channel());
        this.totalMessage.remove(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(CLIENT_EXCEPTION + cause.getMessage(), cause);
//        this.channelLocalMap.remove(ctx.channel());
//        this.totalMessage.remove(ctx.channel());
//        ctx.channel().close();

        this.addResult(ctx.channel(), "client exception:" + cause.getMessage());
    }

    @Override
    public FutureResult getResult(Channel channel) {
        FutureResult result = this.channelLocalMap.get(channel);
        return result;
    }

    @Override
    public void initResultReceiver(Channel channel, FutureResult<?> result) {
        this.channelLocalMap.putIfAbsent(channel, result);
        this.totalMessage.putIfAbsent(channel, new AtomicInteger(result.getNeedReceive()));
    }

    @Override
    public boolean cacheClear(Channel channel) {
        return this.channelLocalMap.get(channel) == null && this.totalMessage.get(channel) == null;
    }

    @Override
    public void addResult(Channel channel, Object object) {
        FutureResult<Object> futureResult = this.channelLocalMap.get(channel);
        if (futureResult == null) {
            throw new IllegalStateException("UnExpect exception");
        }

        if (ERROR_MESSAGE.equals(object)) {
            futureResult.setStatus(FutureResult.SERVER_ERROR);
            futureResult.set(object);
            return;
        }

        if (futureResult.get() instanceof List) {
            List list = (List) futureResult.get();
            list.add(object);
        } else {
            futureResult.set(object);
        }

        AtomicInteger integer = this.totalMessage.get(channel);
        int left = integer.get() > 0 ? integer.decrementAndGet() : 0;
        int total = futureResult.getNeedReceive();

        logger.info("已接受[" + (total - left) + "]/待接收[" + left + "]/总共需要接收[" + total + "]");

    }
}
