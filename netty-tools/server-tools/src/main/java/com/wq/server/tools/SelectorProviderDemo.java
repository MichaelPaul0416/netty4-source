package com.wq.server.tools;

import java.io.IOException;
import java.nio.channels.spi.SelectorProvider;

public class SelectorProviderDemo {

    public static void main(String[] args){
        /**
         * Netty中的NioEventLoop中的成员变量Selector就是通过SelectorProvider#openSelector获得
         * 并且,每一个NioEventLoop中的Selector都是不同的
         * 然后由于NioSocketChannel是绑定到Selector上的,然后Selector又跟NioEventLoop绑定的
         * 所以NioSocketChannel就是跟NioEventLoop绑定,也就是说他们是多对一的关系
         * 再次发送消息和接受消息都是通过同一个Channel的,所以,在ChannelHandler中,发送消息和接受消息都是同一个线程
         */
        SelectorProvider provider = SelectorProvider.provider();

        try {
            for(int i=0;i<3;i++) {
                System.out.println(provider.openSelector());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
