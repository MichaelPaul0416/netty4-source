package io.netty.example.paul.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpPlainClient {
    private static final Logger logger = LoggerFactory.getLogger(TcpPlainClient.class);

    private Bootstrap bootstrap;
    private EventLoopGroup workers;
    private int localPort;
    private Channel channel;
    private volatile boolean connected;
    private final char split;

    public TcpPlainClient(int num, int port, char split) {
        this.workers = new NioEventLoopGroup(num);
        this.localPort = port;
        this.split = split;
        this.bootstrap = new Bootstrap();

        init();
    }

    private void init() {
        this.bootstrap.group(workers)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024, Unpooled.wrappedBuffer(new byte[]{(byte) split})))
                                .addLast(new StringDecoder())
                                .addLast(new StringEncoder())
                                .addLast(new TcpClientHandler());
                    }
                });
        try {
            this.channel =
                    this.bootstrap.connect("localhost", localPort).sync().channel();
            this.connected = true;
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void sendMessage(String message) {
        if (!this.connected) {
            logger.error("connected not completed");
            return;
        }

        this.channel.writeAndFlush(message + this.split);
    }

    public void close() {
        try {
            /**
             * channel.close():马上关闭通道
             * channel.closeFuture().sync():等待未来某个时机关闭通道,自己不会主动关闭,自己会阻塞在sync上,等待其他线程关闭通道后自己就会返回
             */
            this.channel.closeFuture().sync();
            logger.info("close successfully...");
        } catch (InterruptedException e) {
            logger.error(e.getMessage(),e);
        } finally {
            this.workers.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        TcpPlainClient client = new TcpPlainClient(10, 8080, '$');
        client.sendMessage("time");
        client.close();

    }

}
