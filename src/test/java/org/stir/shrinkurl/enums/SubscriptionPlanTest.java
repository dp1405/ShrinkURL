package org.stir.shrinkurl.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SubscriptionPlanTest {
    
    @Test
    public void testFreeePlanProperties() {
        SubscriptionPlan freePlan = SubscriptionPlan.FREE;
        assertEquals("Free", freePlan.getDisplayName());
        assertEquals(0.0, freePlan.getPrice());
        assertEquals(10, freePlan.getUrlLimit());
        assertEquals(100, freePlan.getApiCallLimit());
        assertFalse(freePlan.isPremium());
        assertFalse(freePlan.isPopular());
        assertEquals(0, freePlan.getSavingsPercentage());
    }
    
    @Test
    public void testMonthlyPlanProperties() {
        SubscriptionPlan monthlyPlan = SubscriptionPlan.MONTHLY;
        assertEquals("Monthly Premium", monthlyPlan.getDisplayName());
        assertEquals(9.99, monthlyPlan.getPrice());
        assertEquals(1000, monthlyPlan.getUrlLimit());
        assertEquals(10000, monthlyPlan.getApiCallLimit());
        assertTrue(monthlyPlan.isPremium());
        assertFalse(monthlyPlan.isPopular());
        assertEquals(0, monthlyPlan.getSavingsPercentage());
    }
    
    @Test
    public void testYearlyPlanProperties() {
        SubscriptionPlan yearlyPlan = SubscriptionPlan.YEARLY;
        assertEquals("Yearly Premium", yearlyPlan.getDisplayName());
        assertEquals(99.99, yearlyPlan.getPrice());
        assertEquals(10000, yearlyPlan.getUrlLimit());
        assertEquals(100000, yearlyPlan.getApiCallLimit());
        assertTrue(yearlyPlan.isPremium());
        assertTrue(yearlyPlan.isPopular());
        assertTrue(yearlyPlan.getSavingsPercentage() > 0);
    }
    
    @Test
    public void testLifetimePlanProperties() {
        SubscriptionPlan lifetimePlan = SubscriptionPlan.LIFETIME;
        assertEquals("Lifetime Premium", lifetimePlan.getDisplayName());
        assertEquals(299.99, lifetimePlan.getPrice());
        assertEquals(Integer.MAX_VALUE, lifetimePlan.getUrlLimit());
        assertEquals(Integer.MAX_VALUE, lifetimePlan.getApiCallLimit());
        assertTrue(lifetimePlan.isPremium());
        assertFalse(lifetimePlan.isPopular());
        assertEquals(0, lifetimePlan.getSavingsPercentage());
    }
    
    @Test
    public void testFormattedPriceAndPeriod() {
        assertEquals("Free", SubscriptionPlan.FREE.getFormattedPrice());
        assertEquals("", SubscriptionPlan.FREE.getPricePeriod());
        
        assertEquals("$9.99", SubscriptionPlan.MONTHLY.getFormattedPrice());
        assertEquals("/month", SubscriptionPlan.MONTHLY.getPricePeriod());
        
        assertEquals("$99.99", SubscriptionPlan.YEARLY.getFormattedPrice());
        assertEquals("/year", SubscriptionPlan.YEARLY.getPricePeriod());
        
        assertEquals("$299.99", SubscriptionPlan.LIFETIME.getFormattedPrice());
        assertEquals(" one-time", SubscriptionPlan.LIFETIME.getPricePeriod());
    }
    
    @Test
    public void testRetentionDescription() {
        assertEquals("30 days", SubscriptionPlan.FREE.getRetentionDescription());
        assertEquals("365 days", SubscriptionPlan.MONTHLY.getRetentionDescription());
        assertEquals("No expiration", SubscriptionPlan.YEARLY.getRetentionDescription());
        assertEquals("No expiration", SubscriptionPlan.LIFETIME.getRetentionDescription());
    }
    
    @Test
    public void testFormattedLimits() {
        assertEquals("10", SubscriptionPlan.FREE.getFormattedUrlLimit());
        assertEquals("1,000", SubscriptionPlan.MONTHLY.getFormattedUrlLimit());
        assertEquals("10,000", SubscriptionPlan.YEARLY.getFormattedUrlLimit());
        assertEquals("Unlimited", SubscriptionPlan.LIFETIME.getFormattedUrlLimit());
        
        assertEquals("100", SubscriptionPlan.FREE.getFormattedApiLimit());
        assertEquals("10,000", SubscriptionPlan.MONTHLY.getFormattedApiLimit());
        assertEquals("100,000", SubscriptionPlan.YEARLY.getFormattedApiLimit());
        assertEquals("Unlimited", SubscriptionPlan.LIFETIME.getFormattedApiLimit());
    }
    
    @Test
    public void testStorageFormatting() {
        assertEquals("100 MB", SubscriptionPlan.FREE.getFormattedStorage());
        assertEquals("5 GB", SubscriptionPlan.MONTHLY.getFormattedStorage());
        assertEquals("50 GB", SubscriptionPlan.YEARLY.getFormattedStorage());
        assertEquals("1 TB", SubscriptionPlan.LIFETIME.getFormattedStorage());
    }
    
    @Test
    public void testFeatureAvailability() {
        // Free plan should have basic features
        assertTrue(SubscriptionPlan.FREE.getFeatures().contains("Basic URL shortening"));
        assertTrue(SubscriptionPlan.FREE.getFeatures().contains("Basic analytics"));
        
        // Premium plans should have advanced features
        assertTrue(SubscriptionPlan.MONTHLY.getFeatures().contains("Custom short codes"));
        assertTrue(SubscriptionPlan.YEARLY.getFeatures().contains("Priority support"));
        assertTrue(SubscriptionPlan.LIFETIME.getFeatures().contains("24/7 support"));
    }
}
