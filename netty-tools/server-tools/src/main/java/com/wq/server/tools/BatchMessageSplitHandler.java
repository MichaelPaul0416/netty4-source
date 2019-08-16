package com.wq.server.tools;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: wangqiang20995
 * @Date:2019/5/30
 * @Description:
 * @Resource:
 */
public class BatchMessageSplitHandler extends SimpleChannelInboundHandler<String> {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
        if (StringUtil.isNullOrEmpty(s)) {
            throw new NullPointerException("empty input string...");
        }


        logger.info("single message:" + s);

        String functionNo = parseRequest(s);
        channelHandlerContext.writeAndFlush("received message,and functionNo:" + functionNo + "\n");
    }

    private String parseRequest(String request) {
        if (!request.contains("<FunctionNo>")) {
            return "illegal request...";
        }

        int start = request.indexOf("<FunctionNo>") + 12;
        return request.substring(start, start + 4);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage(), cause);
        ctx.writeAndFlush("some error in server...");
    }
}
