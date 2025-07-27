package org.stir.shrinkurl.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stir.shrinkurl.dto.SubscriptionUsageDTO;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.entity.Subscription;
import org.stir.shrinkurl.enums.SubscriptionPlan;
import org.stir.shrinkurl.enums.SubscriptionStatus;
import org.stir.shrinkurl.repository.SubscriptionRepository;
import org.stir.shrinkurl.repository.UrlAnalyticsRepository;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    
    @Mock
    private UrlService urlService;
    
    @Mock
    private UrlAnalyticsRepository urlAnalyticsRepository;
    
    @InjectMocks
    private SubscriptionService subscriptionService;
    
    private User testUser;
    private Subscription testSubscription;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create test user
        testUser = User.builder()
            .id(1L)
            .email("test@example.com")
            .name("Test User")
            .build();
        
        // Create test subscription
        testSubscription = Subscription.builder()
            .id(1L)
            .user(testUser)
            .plan(SubscriptionPlan.FREE)
            .status(SubscriptionStatus.ACTIVE)
            .startDate(LocalDateTime.now())
            .endDate(LocalDateTime.now().plusYears(1))
            .build();
        
        testUser.setSubscription(testSubscription);
    }
    
    @Test
    void testGetUsageStats_FreeUser() {
        // Mock dependencies
        when(urlService.getUserUrls(1L)).thenReturn(Collections.emptyList());
        when(urlService.getUserTotalClicks(1L)).thenReturn(0L);
        when(urlAnalyticsRepository.getClicksForUserInDateRange(any(), any())).thenReturn(Collections.emptyList());
        
        // Call method
        SubscriptionUsageDTO usage = subscriptionService.getUsageStats(testUser);
        
        // Verify results
        assertNotNull(usage);
        assertEquals(SubscriptionPlan.FREE, usage.getPlan());
        assertEquals(10, usage.getUrlLimit()); // FREE plan limit
        assertEquals(100, usage.getApiCallLimit()); // FREE plan limit
        assertEquals(0, usage.getUrlsCreated());
        assertEquals(0, usage.getApiCallsToday());
        assertEquals(0L, usage.getTotalClicks());
    }
    
    @Test
    void testIsFeatureAvailable_FreeUser() {
        // Test premium features should be false
        assertFalse(subscriptionService.isFeatureAvailable(testUser, "custom_short_codes"));
        assertFalse(subscriptionService.isFeatureAvailable(testUser, "custom_domains"));
        assertFalse(subscriptionService.isFeatureAvailable(testUser, "advanced_analytics"));
        assertFalse(subscriptionService.isFeatureAvailable(testUser, "unlimited_urls"));
        
        // Test unknown features should be false
        assertFalse(subscriptionService.isFeatureAvailable(testUser, "unknown_feature"));
    }
    
    @Test
    void testIsFeatureAvailable_PremiumUser() {
        // Change user to premium
        testSubscription.setPlan(SubscriptionPlan.YEARLY);
        
        // Test premium features
        assertTrue(subscriptionService.isFeatureAvailable(testUser, "custom_short_codes"));
        assertTrue(subscriptionService.isFeatureAvailable(testUser, "custom_domains"));
        assertTrue(subscriptionService.isFeatureAvailable(testUser, "advanced_analytics"));
        assertTrue(subscriptionService.isFeatureAvailable(testUser, "priority_support"));
        
        // Test lifetime features
        assertFalse(subscriptionService.isFeatureAvailable(testUser, "unlimited_urls"));
    }
    
    @Test
    void testIsFeatureAvailable_LifetimeUser() {
        // Change user to lifetime
        testSubscription.setPlan(SubscriptionPlan.LIFETIME);
        
        // Test all features
        assertTrue(subscriptionService.isFeatureAvailable(testUser, "custom_short_codes"));
        assertTrue(subscriptionService.isFeatureAvailable(testUser, "custom_domains"));
        assertTrue(subscriptionService.isFeatureAvailable(testUser, "advanced_analytics"));
        assertTrue(subscriptionService.isFeatureAvailable(testUser, "unlimited_urls"));
        assertTrue(subscriptionService.isFeatureAvailable(testUser, "unlimited_api"));
        assertTrue(subscriptionService.isFeatureAvailable(testUser, "unlimited_storage"));
    }
    
    @Test
    void testCanCreateUrl_WithinLimit() {
        // Mock URL service to return empty list (0 URLs created)
        when(urlService.getUserUrls(1L)).thenReturn(Collections.emptyList());
        when(urlService.getUserTotalClicks(1L)).thenReturn(0L);
        when(urlAnalyticsRepository.getClicksForUserInDateRange(any(), any())).thenReturn(Collections.emptyList());
        
        // Test - should be able to create URL (0 < 10)
        assertTrue(subscriptionService.canCreateUrl(testUser));
    }
    
    @Test
    void testHasUsedTrial() {
        // Mock repository
        when(subscriptionRepository.existsByUserIdAndStatus(1L, SubscriptionStatus.TRIAL)).thenReturn(false);
        
        // Test
        assertFalse(subscriptionService.hasUsedTrial(testUser));
        
        // Test with trial used
        when(subscriptionRepository.existsByUserIdAndStatus(1L, SubscriptionStatus.TRIAL)).thenReturn(true);
        assertTrue(subscriptionService.hasUsedTrial(testUser));
    }
    
    @Test
    void testGetUpgradeMessage() {
        // Test free user message
        String message = subscriptionService.getUpgradeMessage(testUser);
        assertNotNull(message);
        assertTrue(message.contains("Upgrade to premium"));
        
        // Test monthly user message
        testSubscription.setPlan(SubscriptionPlan.MONTHLY);
        message = subscriptionService.getUpgradeMessage(testUser);
        assertNotNull(message);
        assertTrue(message.contains("yearly"));
        
        // Test lifetime user (should have no upgrade message)
        testSubscription.setPlan(SubscriptionPlan.LIFETIME);
        message = subscriptionService.getUpgradeMessage(testUser);
        assertNull(message);
    }
}
