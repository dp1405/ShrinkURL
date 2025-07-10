package org.stir.shrinkurl.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stir.shrinkurl.entity.Url;
import org.stir.shrinkurl.entity.UrlAnalytics;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.enums.SubscriptionPlan;
import org.stir.shrinkurl.repository.UrlAnalyticsRepository;
import org.stir.shrinkurl.repository.UrlRepository;
import org.stir.shrinkurl.utils.UrlShortenerUtil;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UrlService {
    
    @Autowired
    private UrlRepository urlRepository;
    
    @Autowired
    private UrlAnalyticsRepository urlAnalyticsRepository;
    
    @Autowired
    private UrlShortenerUtil urlShortenerUtil;
    
    @Autowired
    @Qualifier("urlMappingRedisTemplate")
    private RedisTemplate<String, Object> urlMappingRedisTemplate;
    
    private static final String REDIS_URL_PREFIX = "url:";
    
    /**
     * Create shortened URL
     */
    @Transactional
    public Url createShortenedUrl(String originalUrl, User user, String customCode) {
        // Validate limits based on subscription
        validateUrlCreationLimits(user);
        
        String shortCode;
        if (customCode != null && !customCode.isEmpty()) {
            // Custom code - only for premium users
            if (user.getSubscriptionPlan() == SubscriptionPlan.FREE) {
                throw new IllegalArgumentException("Custom URLs are only available for premium users");
            }
            
            if (!urlShortenerUtil.isValidCustomCode(customCode)) {
                throw new IllegalArgumentException("Invalid custom code format");
            }
            
            if (urlRepository.findByShortCode(customCode).isPresent()) {
                throw new IllegalArgumentException("Custom code already exists");
            }
            
            shortCode = customCode;
        } else {
            // Generate short code with collision handling
            shortCode = generateUniqueShortCode(originalUrl);
        }
        
        // Set expiration based on subscription
        LocalDateTime expiresAt = calculateExpirationDate(user.getSubscriptionPlan());
        
        // Create URL entity
        Url url = new Url();
        url.setOriginalUrl(originalUrl);
        url.setShortCode(shortCode);
        url.setUserId(user.getId());
        url.setCreatedAt(LocalDateTime.now());
        url.setExpiresAt(expiresAt);
        url.setIsActive(true);
        url.setClickCount(0);
        
        // Save to database
        url = urlRepository.save(url);
        
        // Cache in Redis with TTL
        cacheUrlMapping(shortCode, originalUrl, expiresAt);
        
        log.info("Created shortened URL: {} -> {} for user: {}", shortCode, originalUrl, user.getEmail());
        return url;
    }
    
    /**
     * Resolve short code to original URL with Redis fallback
     */
    @Transactional
    public Optional<String> resolveUrl(String shortCode) {
        // Try Redis first with 1 second timeout
        try {
            // Use a timeout wrapper for Redis operations
            String originalUrl = getFromRedisWithTimeout(shortCode, 1000);
            
            if (originalUrl != null) {
                // Track click asynchronously
                trackClickAsync(shortCode);
                return Optional.of(originalUrl);
            }
        } catch (Exception e) {
            log.warn("Redis lookup failed for {}, falling back to database: {}", shortCode, e.getMessage());
        }
        
        // Fallback to database
        Optional<Url> urlOpt = urlRepository.findByShortCodeAndIsActive(shortCode, true);
        if (urlOpt.isPresent()) {
            Url url = urlOpt.get();
            
            // Check if expired
            if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now())) {
                url.setIsActive(false);
                urlRepository.save(url);
                return Optional.empty();
            }
            
            // Re-cache in Redis
            cacheUrlMapping(shortCode, url.getOriginalUrl(), url.getExpiresAt());
            
            // Track click
            trackClickAsync(shortCode);
            
            return Optional.of(url.getOriginalUrl());
        }
        
        return Optional.empty();
    }
    
    /**
     * Get URL from Redis with timeout
     */
    private String getFromRedisWithTimeout(String shortCode, long timeoutMs) {
        try {
            // This will use the default timeout configured in Redis template
            String originalUrl = (String) urlMappingRedisTemplate.opsForValue()
                .get(REDIS_URL_PREFIX + shortCode);
            return originalUrl;
        } catch (Exception e) {
            log.debug("Redis timeout or error for {}: {}", shortCode, e.getMessage());
            return null;
        }
    }
    
    /**
     * Generate unique short code with collision handling
     */
    private String generateUniqueShortCode(String originalUrl) {
        String shortCode;
        int attempt = 0;
        
        do {
            shortCode = attempt == 0 ? 
                urlShortenerUtil.generateShortCode(originalUrl) : 
                urlShortenerUtil.generateShortCodeWithSalt(originalUrl, attempt);
            attempt++;
        } while (urlRepository.findByShortCode(shortCode).isPresent() && attempt < 10);
        
        if (attempt >= 10) {
            throw new RuntimeException("Could not generate unique short code after 10 attempts");
        }
        
        return shortCode;
    }
    
    /**
     * Cache URL mapping in Redis
     */
    private void cacheUrlMapping(String shortCode, String originalUrl, LocalDateTime expiresAt) {
        try {
            String redisKey = REDIS_URL_PREFIX + shortCode;
            
            if (expiresAt != null) {
                long ttl = java.time.Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
                if (ttl > 0) {
                    urlMappingRedisTemplate.opsForValue().set(redisKey, originalUrl);
                    urlMappingRedisTemplate.expire(redisKey, ttl, TimeUnit.SECONDS);
                }
            } else {
                urlMappingRedisTemplate.opsForValue().set(redisKey, originalUrl);
            }
        } catch (Exception e) {
            log.warn("Failed to cache URL mapping for {}: {}", shortCode, e.getMessage());
        }
    }
    
    /**
     * Track click asynchronously
     */
    private void trackClickAsync(String shortCode) {
        // This would be better implemented with @Async
        try {
            Optional<Url> urlOpt = urlRepository.findByShortCode(shortCode);
            if (urlOpt.isPresent()) {
                Url url = urlOpt.get();
                
                // Increment total click count
                url.setClickCount(url.getClickCount() + 1);
                urlRepository.save(url);
                
                // Update daily analytics
                LocalDate today = LocalDate.now();
                Optional<UrlAnalytics> analyticsOpt = urlAnalyticsRepository.findByUrlIdAndClickDate(url.getId(), today);
                
                if (analyticsOpt.isPresent()) {
                    UrlAnalytics analytics = analyticsOpt.get();
                    analytics.setClickCount(analytics.getClickCount() + 1);
                    urlAnalyticsRepository.save(analytics);
                } else {
                    UrlAnalytics analytics = UrlAnalytics.builder()
                        .urlId(url.getId())
                        .clickDate(today)
                        .clickCount(1)
                        .build();
                    urlAnalyticsRepository.save(analytics);
                }
            }
        } catch (Exception e) {
            log.error("Failed to track click for {}: {}", shortCode, e.getMessage());
        }
    }
    
    /**
     * Validate URL creation limits
     */
    private void validateUrlCreationLimits(User user) {
        long urlCount = urlRepository.countByUserId(user.getId());
        int limit = getUrlLimit(user.getSubscriptionPlan());
        
        if (urlCount >= limit) {
            throw new IllegalArgumentException("URL limit exceeded for your subscription plan");
        }
    }
    
    /**
     * Calculate expiration date based on subscription
     */
    private LocalDateTime calculateExpirationDate(SubscriptionPlan plan) {
        switch (plan) {
            case FREE:
                return LocalDateTime.now().plusDays(30);
            case MONTHLY:
                return LocalDateTime.now().plusDays(365);
            case YEARLY:
            case LIFETIME:
                return null; // No expiration
            default:
                return LocalDateTime.now().plusDays(30);
        }
    }
    
    /**
     * Get URL limit based on subscription
     */
    private int getUrlLimit(SubscriptionPlan plan) {
        switch (plan) {
            case FREE: return 10;
            case MONTHLY: return 1000;
            case YEARLY: return 10000;
            case LIFETIME: return Integer.MAX_VALUE;
            default: return 10;
        }
    }
    
    /**
     * Get user's URLs
     */
    public List<Url> getUserUrls(Long userId) {
        return urlRepository.findByUserIdAndIsActiveOrderByCreatedAtDesc(userId, true);
    }
    
    /**
     * Get URL analytics
     */
    public List<UrlAnalytics> getUrlAnalytics(Long urlId, int days) {
        LocalDate startDate = LocalDate.now().minusDays(days - 1);
        return urlAnalyticsRepository.getClicksForLastNDays(urlId, startDate);
    }
    
    /**
     * Get total clicks for user
     */
    public Long getUserTotalClicks(Long userId) {
        Long clicks = urlAnalyticsRepository.getTotalClicksByUserId(userId);
        return clicks != null ? clicks : 0L;
    }
    
    /**
     * Get URL by ID for user
     */
    public Optional<Url> getUserUrl(Long urlId, Long userId) {
        return urlRepository.findByIdAndUserId(urlId, userId);
    }
    
    /**
     * Get user dashboard statistics
     */
    public UserDashboardStats getUserDashboardStats(Long userId) {
        long totalUrls = urlRepository.countByUserIdAndIsActive(userId, true);
        
        // Get total clicks across all user URLs
        List<Url> userUrls = urlRepository.findByUserIdAndIsActiveOrderByCreatedAtDesc(userId, true);
        long totalClicks = userUrls.stream()
            .mapToLong(Url::getClickCount)
            .sum();
        
        // Calculate average CTR (Click Through Rate)
        double avgCtr = totalUrls > 0 ? (double) totalClicks / totalUrls : 0.0;
        
        return new UserDashboardStats(totalUrls, totalClicks, avgCtr);
    }
    
    /**
     * Get recent URLs for user (limited to 5)
     */
    public List<Url> getRecentUserUrls(Long userId, int limit) {
        return urlRepository.findByUserIdAndIsActiveOrderByCreatedAtDesc(userId, true)
            .stream()
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Dashboard statistics data class
     */
    public static class UserDashboardStats {
        private final long totalUrls;
        private final long totalClicks;
        private final double avgCtr;
        
        public UserDashboardStats(long totalUrls, long totalClicks, double avgCtr) {
            this.totalUrls = totalUrls;
            this.totalClicks = totalClicks;
            this.avgCtr = avgCtr;
        }
        
        public long getTotalUrls() { return totalUrls; }
        public long getTotalClicks() { return totalClicks; }
        public double getAvgCtr() { return avgCtr; }
    }
}