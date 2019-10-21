package com.wq.netty.client.redis;

import com.wq.netty.bytebuf.wrapper.codec.AbstractRedisDataCodec;
import com.wq.netty.bytebuf.wrapper.core.RedisException;
import com.wq.netty.client.redis.bean.RedisMessage;
import com.wq.netty.client.redis.bean.RedisResult;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RedisResultFuture<T> {

    private Channel channel;
    private RedisResult<T> redisResult;

    private final Lock lock = new ReentrantLock();
    private final Condition done = lock.newCondition();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final long TIMEOUT = 500000;

    public RedisResultFuture(Channel channel) {
        this.channel = channel;
    }

    public RedisResultFuture execute(String cmd) {
        RedisMessage redisMessage = new RedisMessage();
        redisMessage.setType(AbstractRedisDataCodec.PLAIN_STRING);
        redisMessage.setObject(cmd);

        this.channel.writeAndFlush(redisMessage);

        lock.lock();
        long start = System.currentTimeMillis();
        redisResult = new RedisResult<>();

        try {
            while (!redisResult.done()) {
                done.await(TIMEOUT, TimeUnit.MILLISECONDS);
                if (redisResult.done() || System.currentTimeMillis() - start > TIMEOUT) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        } finally {
            lock.unlock();
        }

        return this;
    }

    public void received(T t) {
        lock.lock();
        try {
            if (this.redisResult == null || this.redisResult.done()) {
                throw new RedisException("redis 等待设置返回结果异常");
            }

            this.redisResult.setResult(t);
            this.done.signalAll();
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    public T get() {
        lock.lock();
        T result = null;
        try {
            result = this.redisResult.getResult();
            this.redisResult = null;
        } finally {
            lock.unlock();
        }

        return result;
    }


}
