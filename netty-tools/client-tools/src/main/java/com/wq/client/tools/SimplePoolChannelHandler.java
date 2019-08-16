package com.wq.client.tools;

import com.wq.netty.core.pool.CallBackProcessor;
import com.wq.netty.core.pool.ProtocolCallbackSelector;
import com.wq.client.tools.core.proto.StringProtocol;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: wangqiang20995
 * @Date:2019/8/16
 * @Description:
 * @Resource:
 */
@ChannelHandler.Sharable
public class SimplePoolChannelHandler extends SimpleChannelInboundHandler<String> {

    private final ProtocolCallbackSelector<StringProtocol> selector;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String headSplit = "#";

    public SimplePoolChannelHandler(ProtocolCallbackSelector<StringProtocol> selector){
        this.selector = selector;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
        String uuid = chooseUuid(s);
        if(StringUtil.isNullOrEmpty(uuid)){
            return;
        }

        CallBackProcessor<StringProtocol> callBackProcessor = this.selector.select(uuid);
        StringProtocol protocol = new StringProtocol(chooseBody(s));
        callBackProcessor.process(protocol);
    }

    private String chooseBody(String message){
        if (message.contains(headSplit)){
            return message.substring(message.indexOf(headSplit) + 1);
        }

        return message;
    }

    private String chooseUuid(String message){
        if(!message.contains(headSplit)){
            logger.warn("无效的消息，没有消息头uuid，该条消息将被抛弃");
            return null;
        }

        String uuid = message.substring(0,message.indexOf(headSplit));
        return uuid;
    }
}
