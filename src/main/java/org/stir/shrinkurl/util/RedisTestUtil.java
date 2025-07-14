package org.stir.shrinkurl.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.stir.shrinkurl.service.RateLimitService;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RedisTestUtil {
    
    @Autowired
    @Qualifier("urlMappingRedisTemplate")
    private RedisTemplate<String, Object> urlMappingRedisTemplate;
    
    @Autowired
    @Qualifier("rateLimitRedisTemplate")
    private RedisTemplate<String, Object> rateLimitRedisTemplate;
    
    @Autowired
    private RateLimitService rateLimitService;
    
    /**
     * Test Redis connection and basic operations
     */
    public Map<String, Object> testRedisConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Test URL mapping Redis
            String testKey = "test:connection:" + System.currentTimeMillis();
            String testValue = "connection_test_value";
            
            urlMappingRedisTemplate.opsForValue().set(testKey, testValue, 10, TimeUnit.SECONDS);
            String retrievedValue = (String) urlMappingRedisTemplate.opsForValue().get(testKey);
            
            result.put("url_mapping_redis", testValue.equals(retrievedValue) ? "OK" : "FAILED");
            
            // Test rate limit Redis
            rateLimitRedisTemplate.opsForValue().set(testKey, testValue, 10, TimeUnit.SECONDS);
            String retrievedRateValue = (String) rateLimitRedisTemplate.opsForValue().get(testKey);
            
            result.put("rate_limit_redis", testValue.equals(retrievedRateValue) ? "OK" : "FAILED");
            
            // Clean up
            urlMappingRedisTemplate.delete(testKey);
            rateLimitRedisTemplate.delete(testKey);
            
            result.put("overall_status", "SUCCESS");
            
        } catch (Exception e) {
            log.error("Redis connection test failed", e);
            result.put("overall_status", "FAILED");
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Test URL mapping storage and retrieval
     */
    public Map<String, Object> testUrlMapping(String testUrl) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Create a shortened URL
            String shortCode = "test" + System.currentTimeMillis();
            String redisKey = "url:" + shortCode;
            
            // Store in Redis
            urlMappingRedisTemplate.opsForValue().set(redisKey, testUrl, 300, TimeUnit.SECONDS);
            
            // Retrieve from Redis
            String retrievedUrl = (String) urlMappingRedisTemplate.opsForValue().get(redisKey);
            
            result.put("test_url", testUrl);
            result.put("short_code", shortCode);
            result.put("redis_key", redisKey);
            result.put("retrieved_url", retrievedUrl);
            result.put("mapping_works", testUrl.equals(retrievedUrl));
            
            // Check TTL
            Long ttl = urlMappingRedisTemplate.getExpire(redisKey);
            result.put("ttl_seconds", ttl);
            
            // Clean up
            urlMappingRedisTemplate.delete(redisKey);
            
        } catch (Exception e) {
            log.error("URL mapping test failed", e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Test rate limiting functionality
     */
    public Map<String, Object> testRateLimiting(String testKey, int limit, int windowSeconds) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Clear any existing rate limit
            rateLimitService.resetRateLimit(testKey);
            
            // Test rate limiting
            int requestCount = 0;
            boolean hitLimit = false;
            
            for (int i = 0; i < limit + 5; i++) {
                if (rateLimitService.isAllowed(testKey, limit, windowSeconds)) {
                    requestCount++;
                } else {
                    hitLimit = true;
                    break;
                }
            }
            
            result.put("test_key", testKey);
            result.put("limit", limit);
            result.put("window_seconds", windowSeconds);
            result.put("allowed_requests", requestCount);
            result.put("hit_limit", hitLimit);
            result.put("rate_limiting_works", requestCount <= limit && hitLimit);
            
            // Get rate limit status
            RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(testKey, limit, windowSeconds);
            result.put("current_count", status.getCurrentCount());
            result.put("remaining", status.getRemaining());
            result.put("reset_time", status.getResetTime());
            
            // Clean up
            rateLimitService.resetRateLimit(testKey);
            
        } catch (Exception e) {
            log.error("Rate limiting test failed", e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Get all URL mappings from Redis
     */
    public Map<String, Object> getAllUrlMappings() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Set<String> keys = urlMappingRedisTemplate.keys("url:*");
            result.put("total_mappings", keys != null ? keys.size() : 0);
            
            if (keys != null) {
                Map<String, Object> mappings = new HashMap<>();
                for (String key : keys) {
                    String originalUrl = (String) urlMappingRedisTemplate.opsForValue().get(key);
                    Long ttl = urlMappingRedisTemplate.getExpire(key);
                    
                    Map<String, Object> mapping = new HashMap<>();
                    mapping.put("original_url", originalUrl);
                    mapping.put("ttl_seconds", ttl);
                    
                    mappings.put(key, mapping);
                }
                result.put("mappings", mappings);
            }
            
        } catch (Exception e) {
            log.error("Failed to get URL mappings", e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Get all rate limit data from Redis
     */
    public Map<String, Object> getAllRateLimitData() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Set<String> rateLimitKeys = rateLimitRedisTemplate.keys("rate_limit:*");
            Set<String> globalRateLimitKeys = rateLimitRedisTemplate.keys("global_rate_limit:*");
            
            result.put("total_rate_limit_keys", rateLimitKeys != null ? rateLimitKeys.size() : 0);
            result.put("total_global_rate_limit_keys", globalRateLimitKeys != null ? globalRateLimitKeys.size() : 0);
            
            if (rateLimitKeys != null) {
                Map<String, Object> rateLimits = new HashMap<>();
                for (String key : rateLimitKeys) {
                    String count = (String) rateLimitRedisTemplate.opsForValue().get(key);
                    Long ttl = rateLimitRedisTemplate.getExpire(key);
                    
                    Map<String, Object> rateLimit = new HashMap<>();
                    rateLimit.put("count", count != null ? count.replace("\"", "") : "0");
                    rateLimit.put("ttl_seconds", ttl);
                    
                    rateLimits.put(key, rateLimit);
                }
                result.put("rate_limits", rateLimits);
            }
            
            if (globalRateLimitKeys != null) {
                Map<String, Object> globalRateLimits = new HashMap<>();
                for (String key : globalRateLimitKeys) {
                    String count = (String) rateLimitRedisTemplate.opsForValue().get(key);
                    Long ttl = rateLimitRedisTemplate.getExpire(key);
                    
                    Map<String, Object> rateLimit = new HashMap<>();
                    rateLimit.put("count", count != null ? count.replace("\"", "") : "0");
                    rateLimit.put("ttl_seconds", ttl);
                    
                    globalRateLimits.put(key, rateLimit);
                }
                result.put("global_rate_limits", globalRateLimits);
            }
            
        } catch (Exception e) {
            log.error("Failed to get rate limit data", e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}
