package com.wq.netty.bytebuf.wrapper.codec;

import com.wq.netty.bytebuf.wrapper.core.RedisException;
import io.netty.buffer.ByteBuf;
import io.netty.util.internal.TypeParameterMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public abstract class AbstractRedisDataCodec<T extends Serializable> implements RedisDataTypeCodec {
    public static final byte PLAIN_STRING = (byte) '+';
    public static final byte EXCEPTION = (byte) '-';
    public static final byte NUMBER_LONG = (byte) ':';
    public static final byte FIX_LENGTH_STRING = (byte) '$';
    public static final byte ITEM_ARRAY = (byte) '*';

    public static final byte NEGATIVE_CODE = (byte) '-';

    public static final byte[] REDIS_CRLF = "\r\n".getBytes(StandardCharsets.US_ASCII);
    public static final byte REDIS_CR = (byte) '\r';

    public static final int FIX_MAX_LENGTH = 1 << 29;// 512MB

    public static final String CHARSET = "UTF-8";
    private final TypeParameterMatcher matcher;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public AbstractRedisDataCodec() {
        matcher = TypeParameterMatcher.find(this, AbstractRedisDataCodec.class, "T");
    }

    @Override
    public Object decode(ByteBuf byteBuf) {
        T result = doDecode(byteBuf);
        return result;
    }

    protected abstract T doDecode(ByteBuf buf);

    @Override
    public ByteBuf encode(Object t) {
        if (matcher.match(t)) {
            return doEncode((T) t);
        }

        throw new IllegalArgumentException("type not match,and now receive type:" + t.getClass());
    }

    protected abstract ByteBuf doEncode(T t);

    /**
     * 需要处理null的情况
     * 长度字段最大为512MB = 2^29 byte < 2^31 -1 ,所以可以直接用int接受，不会溢出
     *
     * @param buf
     * @return
     */
    protected String fixLengthDecode(ByteBuf buf) {
        ByteBuf byteBuf = buf;
        if (byteBuf.getByte(0) != FIX_LENGTH_STRING) {
            throwCodeHeadException(new String(new byte[]{FIX_LENGTH_STRING}));
        }

        // $-1\r\n --> null
        int start = ByteBufCodecUtil.findCodeIndex(byteBuf, NEGATIVE_CODE);// 先将长度作为字符串解析，如果有问题，再将长度作为字节数组byte[]解析
        byteBuf.readByte();// read and strip head
//
        if (start == 1) {
            logger.warn("empty fixed length string");
            buf.readBytes(ByteBufCodecUtil.findCodeIndex(byteBuf, REDIS_CR) + 1);
            return null;
        }

        // not null
        // read length field
        start = ByteBufCodecUtil.findCodeIndex(byteBuf, REDIS_CR);
        byte[] lengthByte = new byte[start - 1];
        byteBuf.readBytes(lengthByte);// length

        int length = fixStringLength(lengthByte,byteBuf,start);
        if(length < 0){
            return null;
        }

        byte[] msgBytes = new byte[length];
        byteBuf.readBytes(msgBytes);

        try {
            String msg = new String(msgBytes, CHARSET);
            byteBuf.readBytes(2);
            return msg;
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getLocalizedMessage(), e);
        }

        return null;
    }

    private int fixStringLength(byte[] lengthByte,ByteBuf byteBuf,int start){
        long length = getLongFromBytes(lengthByte);
        byteBuf.readBytes(2);

        int end = ByteBufCodecUtil.findCodeIndex(byteBuf, REDIS_CR);
        if (end < 0) {
            logger.error("not complete msg with end code '\r\n'");
            return -1;
        }

        checkContentLength(start, length, end);

        return (int) length;
    }
    private void checkContentLength(int start, long length, int end) {
        if (length != (end - start - REDIS_CRLF.length)) {
            throw new RedisException("length field:" + length + ",but receive data length:" + end);
        }

        if(length > FIX_MAX_LENGTH){
            throw new RedisException("fix length too long");
        }
    }


    protected Long decodeLong(ByteBuf buf) {
        if (buf.getByte(0) != NUMBER_LONG) {
            throwCodeHeadException(new String(new byte[]{NUMBER_LONG}));
        }

        int endIndex = ByteBufCodecUtil.findCodeIndex(buf, REDIS_CR);
        if (endIndex < 0) {
            logger.error("negative code:{} index", ":");
            return null;
        }

        ByteBuf byteBuf = buf;

        byteBuf.readByte();
        boolean negative = false;
        if (NEGATIVE_CODE == byteBuf.readByte()) {
            negative = true;
        }
        byte[] bytes = buildContainer(endIndex, byteBuf, negative);

        byteBuf.readBytes(bytes);

        // byte --> long
        long result = getLongFromBytes(bytes);

        byteBuf.readBytes(REDIS_CRLF.length);

        return negative ? -result : result;
    }

    private long getLongFromBytes(byte[] bytes) {
        long result = 0L;
        for (byte code : bytes) {
            result = result * 10 + (code - '0');
        }
        return result;
    }

    private byte[] buildContainer(int endIndex, ByteBuf byteBuf, boolean negative) {

        int length = negative ? endIndex - 2 : endIndex - 1;
        return new byte[length];
    }

    protected void throwCodeHeadException(String code) {
        throw new IllegalArgumentException("illegal bytebuf content, and plain string must start with '" + code + "'");
    }

    protected String decodePlainString(ByteBuf buf, byte type) {
        if (buf.getByte(0) != type) {
            throwCodeHeadException(new String(new byte[]{type}));
        }

        int endIndex = ByteBufCodecUtil.findCodeIndex(buf, REDIS_CR);

        if (endIndex < 0) {
            logger.error("negative code:{} index", "\r");
            return null;
        }

        ByteBuf byteBuf = buf;
        try {
            // strip head
            byteBuf.readByte();

            byte[] bytes = new byte[endIndex - 1];
            byteBuf.readBytes(bytes);

            // 跳过
            byteBuf.readBytes(REDIS_CRLF.length);

            return new String(bytes, CHARSET);
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getLocalizedMessage(), e);
        }
        return null;
    }

}
