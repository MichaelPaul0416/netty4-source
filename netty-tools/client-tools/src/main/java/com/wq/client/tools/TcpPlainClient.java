package com.wq.client.tools;

import com.wq.client.tools.core.CauseContext;
import com.wq.client.tools.core.FutureResult;
import com.wq.client.tools.core.ResourceLoader;
import com.wq.client.tools.core.SyncClient;
import com.wq.netty.core.BatchXmlDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author: wangqiang20995
 * @Date:2019/5/30
 * @Description:
 * @Resource:
 */
public class TcpPlainClient {
    /**
     * netty的client真正发送数据的线程和真正接受数据的线程一定是同一个
     */

    private static final Logger logger = LoggerFactory.getLogger(TcpPlainClient.class);

    private final Bootstrap bootstrap;

    private final String server;

    private final int port;

    private final EventLoopGroup pool;

    private ChannelFuture future;

    private CauseContext throwable;

    private final SyncClient client;

    private final long expireTime;

    private ResourceLoader<String> resourceLoader;

    private String charset;

    public TcpPlainClient(String server, int port, long expireTime, String charset) {
        this.server = server;
        this.port = port;
        this.pool = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        this.throwable = new CauseContext(null);
        this.client = new DefaultTcpClient();
        this.init((ChannelHandler) client);
        this.expireTime = expireTime;
        this.charset = charset;
        this.resourceLoader = new DefaultResourceLoader(this.charset);
    }

    private void init(ChannelHandler client) {
        this.bootstrap.group(pool)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
//                                .addLast(new LineBasedFrameDecoder(100))
//                                .addLast(new BatchXmlDecoder("GB2312", 4))
                                .addLast(new StringDecoder())
                                .addLast(new StringEncoder(Charset.forName("GB2312")))
                                .addLast(client);
                    }
                });
    }

    public void start() {
        try {
            this.future = this.bootstrap.connect(this.server, this.port).sync();

        } catch (InterruptedException e) {
            logger.error("客户端等待连接异常，即将退出");
            this.throwable.throwable(e);
            return;
        } finally {
            if (!this.throwable.empty()) {
                this.pool.shutdownGracefully();
                CauseContext.printException(this.throwable);
            }
        }
    }

    public Object sendSync(String info, int waitNumbers) {
        if (this.future == null) {
            logger.warn("客户端可能尚未构建与服务端的连接，或者客户端初始化失败...");
            return null;
        }

        if (waitNumbers <= 0) {
            throw new IllegalArgumentException("同步等待响应信息数目需要>=0,现在传入[" + waitNumbers + "]");
        }

        ChannelFuture channelFuture;
        FutureResult<List<String>> futureResult = new FutureResult<>(System.currentTimeMillis(), waitNumbers);
        futureResult.set(new ArrayList<>());
        Channel currentChannel = this.future.channel();
        TcpPlainClient.this.client.initResultReceiver(currentChannel, futureResult);
        String temp = info;//带有占位符的字符串

        for (int i = 0; i < waitNumbers; i++) {
            info = temp.replaceAll("\\$", "1000-" + i);
            channelFuture = this.future.channel().writeAndFlush(info);
            int finalI = i;
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    logger.info("send message[batch--" + finalI + "] success...");
                }
            });

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        //同步等待结果
        FutureResult<List<String>> result = null;
        while (true) {
            if (result != null && result.getNeedReceive() == 0) {
                this.future.channel().close();
                return result.get();
            }
            result = this.client.getResult(this.future.channel());
            if (result == null) {
                //走进这里的原因可能是发生异常或者channel断开连接
                logger.warn("channel[{}]已经被关闭且移除", this.future.channel());
                return null;
            }

            //超时
            long start = result.startTime();
            if (System.currentTimeMillis() - start > this.expireTime) {
                logger.error("wait for result time out....");
                CauseContext.printException(this.throwable);
                return null;
            }

            if (FutureResult.SERVER_ERROR == result.getStatus()) {
                logger.error("server error...");
                return futureResult.get();
            }
            List<String> real = result.get();
            if (real != null && real.size() > 0) {
                if(real.size() == waitNumbers){
                    this.future.channel().close();
                    return real;
                }

                if(real.get(real.size() - 1).startsWith(SyncClient.CLIENT_EXCEPTION)){
                    this.future.channel().close();
                    return real;
                }
            }

            //休眠，等待下次
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                this.throwable.throwable(e);
            }

        }

    }

    public void closeGracefully() {
        try {
            this.future.channel().closeFuture();
        } catch (Exception e) {
            logger.error("客户端异常，即将退出...");
            this.throwable.throwable(e);
        } finally {
            if (!this.throwable.empty()) {
                CauseContext.printException(this.throwable);
            }

            this.pool.shutdownGracefully();
        }
    }

    public String loadResource(String fullName) {
        return this.resourceLoader.loadSourceFromClassPath(fullName);
    }


    public static void main(String[] args) {
        TcpPlainClient client = new TcpPlainClient("localhost", 8080, 10000 * 1000, "UTF-8");
        client.start();
        String xml = client.loadResource("request.txt");
        Object result = client.sendSync(xml, 1);
        logger.info("result-->{}", result);

        client.closeGracefully();
    }
}
