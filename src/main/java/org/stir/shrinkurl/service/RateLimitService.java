package org.stir.shrinkurl.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RateLimitService {
    
    @Autowired
    @Qualifier("rateLimitRedisTemplate")
    private RedisTemplate<String, Object> rateLimitRedisTemplate;
    
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String GLOBAL_RATE_LIMIT_PREFIX = "global_rate_limit:";
    
    /**
     * Check if request is allowed under rate limit
     */
    public boolean isAllowed(String key, int limit, int timeWindowSeconds) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        
        try {
            // Check if key exists first
            Boolean keyExists = rateLimitRedisTemplate.hasKey(redisKey);
            
            if (!keyExists) {
                // Initialize counter with 1 and set expiration
                rateLimitRedisTemplate.opsForValue().set(redisKey, "1");
                rateLimitRedisTemplate.expire(redisKey, timeWindowSeconds, TimeUnit.SECONDS);
                return true;
            }
            
            // Get current count
            String countStr = (String) rateLimitRedisTemplate.opsForValue().get(redisKey);
            int currentCount = 0;
            
            if (countStr != null) {
                // Remove quotes if present
                countStr = countStr.replace("\"", "").trim();
                try {
                    currentCount = Integer.parseInt(countStr);
                } catch (NumberFormatException e) {
                    log.warn("Invalid count value in Redis for key {}: {}", key, countStr);
                    currentCount = 0;
                }
            }
            
            if (currentCount >= limit) {
                log.debug("Rate limit exceeded for key: {} (current: {}, limit: {})", key, currentCount, limit);
                return false;
            }
            
            // Increment counter
            int newCount = currentCount + 1;
            rateLimitRedisTemplate.opsForValue().set(redisKey, String.valueOf(newCount));
            return true;
            
        } catch (Exception e) {
            log.error("Rate limit check failed for key: {}", key, e);
            // In case of Redis failure, allow the request
            return true;
        }
    }
    
    /**
     * Check global rate limit
     */
    public boolean isGloballyAllowed(String identifier, int limit, int timeWindowSeconds) {
        String redisKey = GLOBAL_RATE_LIMIT_PREFIX + identifier;
        
        try {
            // Check if key exists first
            Boolean keyExists = rateLimitRedisTemplate.hasKey(redisKey);
            
            if (!keyExists) {
                // Initialize counter with 1 and set expiration
                rateLimitRedisTemplate.opsForValue().set(redisKey, "1");
                rateLimitRedisTemplate.expire(redisKey, timeWindowSeconds, TimeUnit.SECONDS);
                return true;
            }
            
            // Get current count
            String countStr = (String) rateLimitRedisTemplate.opsForValue().get(redisKey);
            int currentCount = 0;
            
            if (countStr != null) {
                // Remove quotes if present
                countStr = countStr.replace("\"", "").trim();
                try {
                    currentCount = Integer.parseInt(countStr);
                } catch (NumberFormatException e) {
                    log.warn("Invalid count value in Redis for global key {}: {}", identifier, countStr);
                    currentCount = 0;
                }
            }
            
            if (currentCount >= limit) {
                log.debug("Global rate limit exceeded for identifier: {} (current: {}, limit: {})", identifier, currentCount, limit);
                return false;
            }
            
            // Increment counter
            int newCount = currentCount + 1;
            rateLimitRedisTemplate.opsForValue().set(redisKey, String.valueOf(newCount));
            return true;
            
        } catch (Exception e) {
            log.error("Global rate limit check failed for identifier: {}", identifier, e);
            // In case of Redis failure, allow the request
            return true;
        }
    }
    
    /**
     * Get current rate limit status
     */
    public RateLimitStatus getRateLimitStatus(String key, int limit, int timeWindowSeconds) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        
        try {
            // Get current count as String
            String countStr = (String) rateLimitRedisTemplate.opsForValue().get(redisKey);
            int currentCount = 0;
            
            if (countStr != null) {
                // Remove quotes if present
                countStr = countStr.replace("\"", "").trim();
                try {
                    currentCount = Integer.parseInt(countStr);
                } catch (NumberFormatException e) {
                    log.warn("Invalid count value in Redis for key {}: {}", key, countStr);
                    currentCount = 0;
                }
            }
            
            Long ttl = rateLimitRedisTemplate.getExpire(redisKey);
            long resetTime = ttl != null && ttl > 0 ? 
                Instant.now().getEpochSecond() + ttl : 
                Instant.now().getEpochSecond() + timeWindowSeconds;
            
            return new RateLimitStatus(
                currentCount,
                limit,
                Math.max(0, limit - currentCount),
                resetTime
            );
            
        } catch (Exception e) {
            log.error("Failed to get rate limit status for key: {}", key, e);
            return new RateLimitStatus(0, limit, limit, Instant.now().getEpochSecond() + timeWindowSeconds);
        }
    }
    
    /**
     * Reset rate limit for a key
     */
    public void resetRateLimit(String key) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        try {
            rateLimitRedisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.error("Failed to reset rate limit for key: {}", key, e);
        }
    }
    
    /**
     * Rate limit status data class
     */
    public static class RateLimitStatus {
        private final int currentCount;
        private final int limit;
        private final int remaining;
        private final long resetTime;
        
        public RateLimitStatus(int currentCount, int limit, int remaining, long resetTime) {
            this.currentCount = currentCount;
            this.limit = limit;
            this.remaining = remaining;
            this.resetTime = resetTime;
        }
        
        public int getCurrentCount() { return currentCount; }
        public int getLimit() { return limit; }
        public int getRemaining() { return remaining; }
        public long getResetTime() { return resetTime; }
    }
}
