package org.stir.shrinkurl.runner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.stir.shrinkurl.util.RedisTestUtil;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Component
@Slf4j
public class RedisTestRunner implements ApplicationRunner {
    
    @Autowired
    private RedisTestUtil redisTestUtil;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Only run tests if --test-redis argument is provided
        if (args.containsOption("test-redis")) {
            log.info("Running Redis tests...");
            
            // Test Redis connection
            log.info("=== Testing Redis Connection ===");
            Map<String, Object> connectionResult = redisTestUtil.testRedisConnection();
            log.info("Connection test result: {}", connectionResult);
            
            // Test URL mapping
            log.info("=== Testing URL Mapping ===");
            Map<String, Object> urlMappingResult = redisTestUtil.testUrlMapping("https://example.com/test");
            log.info("URL mapping test result: {}", urlMappingResult);
            
            // Test rate limiting
            log.info("=== Testing Rate Limiting ===");
            Map<String, Object> rateLimitResult = redisTestUtil.testRateLimiting("test_startup", 5, 60);
            log.info("Rate limiting test result: {}", rateLimitResult);
            
            // Show current data
            log.info("=== Current URL Mappings ===");
            Map<String, Object> urlMappings = redisTestUtil.getAllUrlMappings();
            log.info("URL mappings: {}", urlMappings);
            
            log.info("=== Current Rate Limit Data ===");
            Map<String, Object> rateLimitData = redisTestUtil.getAllRateLimitData();
            log.info("Rate limit data: {}", rateLimitData);
            
            log.info("Redis tests completed!");
        }
    }
}
