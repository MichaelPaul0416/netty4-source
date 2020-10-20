package com.wq.netty.client.redis;

import org.junit.Test;

public class RedisClientTest {

    //    RedisClient redisClient = new RedisClient("49.235.253.133",6379);
    RedisClient redisClient = new RedisClient("127.0.0.1", 6379);

    @Test
    public void display() {
        redisClient.execute("get iic-authuser:1002226562");
    }

    @Test
    public void hget() {
        redisClient.execute(" hget framework java");
    }

    @Test
    public void lpush() {
        redisClient.execute("del score");
        redisClient.execute("lpush score java golang shell python");
        long len = redisClient.execute("llen score");
        System.out.println("llen score:"+len);
        for (int i = 0; i < len; i++) {
            System.out.println("lindex[" + i + "]:" + redisClient.execute("lindex score " + (i + 1)));
        }
    }
}