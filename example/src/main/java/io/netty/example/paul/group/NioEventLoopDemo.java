package io.netty.example.paul.group;

import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: wangqiang20995
 * @Date:2019/8/12
 * @Description:
 * @Resource:
 */
public class NioEventLoopDemo {

    public static void main(String[] args){
        //EventLoopGroup 可以简单理解为就是jdk中的线程池，只不过它添加了一些额外的功能
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(3);

        AtomicBoolean stop = new AtomicBoolean(false);
        eventLoopGroup.execute(() -> {
            System.out.println("hello");
            stop.set(true);
        });

        while (!stop.get()){

        }

        eventLoopGroup.shutdownGracefully();
    }
}
