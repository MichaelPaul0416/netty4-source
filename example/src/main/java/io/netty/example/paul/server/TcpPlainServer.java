package io.netty.example.paul.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpPlainServer {

    private ServerBootstrap serverBootstrap;

    private NioEventLoopGroup boss;

    private NioEventLoopGroup worker;

    private ChannelFuture startFuture;

    private char spilter;

    private Logger logger = LoggerFactory.getLogger(getClass());


    public TcpPlainServer(int boss, int worker,char spliter) {
        this.serverBootstrap = new ServerBootstrap();
        if (boss <= 0) {
            throw new IllegalArgumentException("boss must > 0");
        }

        if (worker <= 0){
            throw new IllegalArgumentException("worker must > 0");
        }

        this.boss = new NioEventLoopGroup(boss);
        this.worker = new NioEventLoopGroup(worker);
        this.spilter = spliter;
    }

    public void init() {

        this.serverBootstrap.group(boss,worker)
                .handler(new LoggingHandler(LogLevel.INFO))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{(byte) spilter});
                        ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024,buf))
                                .addLast(new StringDecoder())
                                .addLast(new StringEncoder())
                                .addLast(new TimeServerHandler());
                    }
                });

    }

    public void start(int port){
        try {
            this.startFuture  = this.serverBootstrap.bind(port).sync();
            logger.info("server started successfully");
        } catch (InterruptedException e) {
            logger.error(e.getMessage(),e);
        }
    }

    public void shutdown(){
        if (this.startFuture != null){
            this.startFuture.channel().close();
        }
    }

    public static void main(String[] args){
        TcpPlainServer server = new TcpPlainServer(8,16,'$');
        server.init();
        server.start(8080);
    }
}
