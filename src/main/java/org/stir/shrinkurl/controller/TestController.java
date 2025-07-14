package org.stir.shrinkurl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.stir.shrinkurl.util.RedisTestUtil;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@Slf4j
public class TestController {
    
    @Autowired
    private RedisTestUtil redisTestUtil;
    
    /**
     * Test Redis connection
     */
    @GetMapping("/redis/connection")
    public ResponseEntity<Map<String, Object>> testRedisConnection() {
        log.info("Testing Redis connection...");
        return ResponseEntity.ok(redisTestUtil.testRedisConnection());
    }
    
    /**
     * Test URL mapping functionality
     */
    @PostMapping("/redis/url-mapping")
    public ResponseEntity<Map<String, Object>> testUrlMapping(
            @RequestParam(defaultValue = "https://example.com") String testUrl) {
        log.info("Testing URL mapping with URL: {}", testUrl);
        return ResponseEntity.ok(redisTestUtil.testUrlMapping(testUrl));
    }
    
    /**
     * Test rate limiting functionality
     */
    @PostMapping("/redis/rate-limit")
    public ResponseEntity<Map<String, Object>> testRateLimiting(
            @RequestParam(defaultValue = "test_user") String testKey,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "60") int windowSeconds) {
        log.info("Testing rate limiting for key: {}, limit: {}, window: {}", testKey, limit, windowSeconds);
        return ResponseEntity.ok(redisTestUtil.testRateLimiting(testKey, limit, windowSeconds));
    }
    
    /**
     * Get all URL mappings
     */
    @GetMapping("/redis/url-mappings/all")
    public ResponseEntity<Map<String, Object>> getAllUrlMappings() {
        log.info("Getting all URL mappings from Redis...");
        return ResponseEntity.ok(redisTestUtil.getAllUrlMappings());
    }
    
    /**
     * Get all rate limit data
     */
    @GetMapping("/redis/rate-limits/all")
    public ResponseEntity<Map<String, Object>> getAllRateLimitData() {
        log.info("Getting all rate limit data from Redis...");
        return ResponseEntity.ok(redisTestUtil.getAllRateLimitData());
    }
}
