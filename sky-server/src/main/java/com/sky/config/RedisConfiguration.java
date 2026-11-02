package com.sky.config;

import com.sky.json.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfiguration {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {

        log.info("创建RedisTemplate对象...");
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        //设置redis连接工厂对象
        redisTemplate.setConnectionFactory(factory);
        //设置redis的key序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        
        //使用自定义的JacksonObjectMapper来处理LocalDateTime等时间类型的序列化
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(new JacksonObjectMapper());
        //设置redis的value序列化器（使用JSON格式）
        redisTemplate.setValueSerializer(serializer);
        //设置redis的hash key序列化器
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        //设置redis的hash value序列化器
        redisTemplate.setHashValueSerializer(serializer);
        return redisTemplate;
    }
}
