package com.wq.client.tools;

import com.wq.netty.core.pool.CallBackProcessor;
import com.wq.netty.core.pool.ClientChannel;
import com.wq.client.tools.core.proto.StringProtocol;
import com.wq.client.tools.pool.DefaultClientChannel;
import com.wq.client.tools.pool.DefaultStringCallbackSelector;
import com.wq.netty.core.pool.ConnectedFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * @Author: wangqiang20995
 * @Date:2019/8/16
 * @Description:
 * @Resource:
 */
public class TcpClientFromPool {

    private static final Logger logger = LoggerFactory.getLogger(TcpClientFromPool.class);

    public static void main(String[] args) {
        ClientChannel<StringProtocol> clientChannel =
                new DefaultClientChannel(3, 3);
        CountDownLatch watch = new CountDownLatch(5);
        try {
//            singleThreadMultiTimesConnect(clientChannel, watch);
            multiThreadMultiTimesConnect(clientChannel,watch);
            watch.await();
        }catch (ConnectedFailedException e){
            logger.error("connect to server failed,and please check the server address and the network connection");
        } catch (InterruptedException e) {
            logger.error("等待任务执行完毕异常...[{}]",e.getMessage(),e);
        }finally {
            clientChannel.shutdownGracefully();
        }

    }

    private static void multiThreadMultiTimesConnect(ClientChannel<StringProtocol> clientChannel,CountDownLatch countDownLatch){
        for(int i=0;i<5;i++){
            int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    singleTask(clientChannel,countDownLatch, finalI);
                }
            }).start();
        }
    }

    /**
     * 单线程循环多次去，使用连接池去连接
     * @param clientChannel
     * @param watch
     */
    private static void singleThreadMultiTimesConnect(ClientChannel<StringProtocol> clientChannel, CountDownLatch watch) {
        for (int i = 0; i < 5; i++) {
            singleTask(clientChannel, watch, i);
        }
    }

    private static void singleTask(ClientChannel<StringProtocol> clientChannel, CountDownLatch watch, int i) {
        clientChannel.submit("HelloWold-" + i + "$", new CallBackProcessor<StringProtocol>() {
            private String uuid = DefaultStringCallbackSelector.generatorUuid();

            @Override
            public void process(StringProtocol param) {
                logger.info("callback message : {}", (String) param.getProtocolBody());
                watch.countDown();
            }

            @Override
            public String uuid() {
                return uuid;
            }
        });
    }
}
