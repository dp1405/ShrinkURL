package org.stir.shrinkurl.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.stir.shrinkurl.annotation.RateLimit;
import org.stir.shrinkurl.dto.SubscriptionUsageDTO;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.service.SubscriptionService;

@RestController
@RequestMapping("/api/analytics")
@Slf4j
public class AnalyticsController {

    @Autowired
    private SubscriptionService subscriptionService;

    /**
     * Get user's subscription usage statistics
     */
    @GetMapping("/usage")
    @PreAuthorize("hasRole('USER')")
    @RateLimit(value = 50, timeWindow = 60, message = "Usage stats rate limit exceeded")
    public ResponseEntity<SubscriptionUsageDTO> getUserUsageStats(@AuthenticationPrincipal User user) {
        try {
            SubscriptionUsageDTO usage = subscriptionService.getUsageStats(user);
            return ResponseEntity.ok(usage);
        } catch (Exception e) {
            log.error("Error fetching usage stats for user {}: {}", user.getId(), e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get detailed analytics (premium feature)
     */
    @GetMapping("/detailed")
    @PreAuthorize("hasRole('USER')")
    @RateLimit(value = 20, timeWindow = 60, message = "Detailed analytics rate limit exceeded")
    public ResponseEntity<?> getDetailedAnalytics(@AuthenticationPrincipal User user) {
        try {
            // Premium feature check
            if (!user.isPremiumUser()) {
                return ResponseEntity.status(403).body(
                    new ErrorResponse("Detailed analytics are only available for premium users")
                );
            }
            
            SubscriptionUsageDTO usage = subscriptionService.getUsageStats(user);
            
            // Add more detailed analytics for premium users
            DetailedAnalyticsResponse response = new DetailedAnalyticsResponse(
                usage.getTotalClicks(),
                usage.getClicksToday(),
                usage.getClicksThisMonth(),
                usage.getUrlsCreated(),
                usage.getUrlLimit(),
                usage.getPlan().getDisplayName()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching detailed analytics for user {}: {}", user.getId(), e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Response DTOs
     */
    public static class DetailedAnalyticsResponse {
        private final long totalClicks;
        private final long clicksToday;
        private final long clicksThisMonth;
        private final int urlsCreated;
        private final int urlLimit;
        private final String planName;

        public DetailedAnalyticsResponse(long totalClicks, long clicksToday, long clicksThisMonth, 
                                       int urlsCreated, int urlLimit, String planName) {
            this.totalClicks = totalClicks;
            this.clicksToday = clicksToday;
            this.clicksThisMonth = clicksThisMonth;
            this.urlsCreated = urlsCreated;
            this.urlLimit = urlLimit;
            this.planName = planName;
        }

        public long getTotalClicks() { return totalClicks; }
        public long getClicksToday() { return clicksToday; }
        public long getClicksThisMonth() { return clicksThisMonth; }
        public int getUrlsCreated() { return urlsCreated; }
        public int getUrlLimit() { return urlLimit; }
        public String getPlanName() { return planName; }
    }

    public static class ErrorResponse {
        private final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() { return error; }
    }
}
