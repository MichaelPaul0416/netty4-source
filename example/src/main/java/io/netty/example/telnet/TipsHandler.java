package io.netty.example.telnet;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @Author: wangqiang20995
 * @Date:2019/8/6
 * @Description:
 * @Resource:
 */
//@ChannelHandler.Sharable
public class TipsHandler extends SimpleChannelInboundHandler<String> {

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        System.out.println("ChannelHandler[" + this.getClass().getName() + "] added successfully");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.printf("msg:%s\n", msg);
        //通知给下一个
        ctx.fireChannelRead(msg);
    }
}
