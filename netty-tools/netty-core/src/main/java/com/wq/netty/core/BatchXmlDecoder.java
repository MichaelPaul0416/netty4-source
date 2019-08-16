package com.wq.netty.core;

import com.wq.netty.utils.ByteUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: wangqiang20995
 * @Date:2019/5/31
 * @Description:
 * @Resource:
 */
public class BatchXmlDecoder extends ByteToMessageDecoder {
    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=";

    private static final String XML_END = "</MsgText>";

    private final String charset;

    private final int stripBytes;

    private final ThreadLocal<Message> stripHeader = new ThreadLocal<>();

    public BatchXmlDecoder(String charset, int stripBytes) {
        this.charset = charset;
        this.stripBytes = stripBytes;
    }

    private static class Holder<T> {
        private volatile T data;

        public void pushData(T t) {
            this.data = t;
        }

        public T getData() {
            return this.data;
        }

        public Holder() {
        }

        public Holder(T data) {
            this.data = data;
        }
    }

    private static class Message {
        private final int total;
        private int received;

        public Message(int total) {
            this.total = total;
        }

        public void setReceived(int received) {
            this.received += received;
        }

        public boolean done() {
            return this.total == this.received;
        }
    }

    //跟channel绑定，所以即便是多线程的情况下，channel不一样，暨对端不一样，也不会造成Holder线程不安全，因此此时肯定是一个Nio线程维护一个对端
    private final Map<Channel, Holder<String>> unPerfectMessage = new ConcurrentHashMap<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        Holder<String> holder = this.unPerfectMessage.get(ctx.channel());
        if (holder != null) {
            holder.pushData(null);
        }
        holder = null;

        this.unPerfectMessage.remove(ctx.channel());
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (this.stripBytes > 0 && (this.stripHeader.get() == null || this.stripHeader.get().done())) {
            byte[] strip = new byte[this.stripBytes];
            in.readBytes(strip);
            int length = ByteUtils.signedByteToInt(strip);
            logger.debug("complete xml length:{}", length);
            Message message = new Message(length);
            this.stripHeader.set(message);
        }

        int total = in.readableBytes();
        this.stripHeader.get().setReceived(total);

        //半包中文
        byte[] data = new byte[total];
        in.readBytes(data);

        String message = new String(data, this.charset);


        /**
         * 报文不完整有三种情况
         * 1.少头
         * 2.少尾
         * 3.少头和尾
         */

        //完整的xml报文开头,那么holder中必然不可能有东西
        if (message.startsWith(XML_HEADER)) {
            assert this.unPerfectMessage.get(ctx.channel()) == null;
            if (completeXmlEnding(message)) {
                //这次的报文都是完整的，没有半包的情况
                parseCompleteXml(message, out);
            } else {
                //截取最后半段不完整的报文，放入holder
                parseUnCompleteEndingXml(message, out, ctx.channel());
            }

        } else {
            //非完整xml报文头开头，所以根据xml尾判断
            if (message.contains(XML_END)) {
                Holder<String> holder = this.unPerfectMessage.get(ctx.channel());
                //既然这了遇到了完整的xml结尾，那么之前缓存的xml报文中肯定是有完整的xml头,并且这个xml结尾肯定是这个报文中的第一个xml结尾
                String lastXml = holder.getData() + message;
                parseOneOrMoreXml(lastXml, out, ctx.channel());

            } else {
                //头不完整，尾也没有[没有尾部或者尾部不完整]，继续压入Holder堆栈
                Holder<String> holder = this.unPerfectMessage.get(ctx.channel());
                if (holder == null) {
                    throw new IllegalStateException("报文接受异常...");
                }
                synchronized (this.unPerfectMessage) {
                    recordMessageLength(message, false);
                    holder.pushData(holder.getData() + message);
                }

                String xmlInfo = holder.getData();
                if (xmlInfo.contains(XML_HEADER) && xmlInfo.contains(XML_END)) {
                    //可能xml开头包含半个尾部，所以要结合上一次的报文，判断，是否能组成一个完整的报文
                    parseOneOrMoreXml(xmlInfo, out, ctx.channel());
                }
            }

        }

    }

    /**
     * @author:wangqiang20995
     * @datetime:2019/5/31 13:54
     * @param: [xml, out, channel]
     * @description:以完整的xml开头，并且至少包含一条完整的xml信息
     * @return: void
     **/
    private void parseOneOrMoreXml(String xml, List<Object> out, Channel channel) {
        while (xml.contains(XML_END)) {
            int start = 0;
            if (xml.indexOf(XML_HEADER) != 0) {
                start = this.stripBytes;
            }
            int end = xml.indexOf(XML_END);
            assert end != -1;
            end += XML_END.length();
            String full = xml.substring(start, end);
            logger.debug("full xml:[{}]", full);
            out.add(full);

            if (end == xml.length()) {
                this.stripHeader.remove();
                return;
            }

            xml = xml.substring(end);
        }
//        if (!StringUtil.isNullOrEmpty(xml)) {
        Holder<String> holder = this.unPerfectMessage.get(channel);
        if (holder == null) {
            holder = new Holder<>();
        }

        holder.pushData(xml);
        recordMessageLength(xml, true);
//        }

    }

    /**
     * @author:wangqiang20995
     * @datetime:2019/5/31 11:33
     * @param: [xml, out]
     * @description:解析xml结尾不完整的报文
     * @return: void
     **/
    private void parseUnCompleteEndingXml(String xml, List<Object> out, Channel channel) {
        if (xml.lastIndexOf(XML_HEADER) == 0) {
            logger.debug("整个包中的报文都是不完整的xml...[{}]", xml);
            Holder<String> holder = new Holder<>();
            holder.pushData(xml);
            this.unPerfectMessage.putIfAbsent(channel, holder);

            recordMessageLength(xml, false);
            return;
        }

        //至少包含一段完整的xml
        String message = buildAtLeastOneCompleteXml(out, xml, false);

        Holder<String> holder = new Holder<>(message);
        this.unPerfectMessage.putIfAbsent(channel, holder);

    }

    private void recordMessageLength(String xml, boolean newMessage) {
        byte[] data;
        try {
            data = xml.getBytes(this.charset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (newMessage) {
            byte[] header = new byte[this.stripBytes];
            System.arraycopy(data, 0, header, 0, this.stripBytes);
            int len = ByteUtils.signedByteToInt(header);
            Message message = new Message(len);
            message.setReceived(data.length - this.stripBytes);
            return;
        }
        Message message = this.stripHeader.get();
        assert message != null;
        message.setReceived(data.length);
    }

    /**
     * @author:wangqiang20995
     * @datetime:2019/6/18 15:47
     * @param: [out, message]
     * @description:解析这个包中完整的xml部分
     * @return: java.lang.String
     **/
    private String buildAtLeastOneCompleteXml(List<Object> out, String message, boolean complete) {

        while (message.contains(XML_END)) {
            int start = 0;
            int end = message.indexOf(XML_END) + XML_END.length();
            String full = message.substring(start, end);
            logger.debug("xml完整报文[{}]", full);
            out.add(full);
            //没有后续报文
            if (end == message.length()) {
                return "";
            }

            message = message.substring(end);//这个报文可能是带有长度头的完整报文，也可能是带有长度头的不完整报文
            //不是完整的批报文的话，那就留给后面解决
            if (complete) {
                message = message.substring(this.stripBytes);//直接跳过长度头
            }
        }

        //等待接收下一批次的半包
        recordMessageLength(message, true);
        return message;
    }

    /**
     * @author:wangqiang20995
     * @datetime:2019/5/31 11:26
     * @param: [xml, out]
     * @description:解析完整的xml报文
     * @return: void
     **/
    private void parseCompleteXml(String xml, List<Object> out) {
        if (xml.indexOf(XML_END) == xml.lastIndexOf(XML_END)) {
            out.add(xml);
            logger.debug("这个xml报文是一个完整的xml报文[{}]", xml);
            return;
        }

        //可能有多个完整的xml报文
        String message = xml;
        buildAtLeastOneCompleteXml(out, message, true);
    }

    /**
     * @author:wangqiang20995
     * @datetime:2019/5/31 11:22
     * @param: [message]
     * @description:判断当前这个message报文是否是以完整的xml结尾
     * @return: boolean
     **/
    private boolean completeXmlEnding(String message) {

        if (StringUtil.isNullOrEmpty(message)) {
            return false;
        }

        int length = message.length();

        if (message.lastIndexOf(XML_END) < 0) {
            return false;
        }

        return message.lastIndexOf(XML_END) + XML_END.length() == length;
    }
}
