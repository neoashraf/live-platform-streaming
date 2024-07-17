package com.tanvir.core.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisClientConfig {
    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(@Qualifier("reactiveRedisConnectionFactory") ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        RedisSerializationContext.RedisSerializationContextBuilder<String, String> builder = RedisSerializationContext.newSerializationContext(keySerializer);
        RedisSerializationContext<String, String> context = builder.value(keySerializer).build();
        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveValueOperations<String, String> reactiveValueOperations(@Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        return redisTemplate.opsForValue();
    }
}
