package org.stir.shrinkurl.enums;

public enum SubscriptionStatus {
    PENDING,    // Payment pending
    TRIAL,      // In trial period
    ACTIVE,     // Active subscription
    EXPIRED,    // Subscription expired
    CANCELLED,  // User cancelled
    SUSPENDED   // Suspended (payment failed, etc.)
}
