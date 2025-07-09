package org.stir.shrinkurl.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.stir.shrinkurl.dto.SubscriptionUsageDTO;
import org.stir.shrinkurl.entity.Subscription;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.enums.SubscriptionPlan;
import org.stir.shrinkurl.enums.SubscriptionStatus;
import org.stir.shrinkurl.exceptions.TrialAlreadyUsedException;
import org.stir.shrinkurl.repository.SubscriptionRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class SubscriptionService {
    
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    
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
    
    
    // Create pending subscription for upgrade
    public Subscription createPendingSubscription(User user, SubscriptionPlan plan) {
        if (plan == SubscriptionPlan.FREE) {
            throw new IllegalArgumentException("Cannot upgrade to FREE plan");
        }
        
        Subscription pendingSubscription = Subscription.builder()
            .user(user)
            .plan(plan)
            .status(SubscriptionStatus.PENDING)
            .amount(BigDecimal.valueOf(plan.getPrice()))
            .currency("USD")
            .build();
        
        return subscriptionRepository.save(pendingSubscription);
    }
    
    // Activate subscription after payment
    public Subscription activateSubscription(String subscriptionId, String paymentId, String transactionId) {
        Subscription subscription = subscriptionRepository.findById(Long.parseLong(subscriptionId))
            .orElseThrow(() -> new RuntimeException("Subscription not found"));
        
        if (subscription.getStatus() != SubscriptionStatus.PENDING) {
            throw new IllegalStateException("Subscription is not in pending state");
        }
        
        // Deactivate current subscription
        subscriptionRepository.findActiveByUserId(subscription.getUser().getId())
            .ifPresent(current -> {
                current.setStatus(SubscriptionStatus.EXPIRED);
                subscriptionRepository.save(current);
            });
        
        // Activate new subscription
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDateTime.now());
        subscription.calculateEndDate();
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
        
        return SubscriptionUsageDTO.builder()
            .userId(user.getId())
            .plan(plan)
            .urlsCreated(getUrlsCreatedCount(user)) // To be implemented with URL service
            .urlLimit(getUrlLimit(plan))
            .apiCallsToday(getApiCallsToday(user)) // To be implemented with API tracking
            .apiCallLimit(getApiCallLimit(plan))
            .storageUsed(getStorageUsed(user)) // To be implemented with storage service
            .storageLimit(getStorageLimit(plan))
            .build();
    }
    
    private int getUrlLimit(SubscriptionPlan plan) {
        switch (plan) {
            case FREE: return 10;
            case MONTHLY: return 1000;
            case YEARLY: return 10000;
            case LIFETIME: return Integer.MAX_VALUE;
            default: return 10;
        }
    }
    
    private int getApiCallLimit(SubscriptionPlan plan) {
        switch (plan) {
            case FREE: return 100;
            case MONTHLY: return 10000;
            case YEARLY: return 100000;
            case LIFETIME: return Integer.MAX_VALUE;
            default: return 100;
        }
    }
    
    private long getStorageLimit(SubscriptionPlan plan) {
        switch (plan) {
            case FREE: return 100 * 1024 * 1024; // 100 MB
            case MONTHLY: return 5L * 1024 * 1024 * 1024; // 5 GB
            case YEARLY: return 50L * 1024 * 1024 * 1024; // 50 GB
            case LIFETIME: return 1000L * 1024 * 1024 * 1024; // 1 TB
            default: return 100 * 1024 * 1024;
        }
    }
    
    // These would need to be implemented based on your business logic
    private int getUrlsCreatedCount(User user) {
        // Return count from your URL service
        return 0;
    }
    
    private int getApiCallsToday(User user) {
        // Return API calls from your tracking service
        return 0;
    }
    
    private long getStorageUsed(User user) {
        // Return storage used from your storage service
        return 0;
    }
}
