package io.netty.example.paul.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

//如果需要在ChannelInitializer#initChannel方法中new这个对象,那么就需要加这个注解
@ChannelHandler.Sharable
public class TimeServerHandler extends SimpleChannelInboundHandler<String> {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        logger.info("receive:{}", msg);

        String time = new SimpleDateFormat(TIME_FORMAT).format(new Date());

        ctx.writeAndFlush(msg + time + "$");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        ctx.writeAndFlush("server error:" + cause.getMessage() + "$");
    }
}
