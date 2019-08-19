package com.wq.client.tools.pool;

import com.wq.netty.core.pool.*;
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
public class DefaultClientChannel implements ClientChannel<StringProtocol>{

    private Logger logger = LoggerFactory.getLogger(getClass());

    private PooledClient pooledClient;

    public DefaultClientChannel(int size,int retry){
        this.pooledClient = new TcpStringPooledClient(size, retry);
        this.pooledClient.bindBase("localhost",8080);
    }

    /**
     * this method will throw ConnectedFailedException when connect to server failed in several times
     * @param param
     * @param processor
     * @return
     */
    @Override
    public boolean submit(Object param, CallBackProcessor<StringProtocol> processor) {
        pooledClient.submitCallback(processor);

        PoolParam<String> poolParam = new PoolParam<>();
        poolParam.setUuid(processor.uuid());
        if(param instanceof String) {
            poolParam.setMessage((String) param);
        }else {
            poolParam.setMessage(String.valueOf(param));
        }

        //返回null的话就说明将获取channel的请求异步提交了，然后指定了那个channel
        ChannelWrapper channel = pooledClient.selectChannel();
        poolParam.setChannelId(channel.getChannelId());
        if(channel == null){
            //说明分配到当前index的channel正在初始化，还没结束，那么此时就是异步写入数据
            logger.info("channel initializing... and param[{}] submit for aSynchronized calling...",poolParam);
        }

        pooledClient.asynWriteChannel(channel.getChannel(),poolParam,new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                logger.info("消息发送成功...等待回调[{}]",processor.uuid());
            }
        });
        return true;
    }

    @Override
    public boolean shutdownGracefully() {
        return this.pooledClient.shutdownNow();
    }
}
