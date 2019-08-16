package com.wq.client.tools;

import com.wq.netty.core.pool.CallBackProcessor;
import com.wq.netty.core.pool.ClientChannel;
import com.wq.client.tools.core.proto.StringProtocol;
import com.wq.client.tools.pool.DefaultClientChannel;
import com.wq.client.tools.pool.DefaultStringCallbackSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: wangqiang20995
 * @Date:2019/8/16
 * @Description:
 * @Resource:
 */
public class TcpClientFromPool {

    private static final Logger logger = LoggerFactory.getLogger(TcpClientFromPool.class);

    public static void main(String[] args){
        ClientChannel<StringProtocol> clientChannel =
                new DefaultClientChannel(5,3);
        clientChannel.submit("helloworld$",new CallBackProcessor<StringProtocol>() {
            private String uuid = DefaultStringCallbackSelector.generatorUuid();

            @Override
            public void process(StringProtocol param) {
                logger.info("callback message : {}",(String)param.getProtocolBody());
            }

            @Override
            public String uuid() {
                return uuid;
            }
        });
    }
}
