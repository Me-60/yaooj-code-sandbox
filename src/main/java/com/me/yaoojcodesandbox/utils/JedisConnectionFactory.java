package com.me.yaoojcodesandbox.utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

/**
 * Jedis 连接池工具类
 *
 * @author Yao-happy
 * @version 0.0.1
 * 时间：2024/9/7 21:14
 * 作者博客：https://www.cnblogs.com/Yao-happy
 */
public class JedisConnectionFactory {

    private static final JedisPool jedisPool;

    static {

        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();

        jedisPoolConfig.setMaxTotal(8);

        jedisPoolConfig.setMaxIdle(8);

        jedisPoolConfig.setMinIdle(0);

        jedisPoolConfig.setMaxWait(Duration.ofSeconds(5));

        jedisPool = new JedisPool(jedisPoolConfig, "127.0.0.1", 6379, null, "123456");
    }

    public static Jedis getJedis() {

        return jedisPool.getResource();
    }
}
