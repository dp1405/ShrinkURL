package org.stir.shrinkurl.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.stir.shrinkurl.annotation.RateLimit;
import org.stir.shrinkurl.dto.UrlShortenRequest;
import org.stir.shrinkurl.entity.Url;
import org.stir.shrinkurl.entity.UrlAnalytics;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.service.UrlService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/urls")
@Slf4j
public class UrlController {
    
    @Autowired
    private UrlService urlService;

    @Value("${url.base-url}")
    private String baseUrl;
    
    /**
     * Shorten a URL
     */
    @PostMapping("/shorten")
    @PreAuthorize("hasRole('USER')")
    @RateLimit(value = 50, timeWindow = 60, message = "URL shortening rate limit exceeded")
    public ResponseEntity<?> shortenUrl(
            @Valid @RequestBody UrlShortenRequest request,
            @RequestParam(required = false) String customCode,
            @AuthenticationPrincipal User user) {
        
        try {
            Url shortenedUrl = urlService.createShortenedUrl(request.getOriginalUrl(), user, customCode);
            
            return ResponseEntity.ok(new UrlResponse(
                shortenedUrl.getId(),
                shortenedUrl.getOriginalUrl(),
                shortenedUrl.getShortCode(),
                baseUrl + shortenedUrl.getShortCode(),
                shortenedUrl.getCreatedAt(),
                shortenedUrl.getExpiresAt()
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error shortening URL for user {}: {}", user.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("An error occurred while shortening the URL"));
        }
    }
    
    /**
     * Get user's URLs
     */
    @GetMapping("/my-urls")
    @PreAuthorize("hasRole('USER')")
    @RateLimit(value = 100, timeWindow = 60)
    public ResponseEntity<List<Url>> getUserUrls(@AuthenticationPrincipal User user) {
        List<Url> urls = urlService.getUserUrls(user.getId());
        return ResponseEntity.ok(urls);
    }
    
    /**
     * Get URL analytics
     */
    @GetMapping("/{id}/analytics")
    @PreAuthorize("hasRole('USER')")
    @RateLimit(value = 50, timeWindow = 60)
    public ResponseEntity<?> getUrlAnalytics(
            @PathVariable Long id,
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal User user) {
        
        try {
            // Verify URL belongs to user
            Optional<Url> urlOpt = urlService.getUserUrl(id, user.getId());
            if (!urlOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            List<UrlAnalytics> analytics = urlService.getUrlAnalytics(id, days);
            
            // Create analytics response
            UrlAnalyticsResponse response = new UrlAnalyticsResponse(
                urlOpt.get().getShortCode(),
                urlOpt.get().getOriginalUrl(),
                urlOpt.get().getClickCount(),
                analytics
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching analytics for URL {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to fetch analytics"));
        }
    }
    
    /**
     * Get URL by ID for user
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @RateLimit(value = 100, timeWindow = 60)
    public ResponseEntity<?> getUrl(@PathVariable Long id, @AuthenticationPrincipal User user) {
        try {
            Optional<Url> urlOpt = urlService.getUserUrl(id, user.getId());
            if (!urlOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(urlOpt.get());
        } catch (Exception e) {
            log.error("Error fetching URL {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to fetch URL"));
        }
    }
    
    /**
     * Response DTOs
     */
    public static class UrlResponse {
        private final Long id;
        private final String originalUrl;
        private final String shortCode;
        private final String shortUrl;
        private final java.time.LocalDateTime createdAt;
        private final java.time.LocalDateTime expiresAt;
        
        public UrlResponse(Long id, String originalUrl, String shortCode, String shortUrl, 
                          java.time.LocalDateTime createdAt, java.time.LocalDateTime expiresAt) {
            this.id = id;
            this.originalUrl = originalUrl;
            this.shortCode = shortCode;
            this.shortUrl = shortUrl;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }
        
        // Getters
        public Long getId() { return id; }
        public String getOriginalUrl() { return originalUrl; }
        public String getShortCode() { return shortCode; }
        public String getShortUrl() { return shortUrl; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public java.time.LocalDateTime getExpiresAt() { return expiresAt; }
    }
    
    public static class ErrorResponse {
        private final String error;
        
        public ErrorResponse(String error) {
            this.error = error;
        }
        
        public String getError() { return error; }
    }
    
    public static class UrlAnalyticsResponse {
        private final String shortCode;
        private final String originalUrl;
        private final int totalClicks;
        private final List<UrlAnalytics> dailyAnalytics;
        
        public UrlAnalyticsResponse(String shortCode, String originalUrl, int totalClicks, List<UrlAnalytics> dailyAnalytics) {
            this.shortCode = shortCode;
            this.originalUrl = originalUrl;
            this.totalClicks = totalClicks;
            this.dailyAnalytics = dailyAnalytics;
        }
        
        public String getShortCode() { return shortCode; }
        public String getOriginalUrl() { return originalUrl; }
        public int getTotalClicks() { return totalClicks; }
        public List<UrlAnalytics> getDailyAnalytics() { return dailyAnalytics; }
    }
}