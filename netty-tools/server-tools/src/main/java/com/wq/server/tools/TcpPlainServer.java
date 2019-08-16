package com.wq.server.tools;

import com.wq.netty.core.BatchXmlDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;

/**
 * @Author: wangqiang20995
 * @Date:2019/5/30
 * @Description:
 * @Resource:
 */
public class TcpPlainServer implements CommonServer {

    private static Logger logger = LoggerFactory.getLogger(TcpPlainServer.class);

    private final ServerBootstrap serverBootstrap;

    private NioEventLoopGroup boss;

    private NioEventLoopGroup worker;

    private ChannelFuture channelFuture;

    private final String host;
    private final int port;

    public TcpPlainServer(String ip, int port) {
        this.serverBootstrap = new ServerBootstrap();
        this.host = ip;
        this.port = port;
        this.boss = new NioEventLoopGroup();
        this.worker = new NioEventLoopGroup();
        init(boss, worker);
    }

    @Override
    public void startServer() {
        try{
            this.channelFuture = this.serverBootstrap.bind(this.host,this.port).sync();
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    logger.info("tcp plain server start success...");
                }
            });
            this.channelFuture.channel().closeFuture().sync();
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }finally {
            this.boss.shutdownGracefully();
            this.worker.shutdownGracefully();
        }
    }

    @Override
    public void shutdown() {

    }


    public static void main(String[] args) throws ParseException {
        TcpPlainServer server = new TcpPlainServer("localhost",8080);
        server.startServer();
        logger.info("ok");
    }

    private void init(NioEventLoopGroup boss, NioEventLoopGroup worker) {
        this.serverBootstrap.group(boss, worker)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline()
//                                .addLast(new StringDecoder())
                                .addLast(new BatchXmlDecoder("GB2312",-1))
                                .addLast(new StringEncoder())
                                .addLast(new BatchMessageSplitHandler());
                    }
                });
    }
}
