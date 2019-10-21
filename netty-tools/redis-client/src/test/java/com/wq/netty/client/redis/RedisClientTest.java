package com.wq.netty.client.redis;

import org.junit.Test;

public class RedisClientTest {

//    RedisClient redisClient = new RedisClient("49.235.253.133",6379);
    RedisClient redisClient = new RedisClient("localhost",6379);

    @Test
    public void display(){
        redisClient.execute("get s3");
    }
}