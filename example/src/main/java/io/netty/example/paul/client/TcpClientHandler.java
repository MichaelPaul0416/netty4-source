package io.netty.example.paul.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpClientHandler extends SimpleChannelInboundHandler<String> {

    private Logger logger = LoggerFactory.getLogger(getClass());


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        logger.info("receive server message:{}",msg);
        ctx.channel().close();
    }
}
