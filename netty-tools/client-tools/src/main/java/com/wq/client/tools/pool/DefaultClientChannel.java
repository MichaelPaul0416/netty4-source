package com.wq.client.tools.pool;

import com.wq.netty.core.pool.CallBackProcessor;
import com.wq.netty.core.pool.ClientChannel;
import com.wq.netty.core.pool.PoolParam;
import com.wq.netty.core.pool.PooledClient;
import com.wq.client.tools.core.proto.StringProtocol;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: wangqiang20995
 * @Date:2019/8/16
 * @Description:
 * @Resource:
 */
public class DefaultClientChannel implements ClientChannel<StringProtocol> {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private PooledClient pooledClient;

    public DefaultClientChannel(int size,int retry){
        this.pooledClient = new TcpStringPooledClient(size, retry);
        this.pooledClient.bindBase("localhost",8080);
    }

    @Override
    public boolean submit(Object param, CallBackProcessor<StringProtocol> processor) {
        Channel channel = pooledClient.selectChannel();
        pooledClient.submitCallback(processor);

        PoolParam<String> poolParam = new PoolParam<>();
        poolParam.setUuid(processor.uuid());
        if(param instanceof String) {
            poolParam.setMessage((String) param);
        }else {
            poolParam.setMessage(String.valueOf(param));
        }
        channel.writeAndFlush(poolParam).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                logger.info("消息发送成功...等待回调[{}]",processor.uuid());
            }
        });
        return true;
    }
}
