package org.stir.shrinkurl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stir.shrinkurl.service.RateLimitService;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/debug")
@Slf4j
public class RedisDebugController {
    
    @Autowired
    @Qualifier("urlMappingRedisTemplate")
    private RedisTemplate<String, Object> urlMappingRedisTemplate;
    
    @Autowired
    @Qualifier("rateLimitRedisTemplate")
    private RedisTemplate<String, Object> rateLimitRedisTemplate;
    
    @Autowired
    private RateLimitService rateLimitService;
    
    /**
     * Check if Redis is working and show URL mappings
     */
    @GetMapping("/redis/urls")
    public ResponseEntity<Map<String, Object>> checkUrlMappings() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Check connection by doing a simple operation
            urlMappingRedisTemplate.opsForValue().set("connection_test", "OK", 1, TimeUnit.SECONDS);
            response.put("redis_connection", "OK");
            
            // Get all URL keys
            Set<String> urlKeys = urlMappingRedisTemplate.keys("url:*");
            response.put("total_url_mappings", urlKeys != null ? urlKeys.size() : 0);
            
            Map<String, Object> urlMappings = new HashMap<>();
            if (urlKeys != null) {
                for (String key : urlKeys) {
                    String originalUrl = (String) urlMappingRedisTemplate.opsForValue().get(key);
                    Long ttl = urlMappingRedisTemplate.getExpire(key);
                    
                    Map<String, Object> urlData = new HashMap<>();
                    urlData.put("original_url", originalUrl);
                    urlData.put("ttl_seconds", ttl);
                    urlData.put("expires_in", ttl != null && ttl > 0 ? ttl + " seconds" : "No expiration");
                    
                    urlMappings.put(key, urlData);
                }
            }
            
            response.put("url_mappings", urlMappings);
            response.put("status", "success");
            
        } catch (Exception e) {
            log.error("Redis URL mapping check failed", e);
            response.put("status", "error");
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check rate limiting data in Redis
     */
    @GetMapping("/redis/rate-limits")
    public ResponseEntity<Map<String, Object>> checkRateLimits() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Check connection by doing a simple operation
            rateLimitRedisTemplate.opsForValue().set("connection_test", "OK", 1, TimeUnit.SECONDS);
            response.put("redis_connection", "OK");
            
            // Get all rate limit keys
            Set<String> rateLimitKeys = rateLimitRedisTemplate.keys("rate_limit:*");
            Set<String> globalRateLimitKeys = rateLimitRedisTemplate.keys("global_rate_limit:*");
            
            response.put("total_rate_limit_keys", rateLimitKeys != null ? rateLimitKeys.size() : 0);
            response.put("total_global_rate_limit_keys", globalRateLimitKeys != null ? globalRateLimitKeys.size() : 0);
            
            Map<String, Object> rateLimits = new HashMap<>();
            if (rateLimitKeys != null) {
                for (String key : rateLimitKeys) {
                    String count = (String) rateLimitRedisTemplate.opsForValue().get(key);
                    Long ttl = rateLimitRedisTemplate.getExpire(key);
                    
                    Map<String, Object> rateData = new HashMap<>();
                    rateData.put("current_count", count != null ? count.replace("\"", "") : "0");
                    rateData.put("ttl_seconds", ttl);
                    rateData.put("resets_in", ttl != null && ttl > 0 ? ttl + " seconds" : "Expired");
                    
                    rateLimits.put(key, rateData);
                }
            }
            
            Map<String, Object> globalRateLimits = new HashMap<>();
            if (globalRateLimitKeys != null) {
                for (String key : globalRateLimitKeys) {
                    String count = (String) rateLimitRedisTemplate.opsForValue().get(key);
                    Long ttl = rateLimitRedisTemplate.getExpire(key);
                    
                    Map<String, Object> rateData = new HashMap<>();
                    rateData.put("current_count", count != null ? count.replace("\"", "") : "0");
                    rateData.put("ttl_seconds", ttl);
                    rateData.put("resets_in", ttl != null && ttl > 0 ? ttl + " seconds" : "Expired");
                    
                    globalRateLimits.put(key, rateData);
                }
            }
            
            response.put("rate_limits", rateLimits);
            response.put("global_rate_limits", globalRateLimits);
            response.put("status", "success");
            
        } catch (Exception e) {
            log.error("Redis rate limit check failed", e);
            response.put("status", "error");
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check specific URL mapping
     */
    @GetMapping("/redis/url/{shortCode}")
    public ResponseEntity<Map<String, Object>> checkSpecificUrl(@PathVariable String shortCode) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String key = "url:" + shortCode;
            String originalUrl = (String) urlMappingRedisTemplate.opsForValue().get(key);
            Long ttl = urlMappingRedisTemplate.getExpire(key);
            
            response.put("short_code", shortCode);
            response.put("redis_key", key);
            response.put("original_url", originalUrl);
            response.put("ttl_seconds", ttl);
            response.put("found_in_redis", originalUrl != null);
            
            if (ttl != null && ttl > 0) {
                response.put("expires_in", ttl + " seconds");
            } else if (ttl == -1) {
                response.put("expires_in", "No expiration");
            } else {
                response.put("expires_in", "Expired");
            }
            
            response.put("status", "success");
            
        } catch (Exception e) {
            log.error("Redis URL check failed for shortCode: {}", shortCode, e);
            response.put("status", "error");
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check rate limit status for a specific key
     */
    @GetMapping("/redis/rate-limit/{key}")
    public ResponseEntity<Map<String, Object>> checkRateLimit(@PathVariable String key) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Check endpoint rate limit
            RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(key, 100, 60);
            
            response.put("key", key);
            response.put("current_count", status.getCurrentCount());
            response.put("limit", status.getLimit());
            response.put("remaining", status.getRemaining());
            response.put("reset_time", status.getResetTime());
            response.put("status", "success");
            
        } catch (Exception e) {
            log.error("Rate limit check failed for key: {}", key, e);
            response.put("status", "error");
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Redis health check
     */
    @GetMapping("/redis/health")
    public ResponseEntity<Map<String, Object>> redisHealth() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Test URL mapping Redis
            urlMappingRedisTemplate.opsForValue().set("health_check", "OK", 5, TimeUnit.SECONDS);
            String urlHealthCheck = (String) urlMappingRedisTemplate.opsForValue().get("health_check");
            
            // Test rate limit Redis
            rateLimitRedisTemplate.opsForValue().set("health_check", "OK", 5, TimeUnit.SECONDS);
            String rateLimitHealthCheck = (String) rateLimitRedisTemplate.opsForValue().get("health_check");
            
            response.put("url_mapping_redis", "OK".equals(urlHealthCheck) ? "HEALTHY" : "UNHEALTHY");
            response.put("rate_limit_redis", "OK".equals(rateLimitHealthCheck) ? "HEALTHY" : "UNHEALTHY");
            response.put("overall_status", "HEALTHY");
            
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            response.put("overall_status", "UNHEALTHY");
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}
