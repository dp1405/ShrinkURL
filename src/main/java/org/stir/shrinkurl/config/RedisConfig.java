package org.stir.shrinkurl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    
    @Value("${spring.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.redis.password:}")
    private String redisPassword;
    
    // Redis DB 0 for URL mappings
    @Bean(name = "urlMappingRedisTemplate")
    public RedisTemplate<String, Object> urlMappingRedisTemplate() {
        return createRedisTemplate(0);
    }
    
    // Redis DB 1 for rate limiting
    @Bean(name = "rateLimitRedisTemplate")
    public RedisTemplate<String, Object> rateLimitRedisTemplate() {
        return createRedisTemplate(1);
    }
    
    private RedisTemplate<String, Object> createRedisTemplate(int database) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        redisConfig.setDatabase(database);
        
        if (redisPassword != null && !redisPassword.isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }
        
        JedisConnectionFactory connectionFactory = new JedisConnectionFactory(redisConfig);
        connectionFactory.afterPropertiesSet();
        
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.setDefaultSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        
        return template;
    }
}