package org.stir.shrinkurl.util;

import org.springframework.stereotype.Component;
import org.stir.shrinkurl.enums.SubscriptionPlan;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.dto.SubscriptionUsageDTO;
import org.stir.shrinkurl.dto.UsageInsightsDTO;

@Component
public class PlanRecommendationUtil {
    
    public SubscriptionPlan recommendPlan(User user, SubscriptionUsageDTO usage) {
        // If user is consistently hitting limits, recommend upgrade
        if (usage.getUrlUsagePercentage() > 80 || 
            usage.getApiUsagePercentage() > 80 || 
            usage.getStorageUsagePercentage() > 80) {
            
            return getNextTier(user.getSubscriptionPlan());
        }
        
        // If user has very low usage, they might be fine with current plan
        if (usage.getUrlUsagePercentage() < 20 && 
            usage.getApiUsagePercentage() < 20 && 
            usage.getStorageUsagePercentage() < 20) {
            
            return user.getSubscriptionPlan(); // Stay on current plan
        }
        
        // For moderate usage, recommend yearly if on monthly for savings
        if (user.getSubscriptionPlan() == SubscriptionPlan.MONTHLY) {
            return SubscriptionPlan.YEARLY;
        }
        
        return user.getSubscriptionPlan();
    }
    
    private SubscriptionPlan getNextTier(SubscriptionPlan current) {
        switch (current) {
            case FREE:
                return SubscriptionPlan.MONTHLY;
            case MONTHLY:
                return SubscriptionPlan.YEARLY;
            case YEARLY:
                return SubscriptionPlan.LIFETIME;
            default:
                return current;
        }
    }
    
    public String getRecommendationReason(User user, SubscriptionUsageDTO usage) {
        SubscriptionPlan recommended = recommendPlan(user, usage);
        
        if (recommended == user.getSubscriptionPlan()) {
            return "Your current plan fits your usage perfectly!";
        }
        
        if (usage.getUrlUsagePercentage() > 80) {
            return "You're approaching your URL limit. Consider upgrading to avoid service interruption.";
        }
        
        if (usage.getApiUsagePercentage() > 80) {
            return "Your API usage is high. Upgrade for higher limits and better performance.";
        }
        
        if (usage.getStorageUsagePercentage() > 80) {
            return "You're running low on storage space. Upgrade for more storage capacity.";
        }
        
        if (user.getSubscriptionPlan() == SubscriptionPlan.MONTHLY) {
            return "Switch to yearly billing and save " + SubscriptionPlan.YEARLY.getSavingsPercentage() + "%!";
        }
        
        return "Consider upgrading for enhanced features and better value.";
    }
    
    public UsageInsightsDTO getUsageInsights(SubscriptionUsageDTO usage) {
        return UsageInsightsDTO.fromUsage(usage);
    }
}
