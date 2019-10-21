package com.wq.netty.bytebuf.wrapper.codec;

import com.wq.netty.bytebuf.wrapper.core.RedisException;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;


public class RedisArrayCodec extends AbstractRedisDataCodec<RedisArrayCodec.InnerList<Object>> {


    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<Byte, Function<ByteBuf, Object>> resolver = new ConcurrentHashMap<>(5);

    public RedisArrayCodec() {
        resolver.put(AbstractRedisDataCodec.PLAIN_STRING, buf -> RedisArrayCodec.super.decodePlainString(buf, PLAIN_STRING));
        resolver.put(AbstractRedisDataCodec.EXCEPTION, buf -> {
            String msg = RedisArrayCodec.super.decodePlainString(buf, EXCEPTION);
            if (StringUtil.isNullOrEmpty(msg)) {
                throw new RedisException("empty err info");
            }
            return new RedisException(msg);
        });
        resolver.put(AbstractRedisDataCodec.NUMBER_LONG, buf -> RedisArrayCodec.super.decodeLong(buf));
        resolver.put(AbstractRedisDataCodec.FIX_LENGTH_STRING, buf -> RedisArrayCodec.super.fixLengthDecode(buf));
        resolver.put(AbstractRedisDataCodec.ITEM_ARRAY, buf -> RedisArrayCodec.this.readDecodeArray(buf));
    }

    @Override
    protected InnerList<Object> doDecode(ByteBuf buf) {
        ByteBuf copy = buf.copy();
        try {
            Byte type = determine(buf);
            logger.info("decode array item type:{}", RedisDataEnum.getInstance(type.byteValue()));

            int index = ByteBufCodecUtil.findCodeIndex(buf, REDIS_CR);
            if (index == -1) {
                return null;
            }


            if (type.byteValue() == AbstractRedisDataCodec.ITEM_ARRAY) {
                // TODO check apply argument is buf or copy
                copy.readByte();
                return (InnerList<Object>) resolver.get(type).apply(copy);
            }

            InnerList<Object> result = null;
            Object o = resolver.get(type).apply(copy);
            if (o != null){
                result = new InnerList<>();
                result.add(o);
            }
            return result;
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            return null;
        } finally {
            // deal with copy ByteBuf
            if (copy.readerIndex() > 0) {
                // count copy read index and make the same offset to buf
                // -1的原因是readerIndex表示的是下一个要读的字节的下标,实际上这个字节是还没读的,所以要-1
                logger.info("read bytes from copy ByteBuf[reader:{}/writer:{}] and record reader index to buf:{}", copy.readerIndex(), copy.writerIndex(), buf);
                buf.readBytes(copy.readerIndex() - 1);
            } else {
                logger.info("read nothing from copy ByteBuf");
            }
            ReferenceCountUtil.safeRelease(copy);
        }
    }

    private InnerList<Object> readDecodeArray(ByteBuf byteBuf) {
        // null array
        if (nullArray(byteBuf)) {
            return null;
        }

        // empty array
        if (byteBuf.getByte(1) == '0') {
            logger.info("empty array");
            // set index to \r\n
            // 因为这里是empty数组，所以剩余的三个字节一定是0\r\n
            byteBuf.readBytes(3);
            return new InnerList<>(0);
        }

        // each item decode
        // read length field
        int firstIndex = ByteBufCodecUtil.findCodeIndex(byteBuf, REDIS_CR);
        byte[] lengthByte = new byte[firstIndex - 1];

        byteBuf.readBytes(lengthByte);
        byteBuf.readBytes(REDIS_CRLF.length);// strip crlf

        long length = getLongFromBytes(lengthByte);
        if (length > FIX_MAX_LENGTH) {
            throw new RedisException("too long response msg");
        }

        int size = (int) length;
        InnerList<Object> innerList = new InnerList<>(size);

        for (int i = 0; i < size; i++) {
            Object o = this.doDecode(byteBuf);
            if (o instanceof InnerList && ((InnerList) o).size() == 1) {
                innerList.addAll((Collection) o);
            } else {
                innerList.add(o);
            }
        }
        return innerList;
    }

    private boolean nullArray(ByteBuf buf) {
        if (buf.getByte(1) == AbstractRedisDataCodec.NEGATIVE_CODE) {
            logger.info("null array");
            return true;
        }
        return false;
    }

    @Override
    protected ByteBuf doEncode(InnerList<Object> objectInnerList) {
        throw new RedisException("redisCodec can't support array encode...");
    }

    private byte determine(ByteBuf buf) {
        if (buf == null || buf.readableBytes() == 0) {
            throw new RedisException("empty byte buf");
        }

        byte type = buf.readByte();
        switch (type) {
            case AbstractRedisDataCodec.PLAIN_STRING:
                return AbstractRedisDataCodec.PLAIN_STRING;
            case AbstractRedisDataCodec.EXCEPTION:
                return AbstractRedisDataCodec.EXCEPTION;
            case AbstractRedisDataCodec.NUMBER_LONG:
                return AbstractRedisDataCodec.NUMBER_LONG;
            case AbstractRedisDataCodec.FIX_LENGTH_STRING:
                return AbstractRedisDataCodec.FIX_LENGTH_STRING;
            case AbstractRedisDataCodec.ITEM_ARRAY:
                return AbstractRedisDataCodec.ITEM_ARRAY;
            default:
                throw new RedisException("illegal head code");
        }
    }

    public static class InnerList<T> extends ArrayList implements Serializable {

        public InnerList(int length) {
            super(length);
        }

        public InnerList() {
            super();
        }

    }
}
