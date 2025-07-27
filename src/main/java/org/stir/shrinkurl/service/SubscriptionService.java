package org.stir.shrinkurl.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.stir.shrinkurl.dto.SubscriptionUsageDTO;
import org.stir.shrinkurl.entity.Subscription;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.entity.UrlAnalytics;
import org.stir.shrinkurl.enums.SubscriptionPlan;
import org.stir.shrinkurl.enums.SubscriptionStatus;
import org.stir.shrinkurl.exceptions.TrialAlreadyUsedException;
import org.stir.shrinkurl.repository.SubscriptionRepository;
import org.stir.shrinkurl.repository.UrlAnalyticsRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class SubscriptionService {
    
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    
    @Autowired
    private UrlService urlService;
    
    @Autowired
    private UrlAnalyticsRepository urlAnalyticsRepository;
    
    @Value("${subscription.trial.days:7}")
    private int trialDays;
    
    public void initializeFreeSubscription(User user) {
        Subscription subscription = Subscription.builder()
            .user(user)
            .plan(SubscriptionPlan.FREE)
            .status(SubscriptionStatus.ACTIVE)
            .startDate(LocalDateTime.now())
            .endDate(LocalDateTime.now().plusYears(100)) // Effectively never expires
            .amount(BigDecimal.ZERO)
            .currency("INR")
            .autoRenew(false)
            .build();
        
        subscriptionRepository.save(subscription);
        log.info("Initialized free subscription for user: {}", user.getEmail());
    }
    
    
    // Get subscription by ID
    public Subscription getSubscriptionById(Long subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
            .orElse(null);
    }
    
    // Create pending subscription for upgrade
    public Subscription createPendingSubscription(User user, SubscriptionPlan plan) {
        if (plan == SubscriptionPlan.FREE) {
            throw new IllegalArgumentException("Cannot upgrade to FREE plan");
        }
        
        // Get the current subscription
        Subscription currentSubscription = subscriptionRepository.findActiveByUserId(user.getId())
            .orElseThrow(() -> new RuntimeException("User does not have an active subscription"));
        
        // Update current subscription to pending upgrade status
        currentSubscription.setPlan(plan);
        currentSubscription.setStatus(SubscriptionStatus.PENDING);
        currentSubscription.setAmount(BigDecimal.valueOf(plan.getPrice()));
        currentSubscription.setCurrency("USD");
        currentSubscription.setAutoRenew(true);
        
        // Save the updated subscription
        return subscriptionRepository.save(currentSubscription);
    }
    
    // Helper method to calculate end date based on plan
    private LocalDateTime calculateEndDateForPlan(LocalDateTime startDate, SubscriptionPlan plan) {
        switch (plan) {
            case MONTHLY:
                return startDate.plusMonths(1);
            case YEARLY:
                return startDate.plusYears(1);
            case LIFETIME:
                return startDate.plusYears(100); // Effectively lifetime
            default:
                return startDate.plusYears(1); // Default to 1 year
        }
    }
    
    // Activate subscription after payment
    public Subscription activateSubscription(String subscriptionId, String paymentId, String transactionId) {
        Subscription subscription = subscriptionRepository.findById(Long.parseLong(subscriptionId))
            .orElseThrow(() -> new RuntimeException("Subscription not found"));
        
        if (subscription.getStatus() != SubscriptionStatus.PENDING) {
            throw new IllegalStateException("Subscription is not in pending state");
        }
        
        // Activate the subscription (no need to deactivate current since we're updating the same one)
        LocalDateTime newStartDate = LocalDateTime.now();
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(newStartDate);
        subscription.setEndDate(calculateEndDateForPlan(newStartDate, subscription.getPlan()));
        subscription.setAutoRenew(true);
        
        return subscriptionRepository.save(subscription);
    }
    
    // Handle failed payment
    public void handleFailedPayment(String subscriptionId, String reason) {
        Subscription subscription = subscriptionRepository.findById(Long.parseLong(subscriptionId))
            .orElseThrow(() -> new RuntimeException("Subscription not found"));
        
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancellationReason("Payment failed: " + reason);
        subscriptionRepository.save(subscription);
    }
    
    // Cancel subscription
    public void cancelSubscription(Subscription subscription, String reason) {
        subscription.cancel(reason);
        subscriptionRepository.save(subscription);
        
        log.info("Subscription cancelled for user: {}", subscription.getUser().getEmail());
    }
    
    // Activate trial
    public void activateTrial(User user) {
        if (hasUsedTrial(user)) {
            throw new TrialAlreadyUsedException("Trial already used");
        }
        
        // Deactivate current subscription
        subscriptionRepository.findActiveByUserId(user.getId())
            .ifPresent(current -> {
                current.setStatus(SubscriptionStatus.EXPIRED);
                subscriptionRepository.save(current);
            });
        
        // Create trial subscription
        Subscription trial = Subscription.builder()
            .user(user)
            .plan(SubscriptionPlan.MONTHLY)
            .status(SubscriptionStatus.TRIAL)
            .startDate(LocalDateTime.now())
            .endDate(LocalDateTime.now().plusDays(trialDays))
            .trialEndDate(LocalDateTime.now().plusDays(trialDays))
            .amount(BigDecimal.ZERO)
            .currency("USD")
            .autoRenew(false)
            .build();
        
        subscriptionRepository.save(trial);
        log.info("Trial activated for user: {}", user.getEmail());
    }
    
    // Check if user has used trial
    public boolean hasUsedTrial(User user) {
        return subscriptionRepository.existsByUserIdAndStatus(user.getId(), SubscriptionStatus.TRIAL);
    }
    
    // Get usage statistics
    public SubscriptionUsageDTO getUsageStats(User user) {
        SubscriptionPlan plan = user.getSubscriptionPlan();
        
        // Get current counts
        int urlsCreated = getUrlsCreatedCount(user);
        int urlLimit = getUrlLimit(plan);
        int apiCallsToday = getApiCallsToday(user);
        int apiCallLimit = getApiCallLimit(plan);
        long storageUsed = getStorageUsed(user);
        long storageLimit = getStorageLimit(plan);
        
        // Get analytics data
        long totalClicks = urlService.getUserTotalClicks(user.getId());
        long clicksToday = getClicksToday(user);
        long clicksThisMonth = getClicksThisMonth(user);
        
        SubscriptionUsageDTO usage = SubscriptionUsageDTO.builder()
            .userId(user.getId())
            .plan(plan)
            .urlsCreated(urlsCreated)
            .urlLimit(urlLimit)
            .apiCallsToday(apiCallsToday)
            .apiCallLimit(apiCallLimit)
            .storageUsed(storageUsed)
            .storageLimit(storageLimit)
            .totalClicks(totalClicks)
            .clicksToday(clicksToday)
            .clicksThisMonth(clicksThisMonth)
            .build();
        
        // Calculate percentages
        usage.calculatePercentages();
        
        return usage;
    }
    
    private int getUrlLimit(SubscriptionPlan plan) {
        return plan.getUrlLimit();
    }
    
    private int getApiCallLimit(SubscriptionPlan plan) {
        return plan.getApiCallLimit();
    }
    
    private long getStorageLimit(SubscriptionPlan plan) {
        return plan.getStorageLimit();
    }
    
    // These would need to be implemented based on your business logic
    private int getUrlsCreatedCount(User user) {
        return urlService.getUserUrls(user.getId()).size();
    }
    
    private int getApiCallsToday(User user) {
        // TODO: Implement API call tracking
        return 0;
    }
    
    private long getStorageUsed(User user) {
        // TODO: Implement storage tracking
        return 0;
    }
    
    private long getClicksToday(User user) {
        // Get today's clicks from analytics
        return urlAnalyticsRepository.getClicksForUserInDateRange(user.getId(), LocalDate.now())
            .stream()
            .mapToLong(UrlAnalytics::getClickCount)
            .sum();
    }
    
    private long getClicksThisMonth(User user) {
        // Get this month's clicks from analytics
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        return urlAnalyticsRepository.getClicksForUserInDateRange(user.getId(), startOfMonth)
            .stream()
            .mapToLong(UrlAnalytics::getClickCount)
            .sum();
    }
    
    // Enhanced usage tracking methods
    public boolean isFeatureAvailable(User user, String feature) {
        SubscriptionPlan plan = user.getSubscriptionPlan();
        
        switch (feature.toLowerCase()) {
            case "custom_short_codes":
            case "custom_domains":
            case "advanced_analytics":
            case "priority_support":
                return plan.isPremium();
            case "unlimited_urls":
                return plan.getUrlLimit() == Integer.MAX_VALUE;
            case "unlimited_api":
                return plan.getApiCallLimit() == Integer.MAX_VALUE;
            case "unlimited_storage":
                return plan.getStorageLimit() >= 1000L * 1024 * 1024 * 1024; // 1TB+
            default:
                return false;
        }
    }
    
    public List<String> getAvailableFeatures(User user) {
        return user.getSubscriptionPlan().getFeatures();
    }
    
    public List<String> getUpgradeFeatures(User user) {
        SubscriptionPlan currentPlan = user.getSubscriptionPlan();
        if (currentPlan == SubscriptionPlan.LIFETIME) {
            return List.of(); // Already have everything
        }
        
        SubscriptionPlan nextPlan = getNextPlan(currentPlan);
        if (nextPlan == null) {
            return List.of();
        }
        
        return nextPlan.getFeatures();
    }
    
    private SubscriptionPlan getNextPlan(SubscriptionPlan current) {
        switch (current) {
            case FREE: return SubscriptionPlan.MONTHLY;
            case MONTHLY: return SubscriptionPlan.YEARLY;
            case YEARLY: return SubscriptionPlan.LIFETIME;
            default: return null;
        }
    }
    
    public boolean hasReachedLimit(User user, String limitType) {
        SubscriptionUsageDTO usage = getUsageStats(user);
        
        switch (limitType.toLowerCase()) {
            case "urls":
                return usage.getUrlUsagePercentage() >= 100;
            case "api":
                return usage.getApiUsagePercentage() >= 100;
            case "storage":
                return usage.getStorageUsagePercentage() >= 100;
            default:
                return false;
        }
    }

    public boolean canCreateUrl(User user) {
        SubscriptionUsageDTO usage = getUsageStats(user);
        return usage.getUrlsCreated() < usage.getUrlLimit();
    }
    
    public boolean canMakeApiCall(User user) {
        SubscriptionUsageDTO usage = getUsageStats(user);
        return usage.getApiCallsToday() < usage.getApiCallLimit();
    }
    
    public String getUpgradeMessage(User user) {
        SubscriptionPlan current = user.getSubscriptionPlan();
        SubscriptionUsageDTO usage = getUsageStats(user);
        
        if (current == SubscriptionPlan.FREE) {
            if (usage.getUrlUsagePercentage() > 80) {
                return "You're running out of URLs! Upgrade to get more capacity.";
            }
            return "Upgrade to premium for more features and higher limits.";
        }
        
        if (current == SubscriptionPlan.MONTHLY) {
            return "Save 17% with our yearly plan and get unlimited URL retention!";
        }
        
        if (current == SubscriptionPlan.YEARLY) {
            return "Go lifetime for unlimited access and no recurring payments!";
        }
        
        return null;
    }
}
