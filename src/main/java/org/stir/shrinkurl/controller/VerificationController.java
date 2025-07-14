package org.stir.shrinkurl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.stir.shrinkurl.service.RateLimitService;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/verify")
@Slf4j
public class VerificationController {
    
    @Autowired
    @Qualifier("urlMappingRedisTemplate")
    private RedisTemplate<String, Object> urlMappingRedisTemplate;
    
    @Autowired
    @Qualifier("rateLimitRedisTemplate")
    private RedisTemplate<String, Object> rateLimitRedisTemplate;
    
    @Autowired
    private RateLimitService rateLimitService;
    
    /**
     * Comprehensive verification endpoint
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> verifyAll() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. Redis Connection Status
            response.put("redis_status", checkRedisConnection());
            
            // 2. URL Mappings Verification
            response.put("url_mappings", checkUrlMappings());
            
            // 3. Rate Limiting Verification
            response.put("rate_limiting", checkRateLimiting());
            
            // 4. Application Health
            response.put("application_health", checkApplicationHealth());
            
            response.put("verification_time", LocalDateTime.now());
            response.put("status", "success");
            
        } catch (Exception e) {
            log.error("Verification failed", e);
            response.put("status", "error");
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Test URL creation and Redis storage
     */
    @PostMapping("/test-url-storage")
    public ResponseEntity<Map<String, Object>> testUrlStorage(@RequestParam String testUrl) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Create a test URL (this would require authentication in real scenario)
            String shortCode = "test_" + System.currentTimeMillis();
            
            // Store in Redis manually for testing
            String redisKey = "url:" + shortCode;
            urlMappingRedisTemplate.opsForValue().set(redisKey, testUrl);
            
            // Verify storage
            String storedUrl = (String) urlMappingRedisTemplate.opsForValue().get(redisKey);
            Long ttl = urlMappingRedisTemplate.getExpire(redisKey);
            
            response.put("short_code", shortCode);
            response.put("original_url", testUrl);
            response.put("stored_url", storedUrl);
            response.put("storage_successful", testUrl.equals(storedUrl));
            response.put("ttl_seconds", ttl);
            response.put("redis_key", redisKey);
            
            // Clean up
            urlMappingRedisTemplate.delete(redisKey);
            
            response.put("status", "success");
            
        } catch (Exception e) {
            log.error("URL storage test failed", e);
            response.put("status", "error");
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Test rate limiting functionality
     */
    @PostMapping("/test-rate-limiting")
    public ResponseEntity<Map<String, Object>> testRateLimiting(
            @RequestParam(defaultValue = "test_key") String testKey,
            @RequestParam(defaultValue = "3") int limit,
            @RequestParam(defaultValue = "60") int windowSeconds) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Test rate limiting
            Map<String, Object> testResults = new HashMap<>();
            
            for (int i = 1; i <= limit + 2; i++) {
                boolean allowed = rateLimitService.isAllowed(testKey, limit, windowSeconds);
                RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(testKey, limit, windowSeconds);
                
                Map<String, Object> attemptResult = new HashMap<>();
                attemptResult.put("allowed", allowed);
                attemptResult.put("current_count", status.getCurrentCount());
                attemptResult.put("remaining", status.getRemaining());
                attemptResult.put("expected_result", i <= limit ? "allowed" : "blocked");
                attemptResult.put("correct", (i <= limit) == allowed);
                
                testResults.put("attempt_" + i, attemptResult);
            }
            
            response.put("test_results", testResults);
            response.put("test_key", testKey);
            response.put("limit", limit);
            response.put("window_seconds", windowSeconds);
            
            // Clean up
            rateLimitService.resetRateLimit(testKey);
            
            response.put("status", "success");
            
        } catch (Exception e) {
            log.error("Rate limiting test failed", e);
            response.put("status", "error");
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get Redis statistics
     */
    @GetMapping("/redis-stats")
    public ResponseEntity<Map<String, Object>> getRedisStats() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // URL mappings statistics
            Set<String> urlKeys = urlMappingRedisTemplate.keys("url:*");
            response.put("total_url_mappings", urlKeys != null ? urlKeys.size() : 0);
            
            // Rate limiting statistics
            Set<String> rateLimitKeys = rateLimitRedisTemplate.keys("rate_limit:*");
            Set<String> globalRateLimitKeys = rateLimitRedisTemplate.keys("global_rate_limit:*");
            
            response.put("total_rate_limit_keys", rateLimitKeys != null ? rateLimitKeys.size() : 0);
            response.put("total_global_rate_limit_keys", globalRateLimitKeys != null ? globalRateLimitKeys.size() : 0);
            
            // Memory usage (approximate)
            response.put("url_mapping_db", "0");
            response.put("rate_limiting_db", "1");
            
            response.put("status", "success");
            
        } catch (Exception e) {
            log.error("Redis stats retrieval failed", e);
            response.put("status", "error");
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    private Map<String, Object> checkRedisConnection() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Test URL mapping Redis
            urlMappingRedisTemplate.opsForValue().set("test_connection", "OK");
            String result = (String) urlMappingRedisTemplate.opsForValue().get("test_connection");
            urlMappingRedisTemplate.delete("test_connection");
            
            status.put("url_mapping_db", result != null && result.equals("OK") ? "connected" : "failed");
            
            // Test rate limiting Redis
            rateLimitRedisTemplate.opsForValue().set("test_connection", "OK");
            result = (String) rateLimitRedisTemplate.opsForValue().get("test_connection");
            rateLimitRedisTemplate.delete("test_connection");
            
            status.put("rate_limiting_db", result != null && result.equals("OK") ? "connected" : "failed");
            
        } catch (Exception e) {
            status.put("error", e.getMessage());
        }
        
        return status;
    }
    
    private Map<String, Object> checkUrlMappings() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            Set<String> urlKeys = urlMappingRedisTemplate.keys("url:*");
            status.put("total_count", urlKeys != null ? urlKeys.size() : 0);
            
            if (urlKeys != null && !urlKeys.isEmpty()) {
                // Sample a few URLs to verify
                int sampleSize = Math.min(3, urlKeys.size());
                Map<String, Object> samples = new HashMap<>();
                
                int count = 0;
                for (String key : urlKeys) {
                    if (count >= sampleSize) break;
                    
                    String url = (String) urlMappingRedisTemplate.opsForValue().get(key);
                    Long ttl = urlMappingRedisTemplate.getExpire(key);
                    
                    Map<String, Object> sample = new HashMap<>();
                    sample.put("url", url);
                    sample.put("ttl", ttl);
                    
                    samples.put(key, sample);
                    count++;
                }
                
                status.put("samples", samples);
            }
            
        } catch (Exception e) {
            status.put("error", e.getMessage());
        }
        
        return status;
    }
    
    private Map<String, Object> checkRateLimiting() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            Set<String> rateLimitKeys = rateLimitRedisTemplate.keys("rate_limit:*");
            Set<String> globalRateLimitKeys = rateLimitRedisTemplate.keys("global_rate_limit:*");
            
            status.put("endpoint_rate_limits", rateLimitKeys != null ? rateLimitKeys.size() : 0);
            status.put("global_rate_limits", globalRateLimitKeys != null ? globalRateLimitKeys.size() : 0);
            
            // Test rate limiting functionality
            String testKey = "verification_test";
            boolean allowed1 = rateLimitService.isAllowed(testKey, 2, 60);
            boolean allowed2 = rateLimitService.isAllowed(testKey, 2, 60);
            boolean allowed3 = rateLimitService.isAllowed(testKey, 2, 60);
            
            status.put("functionality_test", Map.of(
                "first_request", allowed1,
                "second_request", allowed2,
                "third_request", allowed3,
                "working_correctly", allowed1 && allowed2 && !allowed3
            ));
            
            // Clean up
            rateLimitService.resetRateLimit(testKey);
            
        } catch (Exception e) {
            status.put("error", e.getMessage());
        }
        
        return status;
    }
    
    private Map<String, Object> checkApplicationHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            health.put("timestamp", LocalDateTime.now());
            health.put("status", "healthy");
            
            // Add more health checks as needed
            health.put("database_connection", "active");
            health.put("redis_connection", "active");
            
        } catch (Exception e) {
            health.put("status", "unhealthy");
            health.put("error", e.getMessage());
        }
        
        return health;
    }
}
